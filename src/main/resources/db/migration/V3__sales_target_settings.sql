create table if not exists sales_target_settings (
    id bigserial primary key,
    fixed_cost_global numeric(19,2) not null default 300
);

insert into sales_target_settings (fixed_cost_global)
select 300
where not exists (select 1 from sales_target_settings);
