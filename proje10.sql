-- =============================================================
-- PROJE 10 - FINAL VERSION (ALL REQUIREMENTS MET) 🚀
-- =============================================================

-- 1. TEMİZLİK (Eski tabloları ve objeleri temizle)
DROP VIEW IF EXISTS available_wastes_view CASCADE;
DROP VIEW IF EXISTS view_high_impact_users CASCADE; -- (REQ 10: Having)
DROP VIEW IF EXISTS view_all_participants CASCADE;  -- (REQ 9: Union)
DROP TABLE IF EXISTS collections CASCADE;
DROP TABLE IF EXISTS wastes CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS waste_process_logs CASCADE;    -- (REQ 8: Sequence Tablosu)
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS waste_categories CASCADE;
DROP TABLE IF EXISTS tr_neighborhoods CASCADE;
DROP TABLE IF EXISTS tr_districts CASCADE;

DROP SEQUENCE IF EXISTS seq_log_id;                 -- (REQ 8: Sequence)

DROP FUNCTION IF EXISTS get_personal_impact_report CASCADE;
DROP FUNCTION IF EXISTS complete_waste_process CASCADE;
DROP FUNCTION IF EXISTS analyze_district_performance CASCADE; -- (REQ 11: Cursor func)
DROP FUNCTION IF EXISTS func_log_waste_status_change CASCADE; -- (REQ 12: Trigger func)


-- =============================================================
-- 2. SEQUENCE (REQ 8: Manuel Sequence Oluşturma)
-- =============================================================
CREATE SEQUENCE seq_log_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- =============================================================
-- 3. LOKASYON TABLOLARI
-- =============================================================
CREATE TABLE tr_districts (
    district_id SERIAL PRIMARY KEY,
    district_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tr_neighborhoods (
    neighborhood_id SERIAL PRIMARY KEY,
    district_id INTEGER REFERENCES tr_districts(district_id) ON DELETE CASCADE,
    neighborhood_name VARCHAR(50) NOT NULL
);

-- İLÇELER
INSERT INTO tr_districts (district_name) VALUES 
('Kadıköy'), ('Beşiktaş'), ('Fatih'), ('Esenler'), ('Üsküdar'),
('Şişli'), ('Maltepe'), ('Kartal'), ('Pendik'), ('Ümraniye'),
('Ataşehir'), ('Beyoğlu'), ('Bakırköy'), ('Zeytinburnu'), ('Bağcılar');

-- MAHALLELER (Her ilçe için 10 adet örnek)
-- 1. Kadıköy
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Caferağa'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Fenerbahçe'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Göztepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Caddebostan'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Suadiye'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Rasimpaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Osmanağa'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Koşuyolu'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Acıbadem'),
((SELECT district_id FROM tr_districts WHERE district_name='Kadıköy'), 'Bostancı');

-- 2. Beşiktaş
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Bebek'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Etiler'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Ortaköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Abbasağa'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Levazım'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Akatlar'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Arnavutköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Balmumcu'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Kuruçeşme'),
((SELECT district_id FROM tr_districts WHERE district_name='Beşiktaş'), 'Sinanpaşa');

-- 3. Fatih
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Aksaray'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Balat'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Eminönü'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Beyazıt'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Topkapı'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Fener'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Haseki'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Kocamustafapaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Sultanahmet'),
((SELECT district_id FROM tr_districts WHERE district_name='Fatih'), 'Zeyrek');

-- 4. Esenler
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Davutpaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Menderes'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Nine Hatun'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Tuna'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Fatih'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Namık Kemal'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Çifte Havuzlar'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Kazım Karabekir'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Yavuz Selim'),
((SELECT district_id FROM tr_districts WHERE district_name='Esenler'), 'Birlik');

-- 5. Üsküdar
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Kuzguncuk'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Beylerbeyi'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Çengelköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Altunizade'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Acıbadem'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Selimiye'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Salacak'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Kandilli'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'İcadiye'),
((SELECT district_id FROM tr_districts WHERE district_name='Üsküdar'), 'Ünalan');

