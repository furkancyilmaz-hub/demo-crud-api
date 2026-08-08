-- demo-api test verisi
-- 01-schema.sql'in hemen ardından, container ilk kurulurken çalışır.
-- Değişiklik sonrası: docker compose down -v && docker compose up -d
--
-- Tüm değerler deterministik aritmetikle üretilir (random() yok).
-- Aynı script her çalıştığında aynı veri kümesini verir.
--
-- Dağılım çarpık (skewed). Her ebeveyne eşit sayıda çocuk verilirse hangi kaydı
-- seçersen seç aynı N+1 profilini görürsün; child sorgu sayısı sabit kalır.
-- Bunun yerine satır sayısı, sıra numarasından türetilen bijektif bir anahtarla
-- büyüklük kademelerine dağıtılır:
--
--   rn = id - min(id) + 1                  -- sequence 1'den başlamasa da güvenli
--   t  = 1 + ((rn * <asal>) % <toplam>)    -- 1..toplam aralığında permütasyon
--
-- gcd(asal, toplam) = 1 olduğu için t her satırda benzersizdir; kademe boyutları
-- tam olarak hesaplanabilir ve ağır kayıtlar tabloya yayılır (baştaki id'lerde
-- toplanmaz). Sabitler: proposal 1103, customer 7919.

-- ---------------------------------------------------------------------------
-- proposal — 5.000 satır
-- ---------------------------------------------------------------------------
INSERT INTO proposal (proposal_no, status, issue_date, total_premium)
SELECT
    'PRP-2026-' || lpad(g::text, 6, '0'),
    (ARRAY['DRAFT','APPROVED','REJECTED'])[1 + (g % 3)],
    DATE '2025-01-01' + (g % 550),
    round((1000 + (g * 17) % 90000)::numeric, 2)
FROM generate_series(1, 5000) AS g;

-- ---------------------------------------------------------------------------
-- customer — 200.000 satır, başvuru başına 0-500 müşteri
--
-- Kademeler (toplam 5.000 başvuru → 200.000 müşteri):
--     10 başvuru × 500   =   5.000
--     40 başvuru × 300   =  12.000
--    150 başvuru × 200   =  30.000
--    400 başvuru × 130   =  52.000
--    800 başvuru ×  60   =  48.000
--  1.200 başvuru ×  31   =  37.200
--  1.500 başvuru ×  10   =  15.000
--    800 başvuru ×   1   =     800
--    100 başvuru ×   0   =       0   ← kasıtlı: müşterisiz başvuru
--
-- city dağılımı F09'un can damarı: 81 il üzerine eşit dağıtılıyor, yani tek il
-- sorgusu tablonun ~%1,2'sini dönüyor. Bu, planner'ın index'i tercih edeceği
-- aralık. İl sayısını azaltırsan index oluşturulsa bile kullanılmaz ve F09
-- "fix işe yaramadı" gibi görünür.
--
-- Şehir/isim/kimlik alanları başvuru içi sıra (n) yerine GLOBAL sıra (rn)
-- üzerinden hesaplanır; aksi halde 500 müşterili başvurular il dağılımını bozar.
-- ---------------------------------------------------------------------------
WITH sized AS (
    SELECT
        p.id,
        CASE
            WHEN t <=   10 THEN 500
            WHEN t <=   50 THEN 300
            WHEN t <=  200 THEN 200
            WHEN t <=  600 THEN 130
            WHEN t <= 1400 THEN  60
            WHEN t <= 2600 THEN  31
            WHEN t <= 4100 THEN  10
            WHEN t <= 4900 THEN   1
            ELSE                  0
        END AS cnt
    FROM proposal p
    CROSS JOIN LATERAL (
        SELECT (1 + ((p.id - (SELECT min(id) FROM proposal) + 1) * 1103) % 5000) AS t
    ) k
),
expanded AS (
    SELECT
        s.id AS proposal_id,
        row_number() OVER (ORDER BY s.id, n) AS rn
    FROM sized s
    CROSS JOIN LATERAL generate_series(1, s.cnt) AS n   -- cnt = 0 → hiç satır yok
)
INSERT INTO customer (proposal_id, identity_no, full_name, city, status)
SELECT
    e.proposal_id,
    lpad((10000000000 + e.rn)::text, 11, '0'),
    ref.first_names[(1 + (e.rn % 20))::int] || ' ' || ref.last_names[(1 + ((e.rn / 20) % 20))::int],
    ref.cities[(1 + (e.rn % 81))::int],
    CASE WHEN e.rn % 5 = 0 THEN 'PASSIVE' ELSE 'ACTIVE' END
