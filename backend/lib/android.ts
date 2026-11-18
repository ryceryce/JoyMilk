import { z } from "zod";
import { androidUser, db, errorBody, fail, firebase, id, prepareFirebaseClaims, requireAdmin, userColumns } from "./core";
import { addCategory, getCategories, getPriceHistory, getProducts, saveProduct } from "./catalog";
import { createOrder, editOrder, getOrder, orderColumns, setStatus, statuses } from "./orders";

const orderPath = new RegExp("^orders/(" + id + ")(/status)?$", "i");
const productPath = new RegExp("^products/(" + id + ")(/price-history)?$", "i");
const adminUserInput = z.object({
  name: z.string().trim().min(1).max(200), email: z.email().max(254),
  password: z.string().min(6).max(128), role: z.enum(["admin", "staff"]),
});

async function addUser(raw: unknown) {
  const input = adminUserInput.parse(raw);
  const auth = firebase();
  let account;
  try { account = await auth.createUser({ email: input.email, password: input.password, displayName: input.name }); }
  catch (error) {
    if ((error as { code?: string }).code === "auth/email-already-exists")
      fail(409, "duplicate_user", "Email sudah terdaftar.");
    throw error;
  }
  try {
    await prepareFirebaseClaims(account.uid);
    await db()`insert into joymilk.users (firebase_uid, name, email, role)
      values (${account.uid}, ${input.name}, ${input.email}, ${input.role})`;
  } catch (error) {
    try {
      const [written] = await db()`select 1 from joymilk.users where firebase_uid = ${account.uid}`;
      if (written) return { id: account.uid };
      await auth.deleteUser(account.uid);
    } catch {
      console.error("joymilk_user_provisioning_needs_reconciliation", account.uid);
    }
    throw error;
  }
  return { id: account.uid };
}

async function report(from: string | null, to: string | null) {
  const range = z.object({ from: z.iso.date(), to: z.iso.date() })
    .refine(value => value.from <= value.to).parse({ from, to });
  const sql = db();
  const [totals] = await sql`select coalesce(sum(total_amount), 0)::text as "totalRevenue", count(*)::int as "orderCount"
    from joymilk.orders where status <> 'dibatalkan'
      and created_at >= (${range.from}::date::timestamp at time zone 'Asia/Jakarta')
      and created_at < ((${range.to}::date + 1)::timestamp at time zone 'Asia/Jakarta')`;
  const topProducts = await sql`select i.product_name as name, sum(i.quantity)::text as quantity
    from joymilk.order_items i join joymilk.orders o on o.id = i.order_id where o.status <> 'dibatalkan'
      and o.created_at >= (${range.from}::date::timestamp at time zone 'Asia/Jakarta')
      and o.created_at < ((${range.to}::date + 1)::timestamp at time zone 'Asia/Jakarta')
    group by i.product_name order by sum(i.quantity) desc, i.product_name limit 5`;
  return { ...totals, topProducts };
}

export async function handleAndroid(request: Request) {
  try {
    const user = await androidUser(request);
    const url = new URL(request.url);
    const path = url.pathname.slice("/api/v1/".length);
    const method = request.method;
    const sql = db();
    const admin = () => requireAdmin(user.role);
    if (method === "GET" && path === "me") return Response.json(user);
    if (method === "GET" && path === "products") {
      return Response.json(await getProducts(user.role !== "admin" || url.searchParams.get("active") === "true"));
    }
    if (method === "POST" && path === "products") {
      admin();
      return Response.json(await saveProduct(user.id, await request.json()), { status: 201 });
    }
    const product = productPath.exec(path);
    if (product) {
      admin();
      if (method === "GET" && product[2]) return Response.json(await getPriceHistory(product[1]));
      if (method === "PATCH" && !product[2]) return Response.json(await saveProduct(user.id, await request.json(), product[1]));
    }
    if (method === "GET" && path === "categories") return Response.json(await getCategories());
    if (method === "POST" && path === "categories") {
      admin();
      await addCategory(await request.json());
      return new Response(null, { status: 204 });
    }
    if (method === "GET" && path === "orders") {
      const input = url.searchParams.get("status");
      const status = input ? statuses.parse(input) : null;
      return Response.json(await sql`select ${orderColumns()} from joymilk.orders
        where (${status}::text is null or status = ${status}) order by created_at desc, id`);
    }
    if (method === "POST" && path === "orders") {
      const key = z.uuid().parse(request.headers.get("idempotency-key"));
      return Response.json(await createOrder(user.id, key, await request.json()), { status: 201 });
    }
    const order = orderPath.exec(path);
    if (order) {
      if (method === "GET" && !order[2]) return Response.json(await sql.begin(tx => getOrder(tx, order[1])));
      if (method === "PATCH" && !order[2]) return Response.json(await editOrder(order[1], await request.json()));
      if (method === "PATCH" && order[2]) {
        await setStatus(order[1], await request.json());
        return new Response(null, { status: 204 });
      }
    }
    if (method === "GET" && path === "reports")
      return Response.json(await report(url.searchParams.get("from"), url.searchParams.get("to")));
    if (method === "GET" && path === "users") {
      admin();
      return Response.json(await sql`select ${userColumns()} from joymilk.users order by name, firebase_uid`);
    }
    if (method === "POST" && path === "users") {
      admin();
      return Response.json(await addUser(await request.json()), { status: 201 });
    }
    if (method === "PATCH" && /^users\/[^/]+$/.test(path)) {
      admin();
      const uid = decodeURIComponent(path.slice(6));
      const { isActive } = z.object({ isActive: z.boolean() }).parse(await request.json());
      if (uid === user.id && !isActive) fail(422, "self_deactivation", "Akun sendiri tidak bisa dinonaktifkan.");
      const rows = await sql`update joymilk.users set is_active = ${isActive} where firebase_uid = ${uid} returning firebase_uid`;
      if (!rows.length) fail(404, "user_not_found", "Pengguna tidak ditemukan.");
      return new Response(null, { status: 204 });
    }
    fail(404, "not_found", "Halaman tidak ditemukan.");
  } catch (error) {
    const { status, ...body } = errorBody(error);
    return Response.json(body, { status });
  }
}
