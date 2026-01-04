package view;

import dao.WasteDAO;
import model.Waste; // Waste sınıfın org.example içindeyse 'import org.example.Waste;' yap
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Optional;

public class CollectorPage {

    private String username;
    private WasteDAO wasteDAO = new WasteDAO();
    private TableView<Waste> table = new TableView<>();

    // Pencereyi kapatabilmek için stage referansı
    private Stage stage;

    // Hangi moddayız? (true = Müsait olanlar, false = Rezerve ettiklerim)
    private boolean isViewingAvailable = true;

    public CollectorPage(String username) {
        this.username = username;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Toplayıcı Paneli (" + username + ")");

        // --- ANA ÜST KONTEYNER (VBox) ---
        VBox mainTopContainer = new VBox(10); // 10px boşluklu alt alta dizilim
        mainTopContainer.setPadding(new Insets(10));
        mainTopContainer.setStyle("-fx-background-color: #e0f7fa; -fx-border-color: #b2ebf2; -fx-border-width: 0 0 1 0;");

        // ---------------------------------------------------------
        // SATIR 1: ARAMA, YENİLEME ve ÇIKIŞ (HBox)
        // ---------------------------------------------------------
        HBox searchRow = new HBox(10);

        Label lblSearch = new Label("Mahalle Ara:");
        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Örn: Bornova");

        Button btnSearch = new Button("Ara 🔍");
        Button btnRefresh = new Button("🔄 Yenile");

        // Sağa yaslamak için boşluk (Spacer)
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // ÇIKIŞ BUTONU (Logout)
        Button btnLogout = new Button("Çıkış Yap 🚪");
        btnLogout.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-weight: bold;");

        // Buton Aksiyonları (Satır 1)
        btnSearch.setOnAction(e -> {
            String keyword = txtSearch.getText();
            // Arama sadece müsait atıklarda yapılır
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

        btnLogout.setOnAction(e -> {
            stage.close(); // Mevcut pencreyi kapat
            try {
                new LoginApp().start(new Stage()); // Login ekranını yeniden aç
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        searchRow.getChildren().addAll(lblSearch, txtSearch, btnSearch, btnRefresh, spacer, btnLogout);

        // ---------------------------------------------------------
        // SATIR 2: MOD DEĞİŞTİRME ve İŞLEM BUTONU (HBox)
        // ---------------------------------------------------------
        HBox actionRow = new HBox(10);
        actionRow.setPadding(new Insets(5, 0, 0, 0));

        // Mod Değiştirme Butonları
        ToggleButton tglAvailable = new ToggleButton("Müsait Atıklar");
        ToggleButton tglReserved = new ToggleButton("Rezerve Ettiklerim");
        ToggleGroup group = new ToggleGroup();
        tglAvailable.setToggleGroup(group);
        tglReserved.setToggleGroup(group);
        tglAvailable.setSelected(true); // Varsayılan

        // Aksiyon Butonu (Duruma göre değişecek: Rezerve Et veya Teslim Al)
        Button btnAction = new Button("SEÇİLENİ REZERVE ET 🚛");
        btnAction.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        btnAction.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnAction, Priority.ALWAYS); // Butonu genişlet

        // Mod Değişimi Olayları
        tglAvailable.setOnAction(e -> {
            isViewingAvailable = true;
            btnAction.setText("SEÇİLENİ REZERVE ET 🚛");
            btnAction.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
            refreshTable();
        });

        tglReserved.setOnAction(e -> {
            isViewingAvailable = false;
            btnAction.setText("TESLİM AL VE PUANLA ✅");
            btnAction.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");
            refreshTable();
        });

        // ANA BUTON AKSİYONU
        btnAction.setOnAction(e -> {
            Waste selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Uyarı", "Lütfen listeden bir atık seçiniz!");
                return;
            }

            if (isViewingAvailable) {
                // MOD 1: REZERVE ETME (Insert Trigger)
                boolean success = wasteDAO.reserveWaste(selected.getId(), username);
                if (success) {
                    showAlert("Başarılı", "Atık rezerve edildi! 'Rezerve Ettiklerim' sekmesine geçebilirsiniz.");
                    refreshTable();
                } else {
                    showAlert("Hata", "Rezervasyon yapılamadı.");
                }
            } else {
                // MOD 2: TESLİM ALMA & PUANLAMA (Update Trigger)
                handleCompletion(selected);
            }
        });

        actionRow.getChildren().addAll(tglAvailable, tglReserved, new Label("|"), btnAction);

        // İki satırı ana konteynera ekle
        mainTopContainer.getChildren().addAll(searchRow, actionRow);


        // --- ORTA PANEL (TABLO) ---
        setupTable();
        refreshTable();

        // --- ANA DÜZEN ---
        BorderPane root = new BorderPane();
        root.setTop(mainTopContainer);
        root.setCenter(table);

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.show();
    }

    // Puanlama Dialog Kutusu Açan Metot
    private void handleCompletion(Waste waste) {
        // Kullanıcıya 1'den 5'e kadar seçenek sun
        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(5, 1, 2, 3, 4, 5);
        dialog.setTitle("Puanlama");
        dialog.setHeaderText("Teslimat Tamamlanıyor...");
        dialog.setContentText("Lütfen Sakin'e (Atık Sahibine) puan verin (1-5):");

        Optional<Integer> result = dialog.showAndWait();
        result.ifPresent(rating -> {
            // DAO'yu çağır -> UPDATE atılır -> Trigger çalışır -> Puan artar
            boolean success = wasteDAO.completeCollection(waste.getId(), rating);
            if (success) {
                showAlert("İşlem Tamam", "Teslim alındı! Puan kaydedildi ve kullanıcının skoru güncellendi.");
                refreshTable();
            } else {
                showAlert("Hata", "İşlem sırasında hata oluştu.");
            }
        });
    }

    private void setupTable() {
        // Tabloyu her çağırdığımızda sıfırdan sütun eklememesi için temizle
        table.getColumns().clear();

        TableColumn<Waste, String> colCat = new TableColumn<>("Kategori");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Waste, String> colDist = new TableColumn<>("Mahalle");
        colDist.setCellValueFactory(new PropertyValueFactory<>("district"));

        TableColumn<Waste, Double> colAmount = new TableColumn<>("Miktar");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Waste, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colCat, colDist, colAmount, colStatus);

        colCat.setMaxWidth(200);
        colCat.setMinWidth(160);

        colDist.setMaxWidth(200);
        colDist.setMinWidth(160);

        colAmount.setMaxWidth(180);
        colAmount.setMinWidth(140);

        colStatus.setMinWidth(320);

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
            // Rezerve ettiklerimi getir
            table.setItems(FXCollections.observableArrayList(wasteDAO.getMyReservations(username)));
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}