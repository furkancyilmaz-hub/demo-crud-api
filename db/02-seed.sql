-- demo-api test verisi
-- 01-schema.sql'in hemen ardından, container ilk kurulurken çalışır.
-- Değişiklik sonrası: docker compose down -v && docker compose up -d
--
-- Tüm değerler deterministik aritmetikle üretilir (random() yok).
-- Aynı script her çalıştığında aynı veri kümesini verir.

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
-- customer — 200.000 satır
--
-- city dağılımı F09'un can damarı: 81 il üzerine eşit dağıtılıyor, yani tek il
-- sorgusu tablonun ~%1,2'sini dönüyor. Bu, planner'ın index'i tercih edeceği
-- aralık. İl sayısını azaltırsan index oluşturulsa bile kullanılmaz ve F09
-- "fix işe yaramadı" gibi görünür.
-- ---------------------------------------------------------------------------
INSERT INTO customer (proposal_id, identity_no, full_name, city, status)
SELECT
    (SELECT min(id) FROM proposal) + (g % 5000),
    lpad((10000000000 + g)::text, 11, '0'),
    first_names[1 + (g % 20)] || ' ' || last_names[1 + ((g / 20) % 20)],
    cities[1 + (g % 81)],
    CASE WHEN g % 5 = 0 THEN 'PASSIVE' ELSE 'ACTIVE' END
FROM generate_series(1, 200000) AS g,
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
-- payment — ~600.000 satır
--
-- Her müşteriye 2-4 ödeme; ödemesiz müşteri yok. F08'in kanıtı buna dayanıyor:
-- müşteri başına birden fazla child olmadan "aynı sorgu N kez" izi zayıf kalır.
-- ---------------------------------------------------------------------------
INSERT INTO payment (customer_id, amount, due_date, status)
SELECT
    c.id,
    round((250 + (c.id * 7 + n * 13) % 4500)::numeric, 2),
    DATE '2025-09-01' + (n * 30) + (c.id % 15)::int,
    CASE
        WHEN DATE '2025-09-01' + (n * 30) + (c.id % 15)::int >= DATE '2026-08-01'
            THEN 'PENDING'
        WHEN (c.id + n) % 6 = 0 THEN 'OVERDUE'
        ELSE 'PAID'
        END
FROM customer c
         CROSS JOIN LATERAL generate_series(1, (2 + (c.id % 3))::int) AS n;

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
--   SELECT count(*) FROM payment;                          -- ~600000
--
--   SELECT count(*) FROM customer WHERE city = 'Ankara';   -- ~2469 (%1,2)
--
--   SELECT min(c), max(c), avg(c) FROM (
--     SELECT count(*) c FROM payment GROUP BY customer_id) x;   -- min 2, max 4
--
--   EXPLAIN (ANALYZE) SELECT * FROM customer WHERE city = 'Ankara';
--     -> Seq Scan bekleniyor (city indexsiz, F09 buna dayanıyor)
-- ---------------------------------------------------------------------------
