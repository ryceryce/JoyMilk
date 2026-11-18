import { z } from "zod";
import { db, fail } from "./core";

const productInput = z.object({
  name: z.string().trim().min(1).max(200),
  category: z.string().trim().min(1).max(100),
  size: z.string().trim().max(100).default(""),
  description: z.string().trim().max(2000).default(""),
  unit: z.string().trim().min(1).max(100),
  currentPrice: z.string().regex(/^\d{1,19}$/).pipe(z.string().refine(value => BigInt(value) <= 9223372036854775807n)),
  isActive: z.boolean().default(true),
});

export function getProducts(activeOnly: boolean, query = "") {
  const sql = db();
  return sql`select p.id, p.name, c.name as category, p.size, p.description, p.unit,
    p.current_price::text as "currentPrice", p.is_active as "isActive",
    p.created_at as "createdAt", p.updated_at as "updatedAt"
    from joymilk.products p join joymilk.categories c on c.id = p.category_id
    where (${!activeOnly} or p.is_active) and strpos(lower(p.name || ' ' || p.size), lower(${query})) > 0
    order by p.name, p.id`;
}
export function getCategories() {
  return db()`select name from joymilk.categories order by name`;
}
export async function addCategory(raw: unknown) {
  const { name } = z.object({ name: z.string().trim().min(1).max(100) }).parse(raw);
  await db()`insert into joymilk.categories (name) values (${name}) on conflict do nothing`;
}
export async function saveProduct(actor: string, raw: unknown, id?: string) {
  const activeOnly = z.object({ isActive: z.boolean() }).strict().safeParse(raw);
  if (id && activeOnly.success) {
    const rows = await db()`update joymilk.products set is_active = ${activeOnly.data.isActive},
      updated_at = now() where id = ${id} returning id`;
    if (!rows.length) fail(404, "product_not_found", "Produk tidak ditemukan.");
    return rows[0];
  }
  const p = productInput.parse(raw);
  return db().begin(async sql => {
    const [category] = await sql`select id from joymilk.categories where lower(name) = lower(${p.category})`;
    if (!category) fail(422, "unknown_category", "Kategori tidak ditemukan.");
    if (!id) {
      const [product] = await sql`insert into joymilk.products (category_id, name, size, description, unit, current_price, is_active)
        values (${category.id}, ${p.name}, ${p.size}, ${p.description}, ${p.unit}, ${p.currentPrice}, ${p.isActive}) returning id`;
      return product;
    }
    const [previous] = await sql`select current_price::text as price from joymilk.products where id = ${id} for update`;
    if (!previous) fail(404, "product_not_found", "Produk tidak ditemukan.");
    await sql`update joymilk.products set category_id = ${category.id}, name = ${p.name}, size = ${p.size},
      description = ${p.description}, unit = ${p.unit}, current_price = ${p.currentPrice},
      is_active = ${p.isActive}, updated_at = now() where id = ${id}`;
    if (BigInt(previous.price) !== BigInt(p.currentPrice))
      await sql`insert into joymilk.price_history (product_id, price, changed_by) values (${id}, ${p.currentPrice}, ${actor})`;
    return { id };
  });
}
export function getPriceHistory(id: string) {
  return db()`select id, price::text as price, effective_at as "effectiveDate", changed_by as "changedBy"
    from joymilk.price_history where product_id = ${id} order by effective_at desc`;
}
