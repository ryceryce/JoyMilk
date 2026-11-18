import { randomBytes } from "node:crypto";
import { z } from "zod";
import { db, digest, firebase, prepareFirebaseClaims, profile } from "../lib/core";

const [, , command, uid, value] = process.argv;

async function main() {
  if (!uid) throw new Error("Usage: npm run admin -- profile|link|unlink|token|revoke <firebase-uid> [role|telegram-id|confirm|token-hash]");
  const sql = db();
  if (command === "profile") {
    const role = z.enum(["admin", "staff"]).parse(value);
    const user = await firebase().getUser(uid);
    if (!user.email || user.disabled) throw new Error("Firebase user must have an email and be enabled");
    await prepareFirebaseClaims(uid);
    await sql`insert into joymilk.users (firebase_uid, name, email, role)
      values (${uid}, ${user.displayName || user.email}, ${user.email}, ${role})
      on conflict (firebase_uid) do update set email = excluded.email, role = excluded.role`;
    console.log("Profile ready for", uid);
  } else if (command === "link") {
    await profile(uid);
    const telegramId = z.string().regex(/^\d{1,16}$/).parse(value);
    await sql`insert into joymilk.telegram_identities (telegram_user_id, firebase_uid) values (${telegramId}, ${uid})`;
    console.log("Telegram linked");
  } else if (command === "unlink") {
    await sql`delete from joymilk.telegram_identities where firebase_uid = ${uid}`;
    console.log("Telegram unlinked");
  } else if (command === "token") {
    await profile(uid);
    if (value && value !== "confirm") throw new Error("Use confirm to grant order confirmation");
    const token = randomBytes(32).toString("base64url");
    const hash = digest(token);
    await sql`insert into joymilk.mcp_tokens (token_hash, firebase_uid, can_confirm)
      values (${hash}, ${uid}, ${value === "confirm"})`;
    console.log("Token (shown once):", token);
    console.log("Revocation ID:", hash);
  } else if (command === "revoke") {
    const hash = z.string().regex(/^[a-f0-9]{64}$/).parse(value);
    await sql`update joymilk.mcp_tokens set revoked_at = now() where firebase_uid = ${uid} and token_hash = ${hash}`;
    console.log("Revocation recorded");
  } else throw new Error("Unknown admin command");
}

main().catch(error => {
  console.error(error instanceof Error ? error.message : "Admin command failed");
  process.exitCode = 1;
}).finally(() => db().end());
