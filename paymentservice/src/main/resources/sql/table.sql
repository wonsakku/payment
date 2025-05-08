CREATE TABLE payment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id BIGINT NOT NULL,
    is_payment_done BOOLEAN NOT NULL DEFAULT FALSE,
    payment_key VARCHAR(255) UNIQUE,
    order_id VARCHAR(255) UNIQUE,
    type ENUM('NORMAL') NOT NULL,
    order_name VARCHAR(255) NOT NULL,
    method ENUM('EASY_PAY'),
    psp_raw_data JSON,
    approved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



CREATE TABLE payment_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_event_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_order_status ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN') NOT NULL DEFAULT 'NOT_STARTED',
    ledger_updated BOOLEAN DEFAULT FALSE,
    wallet_updated BOOLEAN DEFAULT FALSE,
    failed_count TINYINT DEFAULT 0,
    threshold TINYINT DEFAULT 5,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (payment_event_id) REFERENCES payment_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE payment_order_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT NOT NULL,
    previous_status ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN'),
    new_status ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN'),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(255),
    reason VARCHAR(500),

    FOREIGN KEY (payment_order_id) REFERENCES payment_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

