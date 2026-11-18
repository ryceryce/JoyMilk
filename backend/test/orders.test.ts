import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import test from "node:test";
import postgres from "postgres";
import { createOrder, confirmOrder, editOrder, prepareOrder, setStatus } from "../lib/orders";
import { saveProduct } from "../lib/catalog";
import { db, digest } from "../lib/core";
import { handleMcp, withTelegramMcp } from "../lib/mcp";
import { telegramWebhook } from "../lib/telegram";

const testUrl = process.env.JOYMILK_TEST_OWNER_URL;
test("Postgres transactions preserve snapshots and prevent duplicate orders across channels", { skip: !testUrl }, async t => {
  const owner = postgres(testUrl!, { prepare: false, max: 1 });
  t.after(async () => { await db().end(); await owner.end(); });
  const uid = randomUUID();
  const staffUid = randomUUID();
  const category = "Test " + uid;
  await owner`insert into joymilk.users (firebase_uid, name, email, role)
    values (${uid}, 'Admin', ${uid + "@example.com"}, 'admin'),
    (${staffUid}, 'Staff', ${staffUid + "@example.com"}, 'staff')`;
  await owner`insert into joymilk.categories (name) values (${category})`;
  const product = { name: "Susu " + uid, category, currentPrice: "10000", unit: "botol" };
  const { id } = await saveProduct(uid, product);
  const form = {
    customerName: "Rina", paymentMethod: "tunai",
    items: [{ productId: id, quantity: 2, sweetLevel: "NORMAL" }],
  };

  const key = randomUUID();
  const [first, replay] = await Promise.all([createOrder(uid, key, form), createOrder(uid, key, form)]);
  assert.equal(first.id, replay.id);
  assert.equal(first.totalAmount, "20000");
  assert.equal(first.items[0].priceAtOrder, "10000");
  await assert.rejects(createOrder(uid, key, { ...form, customerName: "Beda" }), { code: "idempotency_conflict" });

  await owner.unsafe("create function joymilk.reject_fixture_item() returns trigger language plpgsql as $$ begin if new.quantity = 777 then raise exception 'fixture'; end if; return new; end; $$");
  await owner.unsafe("create trigger reject_fixture_item before insert on joymilk.order_items for each row execute function joymilk.reject_fixture_item()");
  try {
    const broken = { ...form, items: [...form.items, { ...form.items[0], quantity: 777 }] };
    const brokenKey = randomUUID();
    await assert.rejects(createOrder(uid, brokenKey, broken), /fixture/);
    const [count] = await owner`select count(*)::int as n from joymilk.orders where created_by = ${uid}`;
    assert.equal(count.n, 1);
    const [claim] = await owner`select count(*)::int as n from joymilk.order_idempotency where key = ${brokenKey}`;
    assert.equal(claim.n, 0);
  } finally {
    await owner.unsafe("drop trigger reject_fixture_item on joymilk.order_items");
    await owner.unsafe("drop function joymilk.reject_fixture_item()");
  }

  const draft = await prepareOrder(uid, "telegram", form);
  await saveProduct(uid, { ...product, currentPrice: "12000" }, id);
  const [history] = await owner`select price::text as price from joymilk.price_history where product_id = ${id}`;
  assert.equal(history.price, "12000");
  assert.equal((await confirmOrder(uid, "telegram", draft.draftId, draft.revision)).review?.totalAmount, "24000");
  await assert.rejects(confirmOrder(staffUid, "telegram", draft.draftId, 2), { code: "draft_not_found" });
  const [confirmed, confirmedAgain] = await Promise.all([
    confirmOrder(uid, "telegram", draft.draftId, 2), confirmOrder(uid, "telegram", draft.draftId, 2),
  ]);
  assert.equal(confirmed.order?.id, confirmedAgain.order?.id);
  assert.equal(confirmed.order?.totalAmount, "24000");
  const [version] = await owner`select updated_at as stamp from joymilk.orders where id = ${first.id}`;
  const edit = await editOrder(first.id, { ...form, updatedAt: version.stamp.toISOString(),
    items: [{ ...form.items[0], id: first.items[0].id, quantity: 3 }] });
  assert.equal(edit.totalAmount, "30000");
  await assert.rejects(setStatus(first.id, { status: "selesai", updatedAt: version.stamp.toISOString() }), { code: "stale_order" });

  const token = randomUUID();
  await owner`insert into joymilk.mcp_tokens (token_hash, firebase_uid) values (${digest(token)}, ${uid})`;
  const mcp = (token: string) => handleMcp(new Request("http://localhost/api/mcp", {
    method: "POST", headers: { Authorization: "Bearer " + token, "Content-Type": "application/json",
      Accept: "application/json, text/event-stream" },
    body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }),
  }));
  assert.equal((await mcp("invalid")).status, 401);
  const tools = await (await mcp(token)).json();
  assert.deepEqual(tools.result.tools.map((tool: { name: string }) => tool.name), ["search_products", "prepare_order"]);
  await withTelegramMcp(uid, false, async client => {
    const result = await client.callTool({ name: "prepare_order", arguments: form });
    assert.notEqual(result.isError, true);
  });
  assert.equal((await telegramWebhook(new Request("http://localhost/api/telegram/webhook", { method: "POST" }))).status, 403);
  const sender = parseInt(uid.replaceAll("-", "").slice(0, 10), 16);
  await owner`insert into joymilk.telegram_identities values (${sender}, ${staffUid})`;
  const secrets = { TELEGRAM_WEBHOOK_SECRET: "telegram-test", TELEGRAM_BOT_TOKEN: "mock",
    OPENAI_API_KEY: "mock", OPENAI_MODEL: "mock" };
  const originals = Object.fromEntries(Object.keys(secrets).map(key => [key, process.env[key]]));
  Object.assign(process.env, secrets);
  t.after(() => {
    for (const [key, value] of Object.entries(originals)) {
      if (value === undefined) delete process.env[key];
      else process.env[key] = value;
    }
  });
  const sent: { text: string; reply_markup?: { inline_keyboard: { callback_data: string }[][] } }[] = [];
  t.mock.method(globalThis, "fetch", async (url: string, request: RequestInit) => {
    if (url.startsWith("https://api.openai.com/")) return Response.json({
      choices: [{ message: { content: JSON.stringify({
        customerName: "Rina", customerPhone: "", deliveryAddress: "", notes: "", paymentMethod: "tunai",
        items: [{ query: product.name, quantity: 2, sweetLevel: "NORMAL" }],
      }) } }],
    });
    if (url.endsWith("/sendMessage")) sent.push(JSON.parse(request.body as string));
    return Response.json({ ok: true });
  });
  const chat = { type: "private", id: sender };
  const webhook = (payload: object) => telegramWebhook(new Request("http://localhost/api/telegram/webhook", {
    method: "POST", headers: { "x-telegram-bot-api-secret-token": "telegram-test" },
    body: JSON.stringify(payload),
  }));
  const updateId = sender * 10;
  assert.equal((await webhook({ update_id: updateId, message: { chat, from: { id: sender }, text: "2 susu" } })).status, 204);
  const callbackData = sent[0].reply_markup?.inline_keyboard[0][0].callback_data;
  assert.ok(callbackData);
  const callback = (updateId: number) => webhook({ update_id: updateId, callback_query: {
    id: "test", from: { id: sender }, message: { chat }, data: callbackData,
  } });
  for (const attempt of [updateId + 1, updateId + 1, updateId + 2]) assert.equal((await callback(attempt)).status, 204);
  assert.equal(sent.length, 3);
  assert.equal(sent[1].text, sent[2].text);
  const [telegramOrders] = await owner`select count(*)::int as n from joymilk.orders where created_by = ${staffUid}`;
  assert.equal(telegramOrders.n, 1);

  await owner`update joymilk.users set is_active = false where firebase_uid = ${uid}`;
  await assert.rejects(createOrder(uid, randomUUID(), form), { code: "inactive_user" });
});