FROM expanded e,
LATERAL (SELECT
    ARRAY['Ahmet','Mehmet','Mustafa','Ali','Hüseyin','Hasan','İbrahim','Osman',
          'Yusuf','Murat','Ömer','Ramazan','Fatma','Ayşe','Emine','Hatice',
          'Zeynep','Elif','Meryem','Şerife'] AS first_names,
    ARRAY['Yılmaz','Kaya','Demir','Çelik','Şahin','Yıldız','Yıldırım','Öztürk',
          'Aydın','Özdemir','Arslan','Doğan','Kılıç','Aslan','Çetin','Kara',
          'Koç','Kurt','Özkan','Şimşek'] AS last_names,
    ARRAY['Adana','Adıyaman','Afyonkarahisar','Ağrı','Amasya','Ankara','Antalya',
          'Artvin','Aydın','Balıkesir','Bilecik','Bingöl','Bitlis','Bolu','Burdur',
          'Bursa','Çanakkale','Çankırı','Çorum','Denizli','Diyarbakır','Edirne',
          'Elazığ','Erzincan','Erzurum','Eskişehir','Gaziantep','Giresun',
          'Gümüşhane','Hakkari','Hatay','Isparta','Mersin','İstanbul','İzmir',
          'Kars','Kastamonu','Kayseri','Kırklareli','Kırşehir','Kocaeli','Konya',
          'Kütahya','Malatya','Manisa','Kahramanmaraş','Mardin','Muğla','Muş',
          'Nevşehir','Niğde','Ordu','Rize','Sakarya','Samsun','Siirt','Sinop',
          'Sivas','Tekirdağ','Tokat','Trabzon','Tunceli','Şanlıurfa','Uşak','Van',
          'Yozgat','Zonguldak','Aksaray','Bayburt','Karaman','Kırıkkale','Batman',
          'Şırnak','Bartın','Ardahan','Iğdır','Yalova','Karabük','Kilis',
          'Osmaniye','Düzce'] AS cities
) ref;

-- ---------------------------------------------------------------------------
-- payment — 600.000 satır, müşteri başına 0-500 ödeme
--
-- Kademeler (toplam 200.000 müşteri → 600.000 ödeme):
--      10 müşteri × 500  =   5.000
--      40 müşteri × 150  =   6.000
--     350 müşteri ×  60  =  21.000
--   3.600 müşteri ×  10  =  36.000
--  28.000 müşteri ×   1  =  28.000
--  57.000 müşteri ×   2  = 114.000
--  70.000 müşteri ×   3  = 210.000
--  36.000 müşteri ×   5  = 180.000
--   5.000 müşteri ×   0  =       0   ← kasıtlı: ödemesiz müşteri
--
-- F08'in kanıtı buna dayanıyor: child sayısı sabit olursa "aynı sorgu N kez"
-- izi her kayıtta aynı çıkar, ağır/hafif kayıt ayrımı yapılamaz.
--
-- due_date 18 aylık pencereye sarılır ((n-1) % 18); sarma olmadan 500. ödemenin
-- vadesi 2066'ya taşar ve PENDING eşiği (2026-08-01) anlamsızlaşır.
-- ---------------------------------------------------------------------------
WITH sized AS (
    SELECT
        c.id,
        CASE
            WHEN t <=     10 THEN 500
            WHEN t <=     50 THEN 150
            WHEN t <=    400 THEN  60
            WHEN t <=   4000 THEN  10
            WHEN t <=  32000 THEN   1
            WHEN t <=  89000 THEN   2
            WHEN t <= 159000 THEN   3
            WHEN t <= 195000 THEN   5
            ELSE                    0
        END AS cnt
    FROM customer c
    CROSS JOIN LATERAL (
        SELECT (1 + ((c.id - (SELECT min(id) FROM customer) + 1) * 7919) % 200000) AS t
    ) k
)
INSERT INTO payment (customer_id, amount, due_date, status)
SELECT
    s.id,
    round((250 + (s.id * 7 + n * 13) % 4500)::numeric, 2),
    d.due_date,
    CASE
        WHEN d.due_date >= DATE '2026-08-01' THEN 'PENDING'
        WHEN (s.id + n) % 6 = 0              THEN 'OVERDUE'
        ELSE 'PAID'
        END
FROM sized s
         CROSS JOIN LATERAL generate_series(1, s.cnt) AS n   -- cnt = 0 → hiç satır yok
         CROSS JOIN LATERAL (
    SELECT DATE '2025-09-01' + (((n - 1) % 18) * 30) + (s.id % 15)::int AS due_date
) d;

-- ---------------------------------------------------------------------------
-- Planner istatistikleri.
-- Bu adım atlanırsa PostgreSQL yanlış plan seçer ve SP009 tutarsız davranır.
-- ---------------------------------------------------------------------------
ANALYZE proposal;
ANALYZE customer;
ANALYZE payment;

-- ---------------------------------------------------------------------------
-- Doğrulama (elle çalıştır):
--
--   SELECT count(*) FROM proposal;                         -- 5000
--   SELECT count(*) FROM customer;                         -- 200000
--   SELECT count(*) FROM payment;                          -- 600000
--
--   -- başvuru başına müşteri dağılımı
--   SELECT cnt, count(*) FROM (
--     SELECT p.id, count(c.id) cnt FROM proposal p
--     LEFT JOIN customer c ON c.proposal_id = p.id GROUP BY p.id) x
--   GROUP BY cnt ORDER BY cnt;
--     -- 0→100, 1→800, 10→1500, 31→1200, 60→800, 130→400, 200→150, 300→40, 500→10
--
--   -- müşteri başına ödeme dağılımı
--   SELECT cnt, count(*) FROM (
--     SELECT c.id, count(pm.id) cnt FROM customer c
--     LEFT JOIN payment pm ON pm.customer_id = c.id GROUP BY c.id) x
--   GROUP BY cnt ORDER BY cnt;
--     -- 0→5000, 1→28000, 2→57000, 3→70000, 5→36000, 10→3600, 60→350, 150→40, 500→10
--
--   SELECT count(*) FROM customer WHERE city = 'Ankara';   -- ~2469 (%1,2)
--   SELECT count(DISTINCT city) FROM customer;             -- 81
--
--   EXPLAIN (ANALYZE) SELECT * FROM customer WHERE city = 'Ankara';
--     -> Seq Scan bekleniyor (city indexsiz, F09 buna dayanıyor)
-- ---------------------------------------------------------------------------
