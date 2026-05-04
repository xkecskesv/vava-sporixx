
CREATE TABLE IF NOT EXISTS currencies (
    id     INTEGER PRIMARY KEY AUTOINCREMENT,
    code   TEXT NOT NULL UNIQUE,
    name   TEXT,
    symbol TEXT
);

CREATE TABLE IF NOT EXISTS regions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    locale_code         TEXT NOT NULL UNIQUE,
    decimal_separator   TEXT NOT NULL,
    thousands_separator TEXT NOT NULL,
    date_format         TEXT NOT NULL,
    time_format         TEXT NOT NULL,
    currency_id         INTEGER NOT NULL,
    FOREIGN KEY (currency_id) REFERENCES currencies(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS exchange_rates (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    base_currency_code   TEXT NOT NULL,
    target_currency_code TEXT NOT NULL,
    rate                 REAL NOT NULL,
    captured_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (base_currency_code)   REFERENCES currencies(code) ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (target_currency_code) REFERENCES currencies(code) ON UPDATE CASCADE ON DELETE RESTRICT,
    UNIQUE (base_currency_code, target_currency_code, captured_at)
);

CREATE TABLE IF NOT EXISTS account_types (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT NOT NULL,
    description TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    email          TEXT NOT NULL UNIQUE,
    password_hash  TEXT NOT NULL,
    first_name     TEXT NOT NULL,
    last_name      TEXT NOT NULL,
    language_code  TEXT NOT NULL,
    currency_code  TEXT NOT NULL,
    photo_path     TEXT,
    gender         TEXT NOT NULL CHECK (gender IN ('M','F','ONHSR')),
    saving_xp      REAL    NOT NULL DEFAULT 0.0,
    budget_xp      REAL    NOT NULL DEFAULT 0.0,
    investor_xp    REAL    NOT NULL DEFAULT 0.0,
    spender_xp     REAL    NOT NULL DEFAULT 0.0,
    saving_level   INTEGER NOT NULL DEFAULT 0 CHECK (saving_level   IN (0,1,2,3,4,5)),
    budget_level   INTEGER NOT NULL DEFAULT 0 CHECK (budget_level   IN (0,1,2,3,4,5)),
    investor_level INTEGER NOT NULL DEFAULT 0 CHECK (investor_level IN (0,1,2,3,4,5)),
    spender_level  INTEGER NOT NULL DEFAULT 0 CHECK (spender_level  IN (0,1,2,3,4,5)),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_user_id         INTEGER NOT NULL,
    region_id             INTEGER NOT NULL DEFAULT 1,
    account_type_id       INTEGER NOT NULL,
    initial_balance       REAL    NOT NULL DEFAULT 0.0,
    current_balance       REAL    NOT NULL DEFAULT 0.0,
    is_active             INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    description           TEXT,
    created_at            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id)   REFERENCES users(id)         ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (region_id)       REFERENCES regions(id)       ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (account_type_id) REFERENCES account_types(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS account_access (
    user_id      INTEGER NOT NULL,
    account_id   INTEGER NOT NULL,
    access_level INTEGER NOT NULL,
    granted_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, account_id),
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    name       TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, name),
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_type (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transaction_status (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS spending_classification (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL UNIQUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id                         INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id                 INTEGER NOT NULL,
    name                       TEXT    NOT NULL,
    category_id                INTEGER NOT NULL,
    spending_classification_id INTEGER,
    transaction_type_id        INTEGER NOT NULL,
    amount                     REAL    NOT NULL CHECK (amount >= 0),
    status_id                  INTEGER NOT NULL,
    transaction_date           DATETIME NOT NULL,
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id)                 REFERENCES accounts(id)               ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (category_id)                REFERENCES categories(id)             ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (transaction_type_id)        REFERENCES transaction_type(id)       ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (status_id)                  REFERENCES transaction_status(id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (spending_classification_id) REFERENCES spending_classification(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS recurring_rules (
    id                         INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id                 INTEGER NOT NULL,
    category_id                INTEGER NOT NULL,
    status_id                  INTEGER NOT NULL,
    transaction_type_id        INTEGER NOT NULL,
    spending_classification_id INTEGER NOT NULL,
    amount                     REAL    NOT NULL CHECK (amount >= 0),
    description                TEXT,
    frequency_type             TEXT    NOT NULL,
    frequency_interval         INTEGER NOT NULL CHECK (frequency_interval > 0),
    start_date                 DATETIME NOT NULL,
    next_due_date              DATETIME NOT NULL,
    end_date                   DATETIME,
    max_occurrences            INTEGER,
    generated_count            INTEGER NOT NULL DEFAULT 0 CHECK (generated_count >= 0),
    is_active                  INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    created_at                 DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id)                 REFERENCES accounts(id)               ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (category_id)                REFERENCES categories(id)             ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (transaction_type_id)        REFERENCES transaction_type(id)       ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (status_id)                  REFERENCES transaction_status(id)     ON UPDATE CASCADE ON DELETE RESTRICT,
    FOREIGN KEY (spending_classification_id) REFERENCES spending_classification(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS saving_goals (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id     INTEGER NOT NULL,
    name           TEXT    NOT NULL,
    goal_type_id   INTEGER NOT NULL,
    target_amount  REAL    NOT NULL CHECK (target_amount > 0),
    current_amount REAL    NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
    target_date    DATETIME,
    is_active      INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id)   REFERENCES accounts(id)   ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (goal_type_id) REFERENCES categories(id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS saving_goal_contributions (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    saving_goal_id    INTEGER NOT NULL,
    transaction_id    INTEGER NOT NULL,
    amount            REAL    NOT NULL CHECK (amount > 0),
    contribution_date DATETIME NOT NULL,
    created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (saving_goal_id)  REFERENCES saving_goals(id)  ON UPDATE CASCADE ON DELETE CASCADE,
    FOREIGN KEY (transaction_id)  REFERENCES transactions(id)  ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS saving_milestones (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    saving_goal_id   INTEGER NOT NULL,
    label            TEXT    NOT NULL,
    milestone_amount REAL    NOT NULL CHECK (milestone_amount > 0),
    achieved_at      DATETIME,
    FOREIGN KEY (saving_goal_id) REFERENCES saving_goals(id) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_budgets (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id                INTEGER NOT NULL,
    monthly_income         REAL NOT NULL CHECK (monthly_income >= 0),
    food                   REAL NOT NULL CHECK (food >= 0),
    rent                   REAL NOT NULL CHECK (rent >= 0),
    transport              REAL NOT NULL CHECK (transport >= 0),
    utilities              REAL NOT NULL CHECK (utilities >= 0),
    other                  REAL NOT NULL CHECK (other >= 0),
    essential_expenses     REAL NOT NULL CHECK (essential_expenses >= 0),
    emergency_fund         REAL NOT NULL CHECK (emergency_fund >= 0),
    savings                REAL NOT NULL CHECK (savings >= 0),
    to_invest              REAL NOT NULL CHECK (to_invest >= 0),
    fun_money              REAL NOT NULL CHECK (fun_money >= 0),
    minimal_emergency_fund REAL NOT NULL CHECK (minimal_emergency_fund >= 0),
    optimal_emergency_fund REAL NOT NULL CHECK (optimal_emergency_fund >= 0),
    period_type            TEXT NOT NULL,
    start_date             DATETIME,
    end_date               DATETIME,
    is_active              INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0,1)),
    created_at             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT check_total_allocation CHECK (
        ROUND(essential_expenses + emergency_fund + savings + to_invest + fun_money, 2)
        <= ROUND(monthly_income, 2)
    )
);

CREATE TABLE IF NOT EXISTS family_requests (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    from_user_id INTEGER NOT NULL,
    to_user_id   INTEGER NOT NULL,
    status       TEXT    NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (from_user_id) REFERENCES users(id),
    FOREIGN KEY (to_user_id)   REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS user_notification_settings (
    user_id            INTEGER PRIMARY KEY,
    notif_upcoming     INTEGER NOT NULL DEFAULT 1,
    notif_budget       INTEGER NOT NULL DEFAULT 1,
    notif_reminders    INTEGER NOT NULL DEFAULT 1,
    notif_goals        INTEGER NOT NULL DEFAULT 1,
    notif_achievements INTEGER NOT NULL DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS milestones (
    badge_name TEXT NOT NULL CHECK (badge_name IN ('Saving Master','Budget Keeper','Investor','Smart Spender')),
    rank       TEXT NOT NULL CHECK (rank IN ('starter','bronze','silver','gold','platinum','diamond')),
    xp         INTEGER  NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS budget_categories (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    name       TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);


INSERT OR IGNORE INTO currencies (id, code, name, symbol) VALUES
    (1, 'EUR', 'Euro', '€'),
    (2, 'USD', 'US Dollar', '$'),
    (3, 'GBP', 'British Pound', '£'),
    (4, 'CZK', 'Česká Koruna', 'Kč'),
    (5, 'PLN', 'Polski Złoty', 'zł');

INSERT OR IGNORE INTO regions (id, locale_code, decimal_separator, thousands_separator, date_format, time_format, currency_id)
VALUES (1, 'sk_SK', ',', ' ', '%d.%m.%Y', '%H:%M:%S', 1),
       (2, 'en_US', '.', ',', '%d/%m/%Y', '%I:%M:%S %p', 2),
       (3, 'en_GB', '.', ',', '%d/%m/%Y', '%H:%M:%S', 3),
       (5, 'pl_PL', ',', ' ', '%d.%m.%Y', '%H:%M:%S', 5),
       (6, 'de_DE', ',', '.', '%d.%m.%Y', '%H:%M:%S', 1);

INSERT OR IGNORE INTO account_types (id, name, description) VALUES
    (1, 'Main Account', 'Everyday account'),
    (2, 'Emergency Fund', 'Use in need'),
    (3, 'Private', ''),
    (4, 'Saving Account', 'Need money for');

INSERT OR IGNORE INTO transaction_type (id, name) VALUES
    (1, 'Income'),
    (2, 'Expense'),
    (3, 'Savings'),
    (4, 'Invest'),
    (5, 'Saving Expense');

INSERT OR IGNORE INTO transaction_status (id, name) VALUES
    (1, 'Completed'),
    (2, 'Pending');

INSERT OR IGNORE INTO spending_classification (id, name) VALUES
    (1, 'Needs'),
    (2, 'Wants'),
    (3, 'Savings');

INSERT OR IGNORE INTO categories (id, user_id, name) VALUES
    (1, NULL, 'Food'),
    (2, NULL, 'Clothing'),
    (3, NULL, 'Entertainment'),
    (4, NULL, 'Rent'),
    (5, NULL, 'Transportation'),
    (6, NULL, 'Saving'),
    (7, NULL, 'Saving Expense'),
    (8, NULL, 'Investment'),
    (9, NULL, 'Transfer'),
    (10, NULL, 'Salary');

INSERT OR IGNORE INTO budget_categories(id, name) VALUES (1, 'food'), (2, 'rent'),
                                                         (3, 'transport'), (4, 'utilities'), (5, 'other');

INSERT OR IGNORE INTO exchange_rates(id, base_currency_code, target_currency_code, rate) VALUES
    (1, 'EUR', 'USD', 1.16),
    (2, 'EUR', 'GBP', 0.87),
    (3, 'EUR', 'CZK', 24.42),
    (4, 'EUR', 'PLN', 4.28),
    (5, 'USD', 'EUR', 0.87),
    (6, 'USD', 'GBP', 0.75),
    (7, 'USD', 'CZK', 21.14),
    (8, 'USD', 'PLN', 3.7),
    (9, 'GBP', 'EUR', 1.15),
    (10, 'GBP', 'USD', 1.32),
    (11, 'GBP', 'CZK', 28.18),
    (12, 'GBP', 'PLN', 4.96),
    (13, 'CZK', 'EUR', 0.041),
    (14, 'CZK', 'USD', 0.047),
    (15, 'CZK', 'GBP', 0.035),
    (16, 'CZK', 'PLN', 0.18),
    (17, 'PLN', 'EUR', 0.23),
    (18, 'PLN', 'USD', 0.27),
    (19, 'PLN', 'GBP', 0.2),
    (20, 'PLN', 'CZK', 5.7);

INSERT OR IGNORE INTO milestones (badge_name, rank, xp) VALUES
                                                  ('Saving Master', 'starter', 10),
                                                  ('Budget Keeper', 'starter', 10),
                                                  ('Investor', 'starter', 10),
                                                  ('Smart Spender', 'starter', 10),
                                                  ('Saving Master', 'bronze', 20),
                                                  ('Budget Keeper', 'bronze', 20),
                                                  ('Investor', 'bronze', 20),
                                                  ('Smart Spender', 'bronze', 20),
                                                  ('Saving Master', 'silver', 30),
                                                  ('Budget Keeper', 'silver', 30),
                                                  ('Investor', 'silver', 30),
                                                  ('Smart Spender', 'silver', 30),
                                                  ('Saving Master', 'gold', 40),
                                                  ('Budget Keeper', 'gold', 40),
                                                  ('Investor', 'gold', 40),
                                                  ('Smart Spender', 'gold', 40),
                                                  ('Saving Master', 'platinum', 69),
                                                  ('Budget Keeper', 'platinum', 69),
                                                  ('Investor', 'platinum', 69),
                                                  ('Smart Spender', 'platinum', 69),
                                                  ('Saving Master', 'diamond', 420),
                                                  ('Budget Keeper', 'diamond', 420),
                                                  ('Investor', 'diamond', 420),
                                                  ('Smart Spender', 'diamond', 420);



