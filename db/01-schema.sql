-- demo-api şeması
-- Bu dosya şemanın tek sahibidir. Hibernate ddl-auto: validate ile yalnızca doğrular.
-- Entity'ye alan eklenirse bu dosya da güncellenmelidir.
--
-- Container ilk kez ayağa kalkarken /docker-entrypoint-initdb.d/ üzerinden
-- 02-seed.sql'den önce çalışır. Değişiklik sonrası: docker compose down -v

CREATE TABLE proposal (
    id            BIGSERIAL PRIMARY KEY,
    proposal_no   VARCHAR(30)   NOT NULL UNIQUE,
    status        VARCHAR(20)   NOT NULL,   -- DRAFT | APPROVED | REJECTED
    issue_date    DATE          NOT NULL,
    total_premium NUMERIC(15,2)
);

CREATE TABLE customer (
    id          BIGSERIAL    PRIMARY KEY,
    proposal_id BIGINT       NOT NULL REFERENCES proposal(id),
    identity_no VARCHAR(20)  NOT NULL,
    full_name   VARCHAR(120) NOT NULL,
    city        VARCHAR(40)  NOT NULL,
    status      VARCHAR(20)  NOT NULL       -- ACTIVE | PASSIVE
);

CREATE TABLE payment (
    id          BIGSERIAL     PRIMARY KEY,
    customer_id BIGINT        NOT NULL REFERENCES customer(id),
    amount      NUMERIC(15,2) NOT NULL,
    due_date    DATE          NOT NULL,
    status      VARCHAR(20)   NOT NULL      -- PENDING | PAID | OVERDUE
);

-- Uygulama loglarının yazıldığı tablo (F05).
CREATE TABLE app_log (
    id             BIGSERIAL   PRIMARY KEY,
    correlation_id VARCHAR(64),
    timestamp      TIMESTAMPTZ NOT NULL,
    level          VARCHAR(10) NOT NULL,
    logger         VARCHAR(200),
    message        TEXT
);

-- PostgreSQL foreign key'lere otomatik index açmaz.
-- Bunlar olmazsa N+1 senaryosundaki child sorguları da yavaşlar ve
-- iki ayrı performans problemi birbirine karışır.
CREATE INDEX idx_customer_proposal ON customer(proposal_id);
CREATE INDEX idx_payment_customer  ON payment(customer_id);

CREATE INDEX idx_app_log_correlation ON app_log(correlation_id, timestamp);

-- customer.city üzerinde bilerek index YOK.
-- Index önerisi SP0010 ile proje kapsamından çıkarıldı; bu kolon indexsiz
-- kaldığı için /api/customers/search sorgusu Seq Scan yapar. Analiz kapsamı
-- N+1 senaryolarıdır, index tuning değil.
