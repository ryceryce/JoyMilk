import { timingSafeEqual } from "node:crypto";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { WebStandardStreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/webStandardStreamableHttp.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { z } from "zod";
import { bearer, db, digest, errorBody, fail, profile } from "./core";
import { getProducts } from "./catalog";
import { confirmOrder, orderInput, prepareOrder } from "./orders";

export function sameSecret(a: string, b: string) {
  return timingSafeEqual(Buffer.from(digest(a), "hex"), Buffer.from(digest(b), "hex"));
}
type Actor = { uid: string; channel: "telegram" | "mcp"; canConfirm: boolean };

function serverFor(actor: Actor) {
  const server = new McpServer({ name: "joymilk", version: "1.0.0" });
  async function tool(fn: () => Promise<unknown>) {
    try {
      await profile(actor.uid);
      const result = await fn();
      return { content: [{ type: "text" as const, text: JSON.stringify(result) }] };
    } catch (error) {
      const { code, message } = errorBody(error);
      return { isError: true, content: [{ type: "text" as const, text: JSON.stringify({ code, message }) }] };
    }
  }

  server.registerTool("search_products", {
    description: "Cari produk aktif di JoyMilk.",
    inputSchema: { query: z.string().trim().max(200).default("") },
    annotations: { readOnlyHint: true },
  }, ({ query }) => tool(async () => (await getProducts(true, query)).slice(0, 20)));

  server.registerTool("prepare_order", {
    description: "Buat draf JoyMilk dan tampilkan ringkasannya untuk ditinjau.",
    inputSchema: orderInput,
  }, input => tool(() => prepareOrder(actor.uid, actor.channel, input)));

  if (actor.canConfirm) server.registerTool("confirm_order", {
    description: "Simpan sekali setelah pengguna menyetujui revisi ringkasan. Tampilkan ulang bila review berubah.",
    inputSchema: { draftId: z.uuid(), revision: z.number().int().positive() },
    annotations: { idempotentHint: true },
  }, ({ draftId, revision }) => tool(() => confirmOrder(actor.uid, actor.channel, draftId, revision)));
  return server;
}

export async function handleMcp(request: Request) {
  try {
    const origin = request.headers.get("origin");
    if (origin && origin !== new URL(request.url).origin)
      fail(403, "invalid_origin", "Origin tidak diizinkan.");
    const tokenHash = digest(bearer(request));
    const [token] = await db()`select firebase_uid, can_confirm from joymilk.mcp_tokens
      where token_hash = ${tokenHash} and revoked_at is null`;
    if (!token) fail(401, "invalid_token", "Token MCP tidak valid.");
    await profile(token.firebase_uid);
    const server = serverFor({ uid: token.firebase_uid, canConfirm: token.can_confirm, channel: "mcp" });
    const transport = new WebStandardStreamableHTTPServerTransport({
      sessionIdGenerator: undefined, enableJsonResponse: true,
    });
    await server.connect(transport);
    try { return await transport.handleRequest(request); }
    finally { await server.close(); }
  } catch (error) {
    const { status, ...body } = errorBody(error);
    return Response.json(body, { status });
  }
}

export async function withTelegramMcp<T>(uid: string, canConfirm: boolean, run: (client: Client) => Promise<T>) {
  const [clientTransport, serverTransport] = InMemoryTransport.createLinkedPair();
  const server = serverFor({ uid, canConfirm, channel: "telegram" });
  const client = new Client({ name: "joymilk-telegram", version: "1.0.0" });
  await server.connect(serverTransport);
  try {
    await client.connect(clientTransport);
    return await run(client);
  } finally {
    await client.close();
    await server.close();
  }
}