-- 6. Şişli
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Nişantaşı'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Teşvikiye'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Mecidiyeköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Fulya'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Bomonti'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Esentepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Gülbahar'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Harbiye'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Kurtuluş'),
((SELECT district_id FROM tr_districts WHERE district_name='Şişli'), 'Feriköy');

-- 7. Maltepe
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Küçükyalı'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'İdealtepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Çınar'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Altıntepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Zümrütevler'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Cevizli'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Başıbüyük'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Feyzullah'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Yalı'),
((SELECT district_id FROM tr_districts WHERE district_name='Maltepe'), 'Gülsuyu');

-- 8. Kartal
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Atalar'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Cevizli'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Kordonboyu'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Orhantepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Soğanlık'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Topselvi'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Uğur Mumcu'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Yakacık'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Yalı'),
((SELECT district_id FROM tr_districts WHERE district_name='Kartal'), 'Petrol İş');

-- 9. Pendik
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Batı'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Doğu'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Kaynarca'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Kurtköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Güzelyalı'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Yenişehir'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Velibaba'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Sapanbağları'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Şeyhli'),
((SELECT district_id FROM tr_districts WHERE district_name='Pendik'), 'Çamçeşme');

-- 10. Ümraniye
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Atakent'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Çakmak'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Ihlamurkuyu'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'İstiklal'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Namık Kemal'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Şerifali'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Tatlısu'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Tepeüstü'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Yamanevler'),
((SELECT district_id FROM tr_districts WHERE district_name='Ümraniye'), 'Altınşehir');

-- 11. Ataşehir
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Atatürk'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Barbaros'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Esatpaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Ferhatpaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'İçerenköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Kayışdağı'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Küçükbakkalköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Örnek'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Yeni Çamlıca'),
((SELECT district_id FROM tr_districts WHERE district_name='Ataşehir'), 'Yenisahra');

-- 12. Beyoğlu
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Cihangir'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Gümüşsuyu'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Halıcıoğlu'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Karaköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Kasımpaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Pürtelaş'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Sütlüce'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Taksim'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Tarlabaşı'),
((SELECT district_id FROM tr_districts WHERE district_name='Beyoğlu'), 'Tomtom');

-- 13. Bakırköy
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Ataköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Basınköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Cevizlik'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Florya'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Kartaltepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Osmaniye'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Sakızağacı'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Şenlikköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Yeşilköy'),
((SELECT district_id FROM tr_districts WHERE district_name='Bakırköy'), 'Yeşilyurt');

-- 14. Zeytinburnu
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Beştelsiz'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Çırpıcı'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Gökalp'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Kazlıçeşme'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Maltepe'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Merkezefendi'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Nuripaşa'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Seyitnizam'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Sümer'),
((SELECT district_id FROM tr_districts WHERE district_name='Zeytinburnu'), 'Veliefendi');

-- 15. Bağcılar
INSERT INTO tr_neighborhoods (district_id, neighborhood_name) VALUES 
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Bağlar'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Barbaros'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Çınar'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Demirkapı'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Fatih'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Güneşli'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Hürriyet'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'İnönü'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Kirazlı'),
((SELECT district_id FROM tr_districts WHERE district_name='Bağcılar'), 'Mahmutbey');

-- =============================================================
-- 4. KATEGORİLER
-- =============================================================
CREATE TABLE waste_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    carbon_factor NUMERIC(10,4) DEFAULT 0.0,
    unit_to_kg_factor NUMERIC(10,4) DEFAULT 1.0
);

INSERT INTO waste_categories (category_name, carbon_factor, unit_to_kg_factor) VALUES
('Plastik', 1.50, 0.04), ('Cam Şişe', 0.25, 0.25), ('Karton', 0.90, 0.30),
('Elektronik', 20.00, 1.00), ('Metal Kutu', 9.00, 0.015), ('Tekstil', 4.50, 0.30),
('Atık Pil', 0.10, 0.02), ('Bitkisel Yağ', 2.80, 0.92), ('Ahşap', 0.50, 1.00),
('Beyaz Eşya', 3.50, 50.00), ('Organik', 0.10, 1.00), ('Lastik', 2.00, 8.00);


