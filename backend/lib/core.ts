import postgres from "postgres";
import { createHash } from "node:crypto";
import { cert, getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { ZodError } from "zod";

let connection: ReturnType<typeof postgres> | undefined;

export function db() {
  if (connection) return connection;
  const url = process.env.DATABASE_URL;
  if (!url) throw new Error("DATABASE_URL is missing");
  return connection = postgres(url, { prepare: false, max: 3, idle_timeout: 20 });
}
export type Sql = ReturnType<typeof db> | postgres.TransactionSql;
export const digest = (value: string) => createHash("sha256").update(value).digest("hex");
export const id = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string) { super(message); }
}
export function fail(status: number, code: string, message: string): never {
  throw new ApiError(status, code, message);
}
export function errorBody(error: unknown) {
  if (error instanceof ApiError) return { status: error.status, code: error.code, message: error.message };
  if (error instanceof ZodError) return {
    status: 422, code: "invalid_input", message: "Periksa kembali isian.",
    field: error.issues[0]?.path.join("."),
  };
  if (error instanceof SyntaxError) return { status: 400, code: "invalid_json", message: "Format JSON tidak valid." };
  if (error instanceof postgres.PostgresError && error.code === "23505")
    return { status: 409, code: "duplicate", message: "Data sudah tersedia." };
  console.error("joymilk_request_failed", error instanceof Error ? error.name : typeof error);
  return { status: 500, code: "internal_error", message: "Layanan sedang bermasalah. Coba lagi." };
}
export function bearer(request: Request) {
  return request.headers.get("authorization")?.match(/^Bearer (\S+)$/i)?.[1]
    ?? fail(401, "unauthenticated", "Silakan masuk kembali.");
}
export function firebase() {
  const existing = getApps()[0];
  if (existing) return getAuth(existing);
  const account = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!account) throw new Error("Firebase admin credentials are missing");
  return getAuth(initializeApp({ credential: cert(JSON.parse(account)) }));
}
export async function prepareFirebaseClaims(uid: string) {
  const auth = firebase();
  const user = await auth.getUser(uid);
  await auth.setCustomUserClaims(uid, { ...user.customClaims, role: "authenticated" });
}
export const userColumns = () => db()`firebase_uid as id, name, email, role, is_active as "isActive", created_at as "createdAt"`;
export async function profile(uid: string, sql: Sql = db()) {
  const [user] = await sql`select ${userColumns()} from joymilk.users where firebase_uid = ${uid} and is_active`;
  if (!user) fail(403, "inactive_user", "Akun JoyMilk belum aktif.");
  return user as { id: string; name: string; role: "admin" | "staff"; email: string; isActive: boolean; createdAt: Date };
}
export async function androidUser(request: Request) {
  const token = bearer(request);
  const auth = firebase();
  let uid: string;
  try { uid = (await auth.verifyIdToken(token, true)).uid; }
  catch (error) {
    const code = (error as { code?: string }).code;
    if (["auth/id-token-expired", "auth/id-token-revoked", "auth/argument-error", "auth/invalid-id-token", "auth/user-disabled", "auth/user-not-found"].includes(code ?? ""))
      fail(401, "unauthenticated", "Silakan masuk kembali.");
    throw error;
  }
  return profile(uid);
}
export function requireAdmin(role: string) {
  if (role !== "admin") fail(403, "forbidden", "Akses khusus admin.");
}
