-- =====================================================
-- AP2 POC DATABASE SCHEMA
-- =====================================================

-- 1. FILE METADATA TABLE
CREATE TABLE IF NOT EXISTS file_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_size BIGINT,
    content_type VARCHAR(100),
    status VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. INVOICE DATA TABLE
CREATE TABLE IF NOT EXISTS invoice_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    file_name VARCHAR(255),
    merchant_name VARCHAR(255),
    invoice_number VARCHAR(100),
    total_amount DOUBLE,
    due_date VARCHAR(50),
    status VARCHAR(50),
    raw_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_merchant_name (merchant_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_total_amount (total_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. INVOICE FIELD MAP TABLE
CREATE TABLE IF NOT EXISTS invoice_field_map (
    invoice_id BIGINT NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    field_value VARCHAR(1000),
    PRIMARY KEY (invoice_id, field_name),
    CONSTRAINT fk_invoice_field_map
        FOREIGN KEY (invoice_id)
        REFERENCES invoice_data(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. PAYMENT TRANSACTIONS TABLE
-- 4. PAYMENT TRANSACTIONS TABLE (Updated)
CREATE TABLE IF NOT EXISTS payment_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    transaction_id VARCHAR(100),
    invoice_uuid VARCHAR(50),
    from_account VARCHAR(255),
    to_account VARCHAR(255),
    from_account_type VARCHAR(50),  -- MISSING FIELD
    to_account_type VARCHAR(50),    -- MISSING FIELD
    amount DOUBLE,
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    status VARCHAR(50),
    gateway_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_invoice_uuid (invoice_uuid),
    INDEX idx_status (status),
    INDEX idx_from_account_type (from_account_type),
    INDEX idx_to_account_type (to_account_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 5. USER PUBLIC KEYS TABLE
CREATE TABLE IF NOT EXISTS user_public_keys (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    public_key TEXT NOT NULL,
    key_algorithm VARCHAR(20) DEFAULT 'RSA',
    key_size INT DEFAULT 2048,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_user_id (user_id),
    INDEX idx_is_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. INTENT MANDATE TABLE
CREATE TABLE IF NOT EXISTS intent_mandate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    intent_hash VARCHAR(255) UNIQUE,
    invoice_uuid VARCHAR(50),
    merchant_name VARCHAR(255),
    status VARCHAR(50),
    total_amount DOUBLE,
    currency VARCHAR(10),
    natural_language_description TEXT,
    intent_expiry VARCHAR(50),
    requires_refundability BOOLEAN DEFAULT FALSE,
    user_authorization TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_intent_hash (intent_hash),
    INDEX idx_invoice_uuid (invoice_uuid),
    INDEX idx_merchant_name (merchant_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. CART MANDATE TABLE
CREATE TABLE IF NOT EXISTS cart_mandate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    cart_id VARCHAR(100) UNIQUE,
    intent_hash VARCHAR(255),
    invoice_uuid VARCHAR(50),
    cart_hash VARCHAR(255),
    cart_json TEXT,
    backend_signature TEXT,
    total_amount DOUBLE,
    status VARCHAR(50),
    timestamp VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_cart_id (cart_id),
    INDEX idx_intent_hash (intent_hash),
    INDEX idx_invoice_uuid (invoice_uuid),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. PAYMENT MANDATE TABLE
CREATE TABLE IF NOT EXISTS payment_mandate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    payment_mandate_id VARCHAR(100),
    cart_id VARCHAR(100),
    cart_hash VARCHAR(255),
    payment_hash VARCHAR(255),
    merchant_name VARCHAR(255),
    amount DOUBLE,
    currency VARCHAR(10),
    payment_method VARCHAR(50),
    timestamp VARCHAR(50),
    backend_signature TEXT,
    status VARCHAR(50),
    gateway_order_id VARCHAR(100),
    gateway_payment_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_payment_mandate_id (payment_mandate_id),
    INDEX idx_cart_id (cart_id),
    INDEX idx_merchant_name (merchant_name),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. AP2 AUDIT LOG TABLE
CREATE TABLE IF NOT EXISTS ap2_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mandate_type VARCHAR(20) NOT NULL,
    mandate_id VARCHAR(255),
    invoice_uuid VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    actor VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    details TEXT,
    signature_hash VARCHAR(255),
    amount DOUBLE,
    merchant_name VARCHAR(255),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_mandate_type (mandate_type),
    INDEX idx_mandate_id (mandate_id),
    INDEX idx_invoice_uuid (invoice_uuid),
    INDEX idx_action (action),
    INDEX idx_status (status),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. CHAT SESSIONS TABLE
CREATE TABLE IF NOT EXISTS chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) UNIQUE NOT NULL,
    user_id VARCHAR(100),
    label VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. CHAT MESSAGES TABLE
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    user_message TEXT,
    bot_response TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_session
        FOREIGN KEY (session_id)
        REFERENCES chat_sessions(id)
        ON DELETE CASCADE,
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


CREATE TABLE IF NOT EXISTS cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    provider_name VARCHAR(64),
    provider_customer_id VARCHAR(64),
    provider_token_id VARCHAR(128) UNIQUE,
    card_alias VARCHAR(128),
    card_holder_name VARCHAR(128),
    card_last_4 VARCHAR(4) NOT NULL,
    card_network VARCHAR(32),
    card_type VARCHAR(16),
    expiry_month INT,
    expiry_year INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_uuid (uuid),
    INDEX idx_user_id (user_id),
    INDEX idx_provider_customer_id (provider_customer_id),
    INDEX idx_provider_token_id (provider_token_id),
    INDEX idx_card_last_4 (card_last_4),
    INDEX idx_is_active (is_active),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- =====================================================
-- END OF SCHEMA CREATION
-- =====================================================