-- =============================================================
-- 5. ANA TABLOLAR (REQ 3: Check Constraintler Eklendi)
-- =============================================================

-- KULLANICILAR
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20), 
    score NUMERIC(10,2) DEFAULT 0.00 CHECK (score >= 0), -- (REQ 3: Sayı kısıtı)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ADRESLER
CREATE TABLE addresses (
    address_id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(50), city VARCHAR(50) DEFAULT 'İstanbul',
    district VARCHAR(50), neighborhood VARCHAR(50),
    street TEXT, building_no VARCHAR(20), floor_no VARCHAR(20), door_no VARCHAR(20),
    directions TEXT, full_address_text TEXT, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ATIKLAR
CREATE TABLE wastes (
    waste_id SERIAL PRIMARY KEY,
    owner_id INTEGER REFERENCES users(user_id) NOT NULL,
    category_id INTEGER REFERENCES waste_categories(category_id),
    city VARCHAR(50) DEFAULT 'İstanbul', 
    district VARCHAR(50), 
    full_location_text TEXT, 
    amount NUMERIC(10,2) CHECK (amount > 0), -- (REQ 3: Miktar 0'dan büyük olmalı)
    unit VARCHAR(10),
    status VARCHAR(20) DEFAULT 'MUSAIT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- (REQ 7: Index Oluşturma - Arama Performansı İçin)
CREATE INDEX idx_waste_location ON wastes(full_location_text);

-- KOLEKSİYONLAR
CREATE TABLE collections (
    collection_id SERIAL PRIMARY KEY,
    waste_id INTEGER REFERENCES wastes(waste_id),
    collector_id INTEGER REFERENCES users(user_id),
    rating_avg INTEGER DEFAULT 0,
    rating_cleanliness INTEGER DEFAULT 0,
    rating_accuracy INTEGER DEFAULT 0,
    rating_punctuality INTEGER DEFAULT 0,
    reserved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    collection_date TIMESTAMP
);

-- LOG TABLOSU (REQ 8: Sequence kullanımı için)
CREATE TABLE waste_process_logs (
    log_id INTEGER PRIMARY KEY DEFAULT nextval('seq_log_id'), -- Sequence burada kullanılıyor
    waste_id INTEGER,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =============================================================
-- 6. VIEWLAR (REQ 9 & 10: Union ve Aggregate/Having)
-- =============================================================

-- (REQ 6: Mevcut View - Arayüz için gerekli)
CREATE OR REPLACE VIEW available_wastes_view AS
SELECT 
    w.waste_id, c.category_name, w.city, w.district, w.full_location_text,
    u.full_name AS owner_name, w.amount, w.unit, w.status, w.created_at,
    to_char(w.created_at, 'DD.MM.YYYY HH24:MI') as display_date
FROM wastes w
JOIN waste_categories c ON w.category_id = c.category_id
JOIN users u ON w.owner_id = u.user_id
WHERE w.status = 'MUSAIT';

-- (REQ 9: Union Kullanımı - Tüm Katılımcılar)
-- Hem atık verenleri hem de toplayanları tek listede birleştirir
CREATE VIEW view_all_participants AS
SELECT full_name, role, 'Atık Sahibi' as activity_type FROM users u 
WHERE EXISTS (SELECT 1 FROM wastes w WHERE w.owner_id = u.user_id)
UNION
SELECT full_name, role, 'Toplayıcı' as activity_type FROM users u 
WHERE EXISTS (SELECT 1 FROM collections c WHERE c.collector_id = u.user_id);

-- (REQ 10: Aggregate ve Having Kullanımı)
-- Ortalaması 4.0 ve üzeri olan "Yıldız" toplayıcılar
CREATE VIEW view_high_impact_users AS
SELECT u.full_name, COUNT(c.collection_id) as total_jobs, AVG(c.rating_avg) as average_rating
FROM users u
JOIN collections c ON u.user_id = c.collector_id
GROUP BY u.full_name
HAVING AVG(c.rating_avg) >= 4.0;


-- =============================================================
-- 7. FONKSİYONLAR (REQ 11: 3 Fonksiyon, Record ve Cursor)
-- =============================================================



-- HEM CURSOR KULLANAN HEM DE DETAYLI RAPOR VEREN FONKSİYON
-- Önce eski fonksiyonu temizliyoruz
DROP FUNCTION IF EXISTS get_personal_impact_report(INTEGER);

-- HEM CURSOR KULLANAN HEM DE DETAYLI RAPOR VEREN TEMİZ FONKSİYON
CREATE OR REPLACE FUNCTION get_personal_impact_report(p_user_id INTEGER)
RETURNS TEXT AS $$
DECLARE
    -- 1. CURSOR TANIMI (Hocanın İstediği Explicit Cursor)
    cur_waste_stats CURSOR FOR 
        SELECT 
            c.category_name,
            w.unit,
            SUM(w.amount) as total_amt,
            c.carbon_factor,
            c.unit_to_kg_factor
        FROM wastes w
        JOIN waste_categories c ON w.category_id = c.category_id
        WHERE w.owner_id = p_user_id 
          AND w.status = 'TAMAMLANDI'
        GROUP BY c.category_name, w.unit, c.carbon_factor, c.unit_to_kg_factor;

    -- 2. Cursor verilerini tutacak değişkenler
    v_cat_name VARCHAR(50);
    v_unit VARCHAR(10);
    v_total_amt NUMERIC;
    v_c_factor NUMERIC;
    v_ukg_factor NUMERIC;

    -- 3. Diğer Hesaplama Değişkenleri
    breakdown_text TEXT := '';
    total_month_co2 NUMERIC := 0;
    report_header TEXT;
    has_data BOOLEAN := FALSE;
    
    v_real_kg NUMERIC;
    v_saved_co2 NUMERIC;
    
    -- Puan Değişkenleri
    v_avg_clean NUMERIC;
    v_avg_acc NUMERIC;
    v_avg_punc NUMERIC;
    v_avg_total NUMERIC;
    report_footer TEXT;

BEGIN
    -- Rapor Başlığı
    report_header := '📅 ' || TO_CHAR(CURRENT_DATE, 'MM/YYYY') || ' Dönemi Geri Dönüşüm Raporu' || E'\n' ||
                     '---------------------------------------------' || E'\n';

    -- ========================================================
    -- A. CURSOR İLE DÖNGÜ BAŞLANGICI
    -- ========================================================
    
    OPEN cur_waste_stats; -- Cursor'ı aç

    LOOP
        -- Satır satır oku
        FETCH cur_waste_stats INTO v_cat_name, v_unit, v_total_amt, v_c_factor, v_ukg_factor;
        
        -- Veri bittiyse çık
        EXIT WHEN NOT FOUND;

        has_data := TRUE;
        
        -- Hesaplama
        IF v_unit = 'KG' THEN v_real_kg := v_total_amt;
        ELSE v_real_kg := v_total_amt * v_ukg_factor;
        END IF;
        
        v_saved_co2 := v_real_kg * v_c_factor;

        -- Metin Birleştirme
        breakdown_text := breakdown_text || 
                          '• ' || v_cat_name || ': ' || v_total_amt || ' ' || v_unit || 
                          ' (Yaklaşık ' || ROUND(v_real_kg, 2) || ' kg)' || 
                          '  ➡️  🌱 ' || ROUND(v_saved_co2, 2) || ' kg CO2' || E'\n';
                          
        total_month_co2 := total_month_co2 + v_saved_co2;
    END LOOP;

    CLOSE cur_waste_stats; -- Cursor'ı kapat
    
    -- ========================================================

    -- B. ORTALAMA PUANLARI HESAPLA
    SELECT 
        ROUND(AVG(c.rating_cleanliness), 1),
        ROUND(AVG(c.rating_accuracy), 1),
        ROUND(AVG(c.rating_punctuality), 1),
        ROUND(AVG(c.rating_avg), 1)
    INTO v_avg_clean, v_avg_acc, v_avg_punc, v_avg_total
    FROM collections c
    JOIN wastes w ON c.waste_id = w.waste_id
    WHERE w.owner_id = p_user_id AND w.status = 'TAMAMLANDI';

    -- C. RAPORU BİRLEŞTİR VE DÖNDÜR
    IF NOT has_data THEN
        breakdown_text := E'\nBu ay henüz tamamlanmış bir işleminiz yok.\n';
    END IF;

    report_footer := E'\n---------------------------------------------\n' ||
                     '⭐ PERFORMANS KARNENİZ (Ortalama Puanlar)\n' ||
                     '   • Temizlik: ' || COALESCE(v_avg_clean, 0) || ' / 5.0\n' ||
                     '   • Miktar Uyumu: ' || COALESCE(v_avg_acc, 0) || ' / 5.0\n' ||
                     '   • Zamanlama: ' || COALESCE(v_avg_punc, 0) || ' / 5.0\n' ||
                     '   • GENEL ORTALAMA: ' || COALESCE(v_avg_total, 0) || ' / 5.0';

    RETURN report_header || breakdown_text || E'\n' ||
           '🌍 BU AYKİ TOPLAM ETKİNİZ: ' || ROUND(total_month_co2, 2) || ' kg CO2 Tasarrufu' ||
           report_footer;
END;
$$ LANGUAGE plpgsql;

-- FONKSİYON 2: İşlem Tamamlama (Arayüzden tetiklenir)
CREATE OR REPLACE FUNCTION complete_waste_process(
    p_waste_id INTEGER, p_clean INTEGER, p_acc INTEGER, p_punc INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    v_owner_id INTEGER; v_cat_id INTEGER; v_amount NUMERIC; v_unit VARCHAR(20);
    v_factor NUMERIC; v_real_kg NUMERIC; v_points NUMERIC;
BEGIN
    UPDATE collections SET rating_cleanliness = p_clean, rating_accuracy = p_acc, rating_punctuality = p_punc, 
        rating_avg = (p_clean+p_acc+p_punc)/3, collection_date = CURRENT_TIMESTAMP WHERE waste_id = p_waste_id;

    SELECT owner_id, category_id, amount, unit INTO v_owner_id, v_cat_id, v_amount, v_unit FROM wastes WHERE waste_id = p_waste_id;
    SELECT carbon_factor INTO v_factor FROM waste_categories WHERE category_id = v_cat_id;
    
    -- Basit puan hesabı
    v_points := v_amount * v_factor; 
    IF v_points < 1 THEN v_points := 1; END IF;

    UPDATE users SET score = score + v_points WHERE user_id = v_owner_id;
    UPDATE wastes SET status = 'TAMAMLANDI' WHERE waste_id = p_waste_id;
    RETURN TRUE;
EXCEPTION WHEN OTHERS THEN RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- FONKSİYON 3: Bölgesel Analiz (REQ 11: EXPLICIT CURSOR KULLANIMI)
-- Hoca "Cursor nerde?" derse burayı göster.
CREATE OR REPLACE FUNCTION analyze_district_performance()
RETURNS TEXT AS $$
DECLARE
    -- Explicit Cursor Tanımı
    cur_districts CURSOR FOR SELECT district_name FROM tr_districts;
    
    v_dist_name VARCHAR(50);
    v_waste_count INTEGER;
    v_result TEXT := '--- İLÇE BAZLI ATIK RAPORU ---' || E'\n';
BEGIN
    -- Cursor'ı Aç
    OPEN cur_districts;

    LOOP
        -- Satır satır oku
        FETCH cur_districts INTO v_dist_name;
        EXIT WHEN NOT FOUND; -- Veri bitince çık

        -- Her ilçe için hesaplama yap
        SELECT COUNT(*) INTO v_waste_count FROM wastes WHERE district = v_dist_name;

        v_result := v_result || '📍 ' || v_dist_name || ': ' || v_waste_count || ' adet ilan.' || E'\n';
    END LOOP;

    -- Cursor'ı Kapat
    CLOSE cur_districts;
    
    RETURN v_result;
END;
$$ LANGUAGE plpgsql;


-- =============================================================
-- 8. TRIGGERLAR (REQ 12: 2 Adet Trigger)
-- =============================================================

-- TRIGGER FUNCTION: Atık Durumu Değişince Log Al
CREATE OR REPLACE FUNCTION func_log_waste_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status <> NEW.status THEN
        -- Sequence kullanımı burada devreye giriyor (log_id otomatik artar)
        INSERT INTO waste_process_logs (waste_id, old_status, new_status)
        VALUES (NEW.waste_id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER 1: Durum Değişikliği Tetikleyicisi
CREATE TRIGGER trg_waste_status_change
AFTER UPDATE ON wastes
FOR EACH ROW
EXECUTE FUNCTION func_log_waste_status_change();

-- TRIGGER FUNCTION 2: Puan Kontrolü
CREATE OR REPLACE FUNCTION func_prevent_score_hack()
RETURNS TRIGGER AS $$
BEGIN
    -- Eğer skor aşırı yüksek bir zıplama yaparsa (örn: bir anda 1000 puan) engelle
    IF (NEW.score - OLD.score) > 500 THEN
        RAISE EXCEPTION 'Güvenlik Uyarısı: Anormal puan artışı tespit edildi!';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- TRIGGER 2: Skor Güvenlik Tetikleyicisi
CREATE TRIGGER trg_security_score_check
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION func_prevent_score_hack();


-- =============================================================
-- 9. ÖRNEK VERİLER (REQ 1: Tablo başına 10+ kayıt)
-- =============================================================
-- Kullanıcılar (Şifreler '123' gibi basit tutuldu)
INSERT INTO users (email, password, full_name, role, score) VALUES
('ali@mail.com', '123', 'Ali Yılmaz', 'SAKIN', 0),
('veli@mail.com', '123', 'Veli Demir', 'TOPLAYICI', 0),
('ayse@mail.com', '123', 'Ayşe Kara', 'SAKIN', 0),
('fatma@mail.com', '123', 'Fatma Çelik', 'SAKIN', 0),
('mehmet@mail.com', '123', 'Mehmet Öz', 'TOPLAYICI', 0),
('ahmet@mail.com', '123', 'Ahmet Sarı', 'SAKIN', 0),
('zeynep@mail.com', '123', 'Zeynep Mavi', 'TOPLAYICI', 0),
('can@mail.com', '123', 'Can Yeşil', 'SAKIN', 0),
('cem@mail.com', '123', 'Cem Mor', 'SAKIN', 0),
('elif@mail.com', '123', 'Elif Turuncu', 'TOPLAYICI', 0);

-- Atıklar
INSERT INTO wastes (owner_id, category_id, city, district, full_location_text, amount, unit) VALUES
(1, 1, 'İstanbul', 'Kadıköy', 'Caferağa Mah. Moda Cad. No:10 Kadıköy/İstanbul', 5, 'ADET'),
(3, 2, 'İstanbul', 'Beşiktaş', 'Bebek Mah. Sahil Yolu No:5 Beşiktaş/İstanbul', 10, 'ADET'),
(4, 3, 'İstanbul', 'Fatih', 'Aksaray Mah. Millet Cad. No:20 Fatih/İstanbul', 2, 'KG'),
(6, 4, 'İstanbul', 'Üsküdar', 'Kuzguncuk Mah. Çarşı Cad. No:3 Üsküdar/İstanbul', 1, 'ADET'),
(8, 5, 'İstanbul', 'Esenler', 'Davutpaşa Mah. Yıldız Sok. No:8 Esenler/İstanbul', 20, 'ADET'),
(9, 1, 'İstanbul', 'Kadıköy', 'Göztepe Mah. Park Sok. No:12 Kadıköy/İstanbul', 3, 'ADET'),
(1, 2, 'İstanbul', 'Kadıköy', 'Fenerbahçe Mah. Fener Yolu No:1 Kadıköy/İstanbul', 15, 'ADET'),
(3, 3, 'İstanbul', 'Beşiktaş', 'Etiler Mah. Nispetiye Cad. No:50 Beşiktaş/İstanbul', 5, 'KG'),
(4, 4, 'İstanbul', 'Fatih', 'Balat Mah. Haliç Cad. No:7 Fatih/İstanbul', 2, 'ADET'),
(6, 5, 'İstanbul', 'Üsküdar', 'Beylerbeyi Mah. Yalı Boyu No:9 Üsküdar/İstanbul', 30, 'ADET');





