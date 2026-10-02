insert into products (id, name, price)
values (45, 'Boots', 9.99)
ON CONFLICT do nothing;
@@

insert into products (id, name, price)
values (2, 'Jackets', 49.99)
ON CONFLICT do nothing;
@@

insert into products (id, name, price)
values (3, 'Jeans', 100)
ON CONFLICT do nothing;
@@
