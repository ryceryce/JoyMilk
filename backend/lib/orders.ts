import { z } from "zod";
import { db, digest, fail, profile, type Sql } from "./core";

const moneyLimit = 9223372036854775807n;
export const statuses = z.enum(["pending", "diproses", "selesai", "dibatalkan"]);
const line = z.object({
  id: z.uuid().optional(),
  productId: z.uuid(),
  quantity: z.number().int().min(1).max(2147483647),
  sweetLevel: z.enum(["TANPA_GULA", "LESS_SUGAR", "NORMAL", "EXTRA_SWEET"]),
});
const form = {
  customerName: z.string().trim().min(1).max(200),
  customerPhone: z.string().trim().max(80).default(""),
  deliveryAddress: z.string().trim().max(1000).default(""),
  paymentMethod: z.enum(["tunai", "transfer", "qris"]),
  notes: z.string().trim().max(2000).default(""),
};
export const orderInput = z.object({ ...form, items: z.array(line.omit({ id: true })).min(1).max(100) });
const editInput = z.object({ ...form, items: z.array(line).min(1).max(100), updatedAt: z.iso.datetime() });
type OrderInput = z.infer<typeof orderInput>;
type Channel = "android" | "telegram" | "mcp";
type Item = {
  id?: string; productId: string; productName: string; priceAtOrder: string;
  quantity: number; subtotal: string; sweetLevel: string;
};
export const orderColumns = () => db()`id, customer_name as "customerName", customer_phone as "customerPhone",
  delivery_address as "deliveryAddress", payment_method as "paymentMethod", status, total_amount::text as "totalAmount",
  created_by as "createdBy", notes, created_at as "createdAt", updated_at as "updatedAt"`;
const itemColumns = () => db()`id, product_id as "productId", product_name as "productName",
  price_at_order::text as "priceAtOrder", quantity, subtotal::text as subtotal, sweet_level as "sweetLevel"`;

export async function getOrder(sql: Sql, id: string) {
  const [order] = await sql<{ id: string; status: string; totalAmount: string }[]>`select ${orderColumns()} from joymilk.orders where id = ${id} for share`;
  if (!order) fail(404, "order_not_found", "Order tidak ditemukan.");
  const items = await sql<Item[]>`select ${itemColumns()} from joymilk.order_items where order_id = ${id} order by id`;
  return { ...order, items };
}

async function snapshot(sql: Sql, input: z.infer<typeof line>[], existing: Item[] = []) {
  const productIds = [...new Set(input.filter(item => !item.id).map(item => item.productId))].sort();
  const products = productIds.length
    ? await sql`select id, name, current_price::text as price from joymilk.products
        where id = any(${productIds}::uuid[]) and is_active order by id for share`
    : [];
  const usedIds = new Set<string>();
  const items = input.map(item => {
    const old = existing.find(row => row.id === item.id);
    if (item.id && (!old || old.productId !== item.productId || usedIds.has(item.id)))
      fail(422, "invalid_item", "Item order tidak valid.");
    if (item.id) usedIds.add(item.id);
    const product = products.find(row => row.id === item.productId);
    if (!old && !product) fail(422, "product_unavailable", "Produk tidak tersedia.");
    const productName = old?.productName ?? product!.name;
    const priceAtOrder = old?.priceAtOrder ?? product!.price;
    const subtotal = BigInt(priceAtOrder) * BigInt(item.quantity);
    return { ...item, productName, priceAtOrder, subtotal: subtotal.toString() } satisfies Item;
  });
  const total = items.reduce((sum, row) => sum + BigInt(row.subtotal), 0n);
  if (total > moneyLimit) fail(422, "amount_too_large", "Jumlah order terlalu besar.");
  return { items, totalAmount: total.toString() };
}

async function insertItem(sql: Sql, orderId: string, item: Item) {
  await sql`insert into joymilk.order_items
      (order_id, product_id, product_name, price_at_order, quantity, subtotal, sweet_level)
      values (${orderId}, ${item.productId}, ${item.productName}, ${item.priceAtOrder}, ${item.quantity}, ${item.subtotal}, ${item.sweetLevel})`;
}

async function persist(sql: Sql, uid: string, channel: Channel, key: string, input: OrderInput,
  preview?: Awaited<ReturnType<typeof snapshot>>) {
  const fingerprint = digest(JSON.stringify(input));
  await sql`insert into joymilk.order_idempotency (actor, channel, key, fingerprint)
    values (${uid}, ${channel}, ${key}, ${fingerprint}) on conflict do nothing`;
  const [claim] = await sql`select fingerprint, order_id from joymilk.order_idempotency
    where actor = ${uid} and channel = ${channel} and key = ${key} for update`;
  if (claim.fingerprint !== fingerprint) fail(409, "idempotency_conflict", "Kunci penyimpanan sudah digunakan.");
  if (claim.order_id) return getOrder(sql, claim.order_id);
  const { items, totalAmount } = preview ?? await snapshot(sql, input.items);
  const [{ id }] = await sql`insert into joymilk.orders
    (customer_name, customer_phone, delivery_address, payment_method, total_amount, created_by, notes)
    values (${input.customerName}, ${input.customerPhone}, ${input.deliveryAddress}, ${input.paymentMethod},
      ${totalAmount}, ${uid}, ${input.notes}) returning id`;
  for (const item of items) await insertItem(sql, id, item);
  await sql`update joymilk.order_idempotency set order_id = ${id} where actor = ${uid} and channel = ${channel} and key = ${key}`;
  return getOrder(sql, id);
}

