-- PostgreSQL 초기 스키마. 새 PostgreSQL DB에 적용한다.
-- 이후 스키마 변경은 V2, V3 등 새 마이그레이션으로 추가한다.

create table app_user (
    id uuid not null,
    생성일 timestamp(6),
    "익명 유저 키" varchar(255) not null,
    "유저 상태" varchar(255) check ("유저 상태" in ('ACTIVE','INACTIVE')) not null,
    primary key (id),
    constraint uk_app_user_anon_key unique ("익명 유저 키")
);

create table household (
    id uuid not null,
    생성일 timestamp(6),
    created_by uuid not null,
    name varchar(100) not null,
    primary key (id),
    constraint fk_household_created_by foreign key (created_by) references app_user (id)
);

create table household_member (
    household_id uuid not null,
    user_id uuid not null,
    joined_at timestamp(6) with time zone not null,
    nickname varchar(20) not null,
    role varchar(255) check (role in ('MEMBER','OWNER')) not null,
    primary key (household_id, user_id),
    constraint fk_household_member_household foreign key (household_id) references household (id),
    constraint fk_household_member_user foreign key (user_id) references app_user (id)
);
-- 내가 속한 공간 목록(GET /me)
create index idx_household_member_user on household_member (user_id);

create table invitation (
    id uuid not null,
    생성일 timestamp(6),
    household_id uuid not null,
    token_hash varchar(64) not null,
    max_uses integer not null,
    used_count integer not null,
    expires_at timestamp(6) with time zone not null,
    revoked_at timestamp(6) with time zone,
    version bigint not null,
    primary key (id),
    constraint uk_invitation_token_hash unique (token_hash)
);
create index idx_invitation_household on invitation (household_id);

create table item (
    id uuid not null,
    생성일 timestamp(6),
    household_id uuid not null,
    name varchar(100) not null,
    category varchar(20) check (category in ('CLEANING','KITCHEN','LAUNDRY','OTHER','TOILETRIES')) not null,
    unit varchar(20) not null,
    quantity integer not null,
    low_stock_threshold integer not null,
    version bigint not null,
    primary key (id)
);
-- 공간의 물품 목록
create index idx_item_household on item (household_id);

create table stock_change (
    id uuid not null,
    생성일 timestamp(6),
    item_id uuid not null,
    actor_user_id uuid not null,
    actor_nickname varchar(20) not null,
    before_quantity integer not null,
    after_quantity integer not null,
    primary key (id)
);
-- 물품의 변경 이력(최근순)
create index idx_stock_change_item_created on stock_change (item_id, 생성일);

create table alert_preference (
    household_id uuid not null,
    user_id uuid not null,
    enabled boolean not null,
    primary key (household_id, user_id)
);

create table low_stock_event (
    id uuid not null,
    생성일 timestamp(6),
    household_id uuid not null,
    item_id uuid not null,
    item_version bigint not null,
    primary key (id)
);
create index idx_low_stock_event_household on low_stock_event (household_id);

create table out_of_stock_alert (
    id uuid not null,
    생성일 timestamp(6),
    household_id uuid not null,
    item_id uuid not null,
    item_name varchar(255) not null,
    actor_user_id uuid not null,
    due_at timestamp(6) with time zone not null,
    reminders_sent integer not null,
    primary key (id)
);
-- 보낼 때가 된 알림 찾기, 물품·공간 단위 삭제
create index idx_out_of_stock_alert_due on out_of_stock_alert (due_at);
create index idx_out_of_stock_alert_item on out_of_stock_alert (item_id);
create index idx_out_of_stock_alert_household on out_of_stock_alert (household_id);
