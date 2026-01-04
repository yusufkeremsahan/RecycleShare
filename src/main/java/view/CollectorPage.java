package view;

import dao.WasteDAO;
import model.Waste;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.Optional;

public class CollectorPage {

    private String username;
    private WasteDAO wasteDAO = new WasteDAO();
    private TableView<Waste> table = new TableView<>();
    private Stage stage;

    // Hangi moddayız? (true = Müsait olanlar, false = Rezerve ettiklerim)
    private boolean isViewingAvailable = true;

    // Arayüz elemanlarını kontrol etmek için referanslar
    private ToggleButton tglAvailable;
    private ToggleButton tglReserved;
    private Button btnAction;

    public CollectorPage(String username) {
        this.username = username;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Toplayıcı Paneli (" + username + ")");

        // 1. ARKA PLAN (Yeşil Gradyan)
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        // 2. ANA DÜZEN (BorderPane)
        BorderPane mainLayout = new BorderPane();

        // --- ÜST BAR (HEADER) ---
        HBox header = createHeader();
        mainLayout.setTop(header);

        // --- ORTA ALAN (BEYAZ KART) ---
        VBox contentCard = createContentCard();

        // Kartı merkeze yerleştir ve kenarlardan boşluk bırak
        BorderPane.setMargin(contentCard, new Insets(20));
        mainLayout.setCenter(contentCard);

        rootPane.getChildren().add(mainLayout);

        // --- BAŞLANGIÇ AYARLARI ---
        setupTable();
        refreshTable();

        Scene scene = new Scene(rootPane, 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // TASARIM BİLEŞENLERİ (UI)
    // ==========================================

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Sol Taraf: Marka ve Kullanıcı Bilgisi
        VBox titleBox = new VBox(2);
        Label lblBrand = new Label("RecycleShare");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblBrand.setTextFill(Color.web("#2E7D32"));

        Label lblUser = new Label("Toplayıcı: " + username);
        lblUser.setFont(Font.font("Segoe UI", 12));
        lblUser.setTextFill(Color.GRAY);
        titleBox.getChildren().addAll(lblBrand, lblUser);

        // Aradaki Boşluk
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sağ Taraf: Çıkış Butonu
        Button btnLogout = new Button("Çıkış Yap 🚪");
        styleDangerButton(btnLogout);
        btnLogout.setOnAction(e -> {
            stage.close();
            try {
                new LoginApp().start(new Stage());
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        header.getChildren().addAll(titleBox, spacer, btnLogout);
        return header;
    }

    private VBox createContentCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        // Kart Stili: Beyaz, Yuvarlak Köşe, Gölge
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

        // --- 1. SATIR: Arama ve Yenileme ---
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Mahalle Ara...");
        styleField(txtSearch);
        txtSearch.setPrefWidth(300);

        Button btnSearch = new Button("Ara 🔍");
        styleSecondaryButton(btnSearch);

        Button btnRefresh = new Button("🔄");
        styleSecondaryButton(btnRefresh);
        Tooltip.install(btnRefresh, new Tooltip("Tabloyu Yenile"));

        // Arama Aksiyonları
        btnSearch.setOnAction(e -> {
            String keyword = txtSearch.getText();
            if (isViewingAvailable) {
                table.setItems(FXCollections.observableArrayList(wasteDAO.searchWastesByDistrict(keyword)));
            } else {
                showAlert("Bilgi", "Arama sadece 'Müsait Atıklar' modunda çalışır.");
            }
        });
        btnRefresh.setOnAction(e -> {
            txtSearch.clear();
            refreshTable();
        });

        topRow.getChildren().addAll(txtSearch, btnSearch, btnRefresh);

        // --- 2. SATIR: Sekmeler (Tabs) ---
        HBox tabRow = new HBox(0); // Birleşik butonlar
        tabRow.setAlignment(Pos.CENTER);

        tglAvailable = new ToggleButton("Müsait Atıklar");
        tglReserved = new ToggleButton("Rezerve Ettiklerim");

        ToggleGroup group = new ToggleGroup();
        tglAvailable.setToggleGroup(group);
        tglReserved.setToggleGroup(group);
        tglAvailable.setSelected(true);

        // Sekme Stilleri (Segmented Control Görünümü)
        styleTabButton(tglAvailable, true);
        styleTabButton(tglReserved, false);

        // Mod Değiştirme Aksiyonları
        tglAvailable.setOnAction(e -> switchMode(true));
        tglReserved.setOnAction(e -> switchMode(false));

        tabRow.getChildren().addAll(tglAvailable, tglReserved);

        // --- 3. SATIR: Tablo ---
        VBox.setVgrow(table, Priority.ALWAYS); // Tablo kalan alanı kaplasın
        // Tablonun kendi kenarlıklarını kaldırıp karta uyumlu hale getirelim
        table.setStyle("-fx-base: #FFFFFF; -fx-control-inner-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-padding: 5;");

        // --- 4. SATIR: Ana Aksiyon Butonu ---
        btnAction = new Button("SEÇİLENİ REZERVE ET 🚛");
        stylePrimaryButton(btnAction);
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setPrefHeight(50); // Büyük buton

        // Ana Buton Aksiyonu
        btnAction.setOnAction(e -> handleMainAction());

        card.getChildren().addAll(topRow, new Separator(), tabRow, table, btnAction);
        return card;
    }

    // ==========================================
    // MANTIK VE İŞLEMLER (Business Logic)
    // ==========================================

    private void switchMode(boolean showAvailable) {
        isViewingAvailable = showAvailable;
        if (showAvailable) {
            btnAction.setText("SEÇİLENİ REZERVE ET 🚛");
            // Turuncu Stil
            btnAction.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
            tglAvailable.setSelected(true);
        } else {
            btnAction.setText("TESLİM AL VE PUANLA ✅");
            // Yeşil Stil
            btnAction.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
            tglReserved.setSelected(true);
        }
        refreshTable();
    }

    private void handleMainAction() {
        Waste selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Uyarı", "Lütfen listeden bir atık seçiniz!");
            return;
        }

        if (isViewingAvailable) {
            // --- MOD 1: REZERVE ETME ---

            // 1. KURAL: Tek Aktif Rezervasyon Kontrolü
            if (wasteDAO.hasActiveReservation(username)) {
                showAlert("İşlem Engellendi ⛔",
                        "Aynı anda sadece tek bir rezervasyon yapabilirsiniz!\n" +
                                "Lütfen önce mevcut işinizi tamamlayın.");
                return;
            }

            boolean success = wasteDAO.reserveWaste(selected.getId(), username);
            if (success) {
                showAlert("Başarılı", "Atık rezerve edildi! 'Rezerve Ettiklerim' sekmesine geçebilirsiniz.");
                refreshTable();
            } else {
                showAlert("Hata", "Rezervasyon yapılamadı.");
            }
        } else {
            // --- MOD 2: PUANLAMA (Eski Sistem: ChoiceDialog) ---
            handleCompletion(selected);
        }
    }

    // Mevcut basit puanlama sistemi (İsteğin üzerine şimdilik bu kaldı)
    private void handleCompletion(Waste waste) {
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(5, 1, 2, 3, 4, 5);
        dialog.setTitle("Puanlama");
        dialog.setHeaderText("Teslimat Tamamlanıyor...");
        dialog.setContentText("Lütfen Sakin'e (Atık Sahibine) puan verin (1-5):");

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(rating -> {
            boolean success = wasteDAO.completeCollection(waste.getId(), rating);
            if (success) {
                showAlert("İşlem Tamam", "Teslim alındı! Puan kaydedildi.");
                refreshTable();
            } else {
                showAlert("Hata", "İşlem sırasında hata oluştu.");
            }
        });
    }

    private void setupTable() {
        table.getColumns().clear();

        TableColumn<Waste, String> colCat = new TableColumn<>("Kategori");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Waste, String> colDist = new TableColumn<>("Mahalle");
        colDist.setCellValueFactory(new PropertyValueFactory<>("district"));

        TableColumn<Waste, Double> colAmount = new TableColumn<>("Miktar");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Yeni eklenen Birim Sütunu
        TableColumn<Waste, String> colUnit = new TableColumn<>("Birim");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Waste, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colCat, colDist, colAmount, colUnit, colStatus);

        // Sütun Genişlik Ayarları (Karta sığacak şekilde optimize edildi)
        colCat.setMaxWidth(150); colCat.setMinWidth(100);
        colDist.setMaxWidth(150); colDist.setMinWidth(100);
        colAmount.setMaxWidth(100); colAmount.setMinWidth(70);
        colUnit.setMaxWidth(80); colUnit.setMinWidth(50);
        colStatus.setMinWidth(200);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> {
            col.setReorderable(false);
            col.setResizable(false);
        });
    }

