create schema joymilk;
revoke all on schema joymilk from public;

create table joymilk.users (
    firebase_uid text primary key,
    name text not null,
    email text not null,
    role text not null check (role in ('admin', 'staff')),
    is_active boolean not null default true,
    created_at timestamptz not null default now()
);
create unique index users_email_key on joymilk.users (lower(email));

create table joymilk.categories (
    id uuid primary key default gen_random_uuid(),
    name text not null
);
create unique index categories_name_key on joymilk.categories (lower(name));

create table joymilk.products (
    id uuid primary key default gen_random_uuid(),
    category_id uuid not null references joymilk.categories,
    name text not null,
    size text not null default '',
    description text not null default '',
    unit text not null,
    current_price bigint not null check (current_price >= 0),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create table joymilk.price_history (
    id uuid primary key default gen_random_uuid(),
    product_id uuid not null references joymilk.products,
    price bigint not null check (price >= 0),
    effective_at timestamptz not null default now(),
    changed_by text not null references joymilk.users
);
create index price_history_product_time on joymilk.price_history (product_id, effective_at desc);

create table joymilk.orders (
    id uuid primary key default gen_random_uuid(),
    customer_name text not null,
    customer_phone text not null default '',
    delivery_address text not null default '',
    payment_method text not null check (payment_method in ('tunai', 'transfer', 'qris')),
    status text not null default 'pending' check (status in ('pending', 'diproses', 'selesai', 'dibatalkan')),
    total_amount bigint not null check (total_amount >= 0),
    created_by text not null references joymilk.users,
    notes text not null default '',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
create index orders_time on joymilk.orders (created_at desc, id);
create index orders_status_time on joymilk.orders (status, created_at desc, id);

create table joymilk.order_items (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references joymilk.orders,
    product_id uuid not null references joymilk.products,
    product_name text not null,
    price_at_order bigint not null check (price_at_order >= 0),
    quantity integer not null check (quantity > 0),
    subtotal bigint not null check (subtotal = price_at_order * quantity),
    sweet_level text not null check (sweet_level in ('TANPA_GULA', 'LESS_SUGAR', 'NORMAL', 'EXTRA_SWEET'))
);
create index order_items_order on joymilk.order_items (order_id);

create table joymilk.order_drafts (
    id uuid primary key default gen_random_uuid(),
    actor text not null references joymilk.users,
    channel text not null check (channel in ('telegram', 'mcp')),
    input jsonb not null,
    preview jsonb not null,
    revision integer not null default 1,
    expires_at timestamptz not null default now() + interval '30 minutes',
    order_id uuid references joymilk.orders,
    cancelled_at timestamptz
);
create table joymilk.order_idempotency (
    actor text not null references joymilk.users,
    channel text not null check (channel in ('android', 'telegram', 'mcp')),
    key text not null,
    fingerprint text not null,
    order_id uuid references joymilk.orders,
    primary key (actor, channel, key)
);
create table joymilk.mcp_tokens (
    token_hash text primary key,
    firebase_uid text not null references joymilk.users,
    can_confirm boolean not null default false,
    revoked_at timestamptz
);
create table joymilk.telegram_identities (
    telegram_user_id bigint primary key,
    firebase_uid text not null unique references joymilk.users
);
create table joymilk.telegram_updates (
    update_id bigint primary key,
    reply jsonb,
    lease uuid,
    lease_until timestamptz,
    delivered_at timestamptz
);
create table joymilk.telegram_context (
    telegram_user_id bigint primary key references joymilk.telegram_identities on delete cascade,
    messages jsonb not null,
    expires_at timestamptz not null
);

create function joymilk.keep_item_snapshot() returns trigger language plpgsql as $$
begin
    if (new.product_id, new.product_name, new.price_at_order) is distinct from
       (old.product_id, old.product_name, old.price_at_order) then
        raise exception 'Order item snapshot is immutable';
    end if;
    return new;
end;
$$;
create trigger order_item_snapshot before update on joymilk.order_items
    for each row execute function joymilk.keep_item_snapshot();

do $$ begin
    if not exists (select from pg_roles where rolname = 'joymilk_runtime') then
        create role joymilk_runtime login;
    end if;
end $$;
grant usage on schema joymilk to joymilk_runtime;
grant select on all tables in schema joymilk to joymilk_runtime;
grant insert, update on joymilk.users, joymilk.products, joymilk.orders, joymilk.order_drafts,
    joymilk.order_idempotency, joymilk.telegram_updates, joymilk.telegram_context to joymilk_runtime;
grant insert on joymilk.categories, joymilk.price_history to joymilk_runtime;
grant insert, update, delete on joymilk.order_items to joymilk_runtime;
grant delete on joymilk.telegram_context to joymilk_runtime;
