-- V2: Auth Domain
-- Accounts and refresh token sessions for authentication

-- Account status enum
create type account_status as enum (
    'ACTIVE',
    'INACTIVE',
    'BANNED'
);

-- Accounts table
create table accounts (
    id uuid primary key,
    email citext not null unique,
    nick text not null,
    password text not null,
    status account_status not null default 'ACTIVE',
    avatar text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    constraint chk_non_empty_email check (length(trim(nick)) > 0)
);

create trigger update_accounts_updated_at
    before update on accounts
    for each row
    execute function update_updated_at_column();

-- Refresh sessions table
create table refresh_sessions (
    id uuid primary key,
    account_id uuid not null references accounts(id) on delete cascade,
    token text not null,
    prev_token text,
    last_used_at timestamp with time zone,
    revoked_at timestamp with time zone,
    revoke_reason text,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),
    constraint chk_revoked_reason check (
        (revoked_at is not null and revoke_reason is not null) or
        (revoked_at is null and revoke_reason is null)
    )
);

create trigger update_refresh_sessions_updated_at
    before update on refresh_sessions
    for each row
    execute function update_updated_at_column();

-- Indexes for refresh_sessions
create index idx_refresh_sessions_account_id on refresh_sessions(account_id);