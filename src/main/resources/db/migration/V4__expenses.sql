create table if not exists expenses (
    id uuid primary key,
    name varchar(255) not null,
    category varchar(120) not null,
    type varchar(32) not null default 'DIFERIDO',
    total_amount numeric(19,2) not null,
    months integer not null,
    monthly_amount numeric(19,2) not null,
    due_day integer not null,
    priority varchar(32) not null,
    active boolean not null default true,
    created_at timestamp not null default now()
);
