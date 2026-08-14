# demo-crud-api

Spring Boot 4 üzerinde geliştirilmiş, örnek bir sigorta/teklif (proposal) domain'i üzerinden CRUD işlemleri sunan demo API. Asıl amacı sadece CRUD göstermek değil; **N+1 sorgu problemlerini ve eksik index senaryolarını** kontrollü, tekrarlanabilir biçimde gözlemleyip analiz edebilecek bir ortam kurmaktır. Bu yüzden normal `/api/**` endpoint'lerinin yanında, log ve şema metadata'sını inceleten `/internal/**` endpoint'leri de içerir.

## Teknoloji Yığını

- **Java 21**
- **Spring Boot 4.1.0** (Spring Framework 7)
- Spring Data JPA (Hibernate)
- Spring Web MVC
- Spring Validation (Jakarta Bean Validation)
- **springdoc-openapi 3.1.0** — Swagger UI / OpenAPI 3
- PostgreSQL (JDBC driver, runtime)
- Spring Boot Docker Compose (dev ortamında Postgres'i otomatik ayağa kaldırır)
- Lombok
- Maven (Maven Wrapper ile — `mvnw` / `mvnw.cmd`)

> Not: Spring Security dahil değildir; endpoint'ler kimlik doğrulama olmadan açıktır.

## Domain Modeli

```
Proposal (1) ──< Customer (N) ──< Payment (N)
```

Ayrıca bağımsız bir `app_log` tablosu bulunur; bu tablo uygulama içi Hibernate SQL loglarını tutar ve N+1 analiz senaryolarında kullanılır.

## Paket Yapısı

Teknik katmanlara göre ayrılmıştır:

| Paket | Sorumluluk |
|---|---|
| `controller` | HTTP katmanı — validation, DTO dönüşümü, servise delegasyon |
| `service` | İş mantığı, `@Transactional` |
| `repository` | Veri erişimi (Spring Data JPA) |
| `entity` | JPA entity'leri |
| `dto` | İstek/yanıt DTO'ları (record) |
| `exception` | Domain exception'ları + merkezi `@RestControllerAdvice` |
| `config` | `OpenApiConfig`, `@ConfigurationProperties` sınıfları |
| `filter` | `CorrelationIdFilter` (`X-Correlation-Id` header yönetimi) |
| `logging` | `DbLogAppender` — Hibernate SQL loglarını `app_log` tablosuna yazan appender |
| `constants` | Paylaşılan sabitler |

## API Endpoint'leri

### Customers

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/customers` | Müşteri oluştur |
| GET | `/api/customers/{id}` | Tek müşteri getir |
| GET | `/api/customers` | Müşterileri listele (opsiyonel `proposalId` filtresi) |
| GET | `/api/customers/detail` | Müşteri + ödemeleri (her müşteri için ayrı sorgu — N+1 senaryosunu göstermek için) |
| GET | `/api/customers/overview` | Müşteri + ödemeleri (tek sorguda, toplu) |
| GET | `/api/customers/search` | Müşteri ara. İki mod: sadece `city` (bu kolonda bilinçli olarak index yok — eksik index senaryosu), ya da `proposalId` + `identityNo` birlikte (bir teklifin müşterileri içinde kimlik no ile arama). Başka kombinasyon `400` döner |
| GET | `/api/customers/{id}/payments` | Bir müşterinin ödemeleri |
| PUT | `/api/customers/{id}` | Müşteri güncelle |
| DELETE | `/api/customers/{id}` | Müşteri sil |

### Proposals

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/proposals` | Teklif oluştur |
| GET | `/api/proposals/{id}` | Tek teklif getir |
| GET | `/api/proposals/search` | `proposalNo` ile tek teklif getir (kolon unique; bulunamazsa `404`) |
| GET | `/api/proposals` | Teklifleri listele |
| GET | `/api/proposals/detail` | Teklif + müşterileri |
| PUT | `/api/proposals/{id}` | Teklif güncelle |
| DELETE | `/api/proposals/{id}` | Teklif sil |

### Payments

| Method | Path | Açıklama |
|---|---|---|
| POST | `/api/payments` | Ödeme oluştur |
| GET | `/api/payments/{id}` | Tek ödeme getir |
| GET | `/api/payments` | Ödemeleri listele (opsiyonel `customerId` filtresi) |
| PUT | `/api/payments/{id}` | Ödeme güncelle |
| DELETE | `/api/payments/{id}` | Ödeme sil |

### Internal (analiz/gözlemleme amaçlı)

| Method | Path | Açıklama |
|---|---|---|
| GET | `/internal/logs` | `correlationId` ve/veya zaman aralığına göre log sorgula |
| GET | `/internal/requests` | Tamamlanmış istek özetlerini zaman aralığında sorgula |
| GET | `/internal/schema/foreign-keys` | Veritabanından canlı foreign key metadata'sı listele |

Create endpoint'leri `201 Created` + `Location` header, delete endpoint'leri `204 No Content` döner.

## Swagger / OpenAPI

- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

`/internal/**` dahil tüm endpoint'ler istisnasız spec'te görünür (bilinçli olarak `GroupedOpenApi` filtresi kullanılmamıştır).

## Çalıştırma

Ön koşul: Docker (Spring Boot Docker Compose entegrasyonu, uygulama ayağa kalkarken `compose.yaml` üzerinden Postgres konteynerini otomatik başlatır).

```bash
./mvnw spring-boot:run
```

Uygulama varsayılan olarak `8080` portunda ayağa kalkar. Veritabanı ilk kalkışta `db/01-schema.sql` ve `db/02-seed.sql` script'leri ile otomatik oluşturulur ve doldurulur (yaklaşık 5.000 proposal, 200.000 customer, 600.000 payment — deterministik seed verisi).

### Yapılandırma (`src/main/resources/application.yaml`)

- `spring.datasource.*` — PostgreSQL bağlantı bilgileri
- `spring.jpa.hibernate.ddl-auto: validate` — şemanın tek sahibi SQL script'leridir, Hibernate sadece doğrular
- `spring.jpa.open-in-view: false`
- `spring.data.web.pageable.default-page-size: 200`
- `schema-metadata.excluded-tables` — `/internal/schema/foreign-keys` çıktısından hariç tutulan tablolar (`app_log`)

## Test

```bash
./mvnw test
```

- Servis katmanı için düz unit testler
- Controller'lar için `@WebMvcTest` tabanlı slice testler
- `OpenApiDocsTest` — `/v3/api-docs` çağırıp tüm path ve operasyonların spec'te yer aldığını doğrular

## Notlar

- `customer.city` kolonu bilinçli olarak index'siz bırakılmıştır; `/api/customers/search?city=` bu eksik index senaryosunu göstermek için kullanılır.
- Buna karşılık `/api/customers/search?proposalId=&identityNo=` sorgusu `idx_customer_proposal_identity` composite index'i üzerinden çalışır — aynı endpoint üzerinde indexli ve indexsiz aramayı karşılaştırmayı mümkün kılar.
- `/api/customers/detail` ile `/api/customers/overview` aynı veriyi farklı sorgulama stratejileriyle (N+1 vs. toplu) döndürür; N+1 analizini karşılaştırmalı göstermek amaçlıdır.
