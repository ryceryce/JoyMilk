create function joymilk.can_receive_changes() returns boolean
language sql security definer set search_path = ''
as $$
    select exists (
        select 1 from joymilk.users
        where firebase_uid = (select auth.jwt() ->> 'sub') and is_active
    );
$$;
revoke all on function joymilk.can_receive_changes() from public;
grant usage on schema joymilk to authenticated;
grant execute on function joymilk.can_receive_changes() to authenticated;

create policy "JoyMilk active profiles receive invalidations" on realtime.messages
for select to authenticated using (
    (select realtime.topic()) = 'joymilk:changes'
    and extension = 'broadcast' and (select joymilk.can_receive_changes())
);

create function joymilk.notify_change() returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
    perform realtime.send('{}'::jsonb, tg_table_name, 'joymilk:changes', true);
    return null;
end;
$$;
revoke all on function joymilk.notify_change() from public;

create trigger notify_orders after insert or update or delete on joymilk.orders
for each statement execute function joymilk.notify_change();
create trigger notify_products after insert or update on joymilk.products
for each statement execute function joymilk.notify_change();
create trigger notify_categories after insert on joymilk.categories
for each statement execute function joymilk.notify_change();
create trigger notify_users after insert or update on joymilk.users
for each statement execute function joymilk.notify_change();