export function createOrder(uid: string, key: string, raw: unknown) {
  const input = orderInput.parse(raw);
  return db().begin(async sql => {
    await profile(uid, sql);
    return persist(sql, uid, "android", key, input);
  });
}

export function editOrder(id: string, raw: unknown) {
  const input = editInput.parse(raw);
  return db().begin(async sql => {
    const [current] = await sql`select status, date_trunc('milliseconds', updated_at) = ${input.updatedAt}::timestamptz
      as matches from joymilk.orders where id = ${id} for update`;
    if (!current) fail(404, "order_not_found", "Order tidak ditemukan.");
    if (current.status !== "pending" || !current.matches) fail(409, "stale_order", "Muat ulang order sebelum mengubahnya.");
    const existing = await sql<Item[]>`select ${itemColumns()} from joymilk.order_items where order_id = ${id}`;
    const { items, totalAmount } = await snapshot(sql, input.items, existing);
    const retained = items.flatMap(item => item.id ? [item.id] : []);
    await sql`delete from joymilk.order_items where order_id = ${id} and not (id = any(${retained}::uuid[]))`;
    for (const item of items) {
      if (!item.id) await insertItem(sql, id, item);
      else await sql`update joymilk.order_items set quantity = ${item.quantity}, subtotal = ${item.subtotal},
        sweet_level = ${item.sweetLevel} where id = ${item.id}`;
    }
    await sql`update joymilk.orders set customer_name = ${input.customerName}, customer_phone = ${input.customerPhone},
      delivery_address = ${input.deliveryAddress}, payment_method = ${input.paymentMethod}, notes = ${input.notes},
      total_amount = ${totalAmount}, updated_at = greatest(clock_timestamp(), updated_at + interval '1 millisecond') where id = ${id}`;
    return getOrder(sql, id);
  });
}

export async function setStatus(id: string, raw: unknown) {
  const input = z.object({ status: statuses, updatedAt: z.iso.datetime() }).parse(raw);
  const rows = await db()`update joymilk.orders set status = ${input.status},
    updated_at = greatest(clock_timestamp(), updated_at + interval '1 millisecond')
    where id = ${id} and date_trunc('milliseconds', updated_at) = ${input.updatedAt}::timestamptz
    and ((status = 'pending' and ${input.status} in ('diproses', 'dibatalkan'))
      or (status = 'diproses' and ${input.status} = 'selesai')) returning id`;
  if (rows.length === 0) fail(409, "stale_order", "Status order berubah atau transisi tidak diizinkan.");
}

export function prepareOrder(uid: string, channel: Exclude<Channel, "android">, raw: unknown) {
  const input = orderInput.parse(raw);
  return db().begin(async sql => {
    await profile(uid, sql);
    const preview = await snapshot(sql, input.items);
    const [draft] = await sql<{ draftId: string; revision: number; expiresAt: Date }[]>`insert into joymilk.order_drafts (actor, channel, input, preview)
      values (${uid}, ${channel}, ${sql.json(input)}, ${sql.json(preview)})
      returning id as "draftId", revision, expires_at as "expiresAt"`;
    return { ...draft, ...input, ...preview };
  });
}

export function confirmOrder(uid: string, channel: Exclude<Channel, "android">, draftId: string, revision: number) {
  return db().begin(async sql => {
    await profile(uid, sql);
    const [draft] = await sql`select *, expires_at <= now() as expired from joymilk.order_drafts
      where id = ${draftId} and actor = ${uid} and channel = ${channel} for update`;
    if (!draft) fail(404, "draft_not_found", "Draf tidak ditemukan.");
    if (draft.order_id) return { order: await getOrder(sql, draft.order_id) };
    if (draft.cancelled_at) fail(409, "draft_cancelled", "Draf sudah dibatalkan.");
    if (draft.expired) fail(409, "draft_expired", "Draf kedaluwarsa. Buat draf baru.");
    const input = orderInput.parse(draft.input);
    const preview = await snapshot(sql, input.items);
    const [comparison] = await sql`select ${sql.json(preview)}::jsonb = ${sql.json(draft.preview)}::jsonb as matches`;
    if (!comparison.matches) {
      draft.revision++;
      await sql`update joymilk.order_drafts set preview = ${sql.json(preview)}, revision = ${draft.revision} where id = ${draftId}`;
    }
    if (!comparison.matches || revision !== draft.revision) return { review: { draftId, revision: draft.revision, ...input, ...preview } };
    const order = await persist(sql, uid, channel, draftId, input, preview);
    await sql`update joymilk.order_drafts set order_id = ${order.id} where id = ${draftId}`;
    return { order };
  });
}
