-- Temizlik 
DROP VIEW IF EXISTS available_wastes_view CASCADE;
DROP VIEW IF EXISTS view_reliable_residents CASCADE;
DROP VIEW IF EXISTS view_unused_categories CASCADE;
DROP VIEW IF EXISTS view_all_participants CASCADE;

DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS collections CASCADE;
DROP TABLE IF EXISTS wastes CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS waste_process_logs CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS waste_categories CASCADE;
DROP TABLE IF EXISTS tr_neighborhoods CASCADE;
DROP TABLE IF EXISTS tr_districts CASCADE;

DROP SEQUENCE IF EXISTS seq_log_id;
DROP SEQUENCE IF EXISTS seq_notification_id;

DROP FUNCTION IF EXISTS get_personal_impact_report CASCADE;
DROP FUNCTION IF EXISTS complete_waste_process CASCADE;
DROP FUNCTION IF EXISTS analyze_district_performance CASCADE;
DROP FUNCTION IF EXISTS func_notify_waste_status CASCADE;
DROP FUNCTION IF EXISTS func_prevent_score_hack CASCADE;

-- Lokasyon Tabloları 
CREATE TABLE tr_districts (
    district_id SERIAL PRIMARY KEY,
    district_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE tr_neighborhoods (
    neighborhood_id SERIAL PRIMARY KEY,
    district_id INTEGER REFERENCES tr_districts(district_id) ON DELETE CASCADE,
    neighborhood_name VARCHAR(50) NOT NULL
);

-- Kategoriler
CREATE TABLE waste_categories (
    category_id SERIAL PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE,
    carbon_factor NUMERIC(10,4) DEFAULT 0.0,
    unit_to_kg_factor NUMERIC(10,4) DEFAULT 1.0
);


-- Kullanıcılar(REQ 3: Check Constraint)
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(50) NOT NULL,
    full_name VARCHAR(100),
    role VARCHAR(20), 
    score NUMERIC(10,2) DEFAULT 0.00 CHECK (score >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Adresler
CREATE TABLE addresses (
    address_id SERIAL PRIMARY KEY,
    user_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    title VARCHAR(50), city VARCHAR(50) DEFAULT 'İstanbul',
    district VARCHAR(50), neighborhood VARCHAR(50),
    street TEXT, building_no VARCHAR(20), floor_no VARCHAR(20), door_no VARCHAR(20),
    directions TEXT, full_address_text TEXT, 
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Atıklar
CREATE TABLE wastes (
    waste_id SERIAL PRIMARY KEY,
    owner_id INTEGER REFERENCES users(user_id) NOT NULL,
    category_id INTEGER REFERENCES waste_categories(category_id),
    city VARCHAR(50) DEFAULT 'İstanbul', 
    district VARCHAR(50), 
    full_location_text TEXT, 
    amount NUMERIC(10,2) CHECK (amount > 0),
    unit VARCHAR(10),
    status VARCHAR(20) DEFAULT 'MUSAIT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Waste tablosu için index oluşturma
CREATE INDEX idx_waste_location ON wastes(full_location_text);



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

-- Bildirimlerin Primary Key'ini sequence ile oluşturuyoruz.
CREATE SEQUENCE seq_notification_id
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- Bildirimler
CREATE TABLE notifications (
    notification_id INTEGER PRIMARY KEY DEFAULT nextval('seq_notification_id'), 
    user_id INTEGER REFERENCES users(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Collector Page'teki tablonun oluşmasını sağlayan view
CREATE OR REPLACE VIEW available_wastes_view AS
SELECT 
    w.waste_id, c.category_name, w.city, w.district, w.full_location_text,
    u.full_name AS owner_name, w.amount, w.unit, w.status, w.created_at,
    to_char(w.created_at, 'DD.MM.YYYY HH24:MI') as display_date
FROM wastes w
JOIN waste_categories c ON w.category_id = c.category_id
JOIN users u ON w.owner_id = u.user_id
WHERE w.status = 'MUSAIT';

--EXCEPT ile hiç kullanılmayan kategorileri bulan view 
CREATE OR REPLACE VIEW view_unused_categories AS
SELECT category_name FROM waste_categories
EXCEPT
SELECT c.category_name 
FROM wastes w 
JOIN waste_categories c ON w.category_id = c.category_id;

-- HAVING ile güvenilir kullanıcıları veren view
CREATE OR REPLACE VIEW view_reliable_residents AS
SELECT
    u.user_id,
    u.full_name,
    COUNT(w.waste_id) as total_completed
FROM users u
JOIN wastes w ON u.user_id = w.owner_id
WHERE w.status = 'TAMAMLANDI'
GROUP BY u.user_id, u.full_name
HAVING COUNT(w.waste_id) >= 5;


-- Rapor Fonsiyonu
CREATE OR REPLACE FUNCTION get_personal_impact_report(p_user_id INTEGER)
RETURNS TEXT AS $$
DECLARE
    -- Cursor tanımı 
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

    -- Cursor verilerini tutacak değişkenler
    v_cat_name VARCHAR(50);
    v_unit VARCHAR(10);
    v_total_amt NUMERIC;
    v_c_factor NUMERIC;
    v_ukg_factor NUMERIC;

    -- Diğer Hesaplama Değişkenleri
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
    -- Cursor başlangıcı
    OPEN cur_waste_stats; 

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
    CLOSE cur_waste_stats; 
    
    -- Ortalama puanları hesaplama
    SELECT 
        ROUND(AVG(c.rating_cleanliness), 1),
        ROUND(AVG(c.rating_accuracy), 1),
        ROUND(AVG(c.rating_punctuality), 1),
        ROUND(AVG(c.rating_avg), 1)
    INTO v_avg_clean, v_avg_acc, v_avg_punc, v_avg_total
    FROM collections c
    JOIN wastes w ON c.waste_id = w.waste_id
    WHERE w.owner_id = p_user_id AND w.status = 'TAMAMLANDI';

    -- Raporu birleştir ve döndür
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


-- İşlem Tamamlama Fonksiyonu
CREATE OR REPLACE FUNCTION complete_waste_process(
    p_waste_id INTEGER, p_clean INTEGER, p_acc INTEGER, p_punc INTEGER
) RETURNS BOOLEAN AS $$
DECLARE
    v_owner_id INTEGER;
    v_unit VARCHAR(20);
    v_amount NUMERIC;
    v_points NUMERIC;
    v_kg_factor NUMERIC;
    v_carbon NUMERIC;
BEGIN
	-- Collections'ta verilen puanları günceller
	UPDATE collections 
    SET rating_cleanliness = p_clean,
        rating_accuracy = p_acc,
        rating_punctuality = p_punc,
        rating_avg = (p_clean + p_acc + p_punc) / 3, 
        collection_date = CURRENT_TIMESTAMP 
    WHERE waste_id = p_waste_id;    
	
    SELECT w.owner_id, w.amount, w.unit, c.carbon_factor, c.unit_to_kg_factor
    INTO v_owner_id, v_amount, v_unit, v_carbon, v_kg_factor
    FROM wastes w JOIN waste_categories c ON w.category_id = c.category_id WHERE w.waste_id = p_waste_id;
    
    -- Puan hesabı
    IF v_unit = 'KG' THEN v_points := v_amount * v_carbon;
    ELSE v_points := (v_amount * v_kg_factor) * v_carbon;
    END IF;
    
	-- Users ve wastes tablolarını günceller
    UPDATE users SET score = score + v_points WHERE user_id = v_owner_id;
    UPDATE wastes SET status = 'TAMAMLANDI' WHERE waste_id = p_waste_id;
    
    RETURN TRUE;
EXCEPTION WHEN OTHERS THEN RETURN FALSE;
END;
$$ LANGUAGE plpgsql;

-- Bölgesel Analiz (Cursor ile)
CREATE OR REPLACE FUNCTION analyze_district_performance()
RETURNS TEXT AS $$
DECLARE
    -- Tüm ilçeleri listeye alan cursor
    cur_districts CURSOR FOR SELECT district_name FROM tr_districts;
    
    v_dist_name VARCHAR(50);
    v_waste_count INTEGER;
    v_result TEXT := '--- İLÇE RAPORU ---' || E'\n'; -- Rapor başlığı
BEGIN
    OPEN cur_districts; 
    
    LOOP
        FETCH cur_districts INTO v_dist_name;
        EXIT WHEN NOT FOUND;
        
        -- O anki ilçede kaç tane atık var
        SELECT COUNT(*) INTO v_waste_count FROM wastes WHERE district = v_dist_name;
        v_result := v_result || '📍 ' || v_dist_name || ': ' || v_waste_count || ' ilan.' || E'\n';
    END LOOP;
    
    CLOSE cur_districts; 
    RETURN v_result; 
END;
$$ LANGUAGE plpgsql;


-- Bildirim Gönderici Trigger
CREATE OR REPLACE FUNCTION func_notify_waste_status()
RETURNS TRIGGER AS $$
DECLARE
    v_owner_id INTEGER;
    v_cat_name VARCHAR(50);
BEGIN
    IF OLD.status <> NEW.status THEN
        SELECT owner_id, c.category_name INTO v_owner_id, v_cat_name
        FROM wastes w JOIN waste_categories c ON w.category_id = c.category_id WHERE w.waste_id = NEW.waste_id;

        IF NEW.status = 'REZERVEYE_ALINDI' THEN
            INSERT INTO notifications (user_id, message) VALUES (v_owner_id, '🔔 ' || v_cat_name || ' atığınız rezerve edildi!');
        ELSIF NEW.status = 'TAMAMLANDI' THEN
            INSERT INTO notifications (user_id, message) VALUES (v_owner_id, '✅ ' || v_cat_name || ' işlemi tamamlandı.');
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_waste_notification
AFTER UPDATE ON wastes
FOR EACH ROW EXECUTE FUNCTION func_notify_waste_status();

-- Puan Güvenliği Sağlayan Trigger
CREATE OR REPLACE FUNCTION func_prevent_score_hack()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.score - OLD.score) > 500 THEN
        RAISE EXCEPTION 'Güvenlik Uyarısı: Anormal puan artışı!';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_security_score_check
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION func_prevent_score_hack();


-- Örnek Veriler 

-- Atık Kategorileri
INSERT INTO waste_categories (category_name, carbon_factor, unit_to_kg_factor) VALUES
('Plastik', 1.50, 0.04), ('Cam Şişe', 0.25, 0.25), ('Karton', 0.90, 0.30),
('Elektronik', 20.00, 1.00), ('Metal Kutu', 9.00, 0.015), ('Tekstil', 4.50, 0.30),
('Atık Pil', 0.10, 0.02), ('Bitkisel Yağ', 2.80, 0.92), ('Ahşap', 0.50, 1.00),
('Beyaz Eşya', 3.50, 50.00), ('Organik', 0.10, 1.00), ('Lastik', 2.00, 8.00);


-- Kullanıcılar
INSERT INTO users (email, password, full_name, role, score) VALUES
('ali@mail.com', '123', 'Ali Yılmaz', 'SAKIN', 55.00),
('veli@mail.com', '123', 'Veli Demir', 'TOPLAYICI', 0),
('ayse@mail.com', '123', 'Ayşe Kara', 'SAKIN', 1.25),
('fatma@mail.com', '123', 'Fatma Çelik', 'SAKIN', 4.50),
('mehmet@mail.com', '123', 'Mehmet Öz', 'TOPLAYICI', 0),
('ahmet@mail.com', '123', 'Ahmet Sarı', 'SAKIN', 13.50),
('zeynep@mail.com', '123', 'Zeynep Mavi', 'TOPLAYICI', 0),
('can@mail.com', '123', 'Can Yeşil', 'SAKIN', 0),
('cem@mail.com', '123', 'Cem Mor', 'SAKIN', 0),
('elif@mail.com', '123', 'Elif Turuncu', 'TOPLAYICI', 0);

-- Adresler
INSERT INTO addresses (user_id, title, city, district, neighborhood, street, building_no, floor_no, door_no, directions, full_address_text) VALUES
(1, 'Ev', 'İstanbul', 'Kadıköy', 'Caferağa', 'Moda Cad.', '10', '3', '8', 'Starbucks yanı', 'Caferağa Mah. Moda Cad. No:10 D:8 Kadıköy/İstanbul'),
(1, 'Ofis', 'İstanbul', 'Ataşehir', 'Barbaros', 'Halk Cad.', '55', '12', '45', 'Palladium AVM karşısı', 'Barbaros Mah. Halk Cad. No:55 D:45 Ataşehir/İstanbul'),
(3, 'Annemler', 'İstanbul', 'Beşiktaş', 'Bebek', 'Sahil Yolu', '5', '1', '2', 'Parkın karşısı', 'Bebek Mah. Sahil Yolu No:5 D:2 Beşiktaş/İstanbul'),
(3, 'Yazlık', 'İstanbul', 'Şile', 'Merkez', 'Fener Cad.', '20', 'Bahçe', '1', 'Deniz kenarı', 'Merkez Mah. Fener Cad. No:20 Şile/İstanbul'),
(4, 'Ev', 'İstanbul', 'Fatih', 'Aksaray', 'Millet Cad.', '20', '4', '11', 'Tramvay durağına yakın', 'Aksaray Mah. Millet Cad. No:20 D:11 Fatih/İstanbul'),
(6, 'Atölye', 'İstanbul', 'Üsküdar', 'Kuzguncuk', 'İcadiye Cad.', '3', 'Zemin', '1', 'Fırının yanı', 'Kuzguncuk Mah. İcadiye Cad. No:3 Üsküdar/İstanbul'),
(6, 'Depo', 'İstanbul', 'Ümraniye', 'Çakmak', 'Sanayi Sok.', '99', 'Giriş', 'A', 'Sanayi sitesi içi', 'Çakmak Mah. Sanayi Sok. No:99 Ümraniye/İstanbul'),
(8, 'Ev', 'İstanbul', 'Esenler', 'Davutpaşa', 'Yıldız Sok.', '8', '2', '5', 'Okul arkası', 'Davutpaşa Mah. Yıldız Sok. No:8 D:5 Esenler/İstanbul'),
(9, 'Dükkan', 'İstanbul', 'Kadıköy', 'Göztepe', 'Tütüncü Mehmet Efendi', '12', 'Giriş', '1', 'Parkın alt sokağı', 'Göztepe Mah. Tütüncü Mehmet Efendi No:12 Kadıköy/İstanbul'),
(9, 'Lojman', 'İstanbul', 'Maltepe', 'Küçükyalı', 'Atatürk Cad.', '44', '5', '10', 'Minibüs yolu üstü', 'Küçükyalı Mah. Atatürk Cad. No:44 D:10 Maltepe/İstanbul');



-- Atıklar
INSERT INTO wastes (owner_id, category_id, city, district, full_location_text, amount, unit, status, created_at) VALUES
(1, 1, 'İstanbul', 'Kadıköy', 'Caferağa Mah. Moda Cad. No:10 D:8 Kadıköy/İstanbul', 10, 'KG', 'TAMAMLANDI', '2026-01-01 09:00:00'),
(3, 2, 'İstanbul', 'Beşiktaş', 'Bebek Mah. Sahil Yolu No:5 D:2 Beşiktaş/İstanbul', 20, 'ADET', 'TAMAMLANDI', '2026-01-02 08:30:00'), 
(4, 3, 'İstanbul', 'Fatih', 'Aksaray Mah. Millet Cad. No:20 D:11 Fatih/İstanbul', 5, 'KG', 'TAMAMLANDI', '2026-01-03 10:00:00'), 
(6, 4, 'İstanbul', 'Üsküdar', 'Kuzguncuk Mah. İcadiye Cad. No:3 Üsküdar/İstanbul', 2, 'ADET', 'TAMAMLANDI', '2026-01-04 11:00:00'), 
(6, 5, 'İstanbul', 'Ümraniye', 'Çakmak Mah. Sanayi Sok. No:99 Ümraniye/İstanbul', 100, 'ADET', 'TAMAMLANDI', '2026-01-05 09:00:00'), 
(1, 4, 'İstanbul', 'Ataşehir', 'Barbaros Mah. Halk Cad. No:55 D:45 Ataşehir/İstanbul', 2, 'ADET', 'TAMAMLANDI', '2026-01-06 14:00:00'), 
(8, 1, 'İstanbul', 'Esenler', 'Davutpaşa Mah. Yıldız Sok. No:8 D:5 Esenler/İstanbul', 5, 'KG', 'REZERVEYE_ALINDI', NOW()), 
(9, 3, 'İstanbul', 'Kadıköy', 'Göztepe Mah. Tütüncü Mehmet Efendi No:12 Kadıköy/İstanbul', 10, 'KG', 'REZERVEYE_ALINDI', NOW()), 
(3, 5, 'İstanbul', 'Şile', 'Merkez Mah. Fener Cad. No:20 Şile/İstanbul', 50, 'ADET', 'REZERVEYE_ALINDI', NOW()), 
(4, 2, 'İstanbul', 'Fatih', 'Aksaray Mah. Millet Cad. No:20 D:11 Fatih/İstanbul', 10, 'ADET', 'MUSAIT', NOW()); 


-- Koleksiyonlar
INSERT INTO collections (waste_id, collector_id, rating_avg, rating_cleanliness, rating_accuracy, rating_punctuality, reserved_at, collection_date) VALUES
(1, 2, 5, 5, 5, 5, '2026-01-01 10:00:00', '2026-01-01 12:30:00'), 
(2, 5, 4, 4, 5, 3, '2026-01-02 09:15:00', '2026-01-02 11:00:00'), 
(3, 7, 5, 5, 5, 5, '2026-01-03 14:00:00', '2026-01-03 16:45:00'), 
(4, 10, 5, 5, 5, 5, '2026-01-04 12:00:00', '2026-01-04 13:00:00'), 
(5, 2, 4, 5, 4, 3, '2026-01-05 10:00:00', '2026-01-05 11:30:00'), 
(6, 5, 5, 5, 5, 5, '2026-01-06 15:00:00', '2026-01-06 16:00:00'), 
(7, 7, 0, 0, 0, 0, NOW(), NULL), 
(8, 10, 0, 0, 0, 0, NOW(), NULL), 
(9, 2, 0, 0, 0, 0, NOW(), NULL); 



-- 5. Bildirimler
INSERT INTO notifications (user_id, message, is_read, created_at) VALUES
(1, '🔔 Plastik atığınız rezerve edildi!', TRUE, '2026-01-01 10:05:00'),
(1, '✅ Plastik işlemi tamamlandı. (15 Puan)', TRUE, '2026-01-01 12:35:00'),
(1, '🔔 Elektronik atığınız rezerve edildi!', TRUE, '2026-01-06 15:05:00'),
(1, '✅ Elektronik işlemi tamamlandı. (40 Puan)', FALSE, '2026-01-06 16:05:00'),
(3, '🔔 Cam Şişe atığınız rezerve edildi!', TRUE, '2026-01-02 09:20:00'),
(3, '✅ Cam Şişe işlemi tamamlandı.', TRUE, '2026-01-02 11:05:00'),
(3, '🔔 Metal Kutu atığınız rezerve edildi!', FALSE, NOW()),
(4, '🔔 Karton atığınız rezerve edildi!', TRUE, '2026-01-03 14:05:00'),
(4, '✅ Karton işlemi tamamlandı.', TRUE, '2026-01-03 16:50:00'),
(6, '✅ Elektronik işlemi tamamlandı.', TRUE, '2026-01-04 13:10:00');


-- İlçeler
INSERT INTO tr_districts (district_name) VALUES 
('Kadıköy'), ('Beşiktaş'), ('Fatih'), ('Esenler'), ('Üsküdar'),
('Şişli'), ('Maltepe'), ('Kartal'), ('Pendik'), ('Ümraniye'),
('Ataşehir'), ('Beyoğlu'), ('Bakırköy'), ('Zeytinburnu'), ('Bağcılar');

-- Mahalleler
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




















