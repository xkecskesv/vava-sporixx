-- Šporixx test schema, extracted from production sporixx.sqlite.
-- Used by JdbcTestSupport to bootstrap an in-memory SQLite for integration tests.
-- Keep in sync with whatever the JDBC repos expect.

CREATE TABLE currencies (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    code   TEXT NOT NULL UNIQUE,
    name   TEXT,
    symbol TEXT
);

CREATE TABLE regions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    locale_code         TEXT NOT NULL UNIQUE,
    decimal_separator   TEXT NOT NULL,
    thousands_separator TEXT NOT NULL,
    date_format         TEXT NOT NULL,
    time_format         TEXT NOT NULL,
    currency_id         INTEGER NOT NULL,
    FOREIGN KEY (currency_id) REFERENCES currencies(id)
);

CREATE TABLE account_types (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,
    first_name      TEXT NOT NULL,
    last_name       TEXT NOT NULL,
    photo_path      TEXT,
    gender          TEXT NOT NULL CHECK (gender IN ('M','F','ONHSR')),
    saving_xp       REAL NOT NULL DEFAULT 0.0,
    budget_xp       REAL NOT NULL DEFAULT 0.0,
    investor_xp     REAL NOT NULL DEFAULT 0.0,
    spender_xp      REAL NOT NULL DEFAULT 0.0,
    saving_level    INTEGER NOT NULL DEFAULT 0,
    budget_level    INTEGER NOT NULL DEFAULT 0,
    investor_level  INTEGER NOT NULL DEFAULT 0,
    spender_level   INTEGER NOT NULL DEFAULT 0,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id         INTEGER NOT NULL,
    region_id             INTEGER NOT NULL,
    description           TEXT,
    account_type_id       TEXT NOT NULL,
    default_currency_code TEXT NOT NULL,
    initial_balance       REAL NOT NULL DEFAULT 0.0,
    current_balance       REAL NOT NULL DEFAULT 0.0,
    is_active             INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE account_access (
    user_id       INTEGER NOT NULL,
    account_id    INTEGER NOT NULL,
    access_level  INTEGER NOT NULL,
    granted_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, account_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE categories (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER,
    name        TEXT NOT NULL,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE transaction_type (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transaction_status (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE spending_classification (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id                  INTEGER NOT NULL,
    name                        TEXT NOT NULL,
    category_id                 INTEGER NOT NULL,
    spending_classification_id  INTEGER,
    transaction_type_id         INTEGER NOT NULL,
    amount                      REAL NOT NULL CHECK (amount >= 0),
    status_id                   INTEGER NOT NULL,
    transaction_date            DATETIME NOT NULL,
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_type_id) REFERENCES transaction_type(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (status_id) REFERENCES transaction_status(id),
    FOREIGN KEY (spending_classification_id) REFERENCES spending_classification(id)
);

CREATE TABLE recurring_rules (
    id                          INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id                  INTEGER NOT NULL,
    category_id                 INTEGER NOT NULL,
    status_id                   INTEGER NOT NULL,
    transaction_type_id         INTEGER NOT NULL,
    spending_classification_id  INTEGER NOT NULL,
    amount                      REAL NOT NULL CHECK (amount >= 0),
    description                 TEXT,
    frequency_type              TEXT NOT NULL,
    frequency_interval          INTEGER NOT NULL CHECK (frequency_interval > 0),
    start_date                  DATETIME NOT NULL,
    next_due_date               DATETIME NOT NULL,
    end_date                    DATETIME,
    max_occurrences             INTEGER,
    generated_count             INTEGER NOT NULL DEFAULT 0,
    is_active                   INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at                  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE saving_goals (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id      INTEGER NOT NULL,
    name            TEXT NOT NULL,
    goal_type_id    INTEGER NOT NULL,
    target_amount   REAL NOT NULL CHECK (target_amount > 0),
    current_amount  REAL NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
    target_date     DATETIME,
    is_active       INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Seed dáta — referenčné tabuľky musia byť pred-naplnené
INSERT INTO currencies (id, code, name, symbol) VALUES (1, 'EUR', 'Euro', '€');
INSERT INTO regions (id, locale_code, decimal_separator, thousands_separator, date_format, time_format, currency_id)
    VALUES (1, 'sk_SK', ',', ' ', 'dd.MM.yyyy', 'HH:mm', 1);
INSERT INTO account_types (id, name) VALUES (1, 'Main'), (2, 'Emergency'), (3, 'Private'), (4, 'Saving');
INSERT INTO transaction_type (id, name) VALUES (1, 'INCOME'), (2, 'EXPENSE');
INSERT INTO transaction_status (id, name) VALUES (1, 'COMPLETED'), (2, 'PENDING');
INSERT INTO spending_classification (id, name) VALUES (1, 'NEED'), (2, 'WANT');

-- System kategórie (musia mať fixne ID 6 a 7 podľa Transaction.CATEGORY_SAVING / CATEGORY_SAVING_EXPENSE)
INSERT INTO categories (id, user_id, name) VALUES
    (1, NULL, 'Food'),
    (2, NULL, 'Clothing'),
    (3, NULL, 'Entertainment'),
    (4, NULL, 'Rent'),
    (5, NULL, 'Transportation'),
    (6, NULL, 'Saving'),
    (7, NULL, 'Saving Expense'),
    (8, NULL, 'Investment'),
    (9, NULL, 'Salary');