    private void refreshTable() {
        if (isViewingAvailable) {
            table.setItems(FXCollections.observableArrayList(wasteDAO.getAvailableWastes()));
        } else {
            table.setItems(FXCollections.observableArrayList(wasteDAO.getMyReservations(username)));
        }
    }

    // ==========================================
    // STİL YARDIMCILARI (CSS benzeri)
    // ==========================================

    private void styleField(TextField txt) {
        txt.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        txt.setFont(Font.font("Segoe UI", 13));
    }

    private void stylePrimaryButton(Button btn) {
        btn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
    }

    private void styleSecondaryButton(Button btn) {
        btn.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333; -fx-border-color: #DDD; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
    }

    private void styleDangerButton(Button btn) {
        btn.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-border-color: #FFCDD2; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 15 5 15;");
    }

    private void styleTabButton(ToggleButton btn, boolean isLeft) {
        // Sol ve sağ butonlar için köşe yuvarlatma (Bitişik görünüm için)
        String radius = isLeft ? "8 0 0 8" : "0 8 8 0";

        // JavaFX Bindings: Seçiliyken Yeşil, Değilken Beyaz
        btn.styleProperty().bind(javafx.beans.binding.Bindings.when(btn.selectedProperty())
                .then("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: " + radius + "; -fx-border-color: #2E7D32; -fx-border-radius: " + radius + "; -fx-font-weight: bold;")
                .otherwise("-fx-background-color: white; -fx-text-fill: #2E7D32; -fx-background-radius: " + radius + "; -fx-border-color: #2E7D32; -fx-border-radius: " + radius + ";"));

        btn.setPrefWidth(160);
        btn.setPrefHeight(35);
        btn.setCursor(javafx.scene.Cursor.HAND);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}