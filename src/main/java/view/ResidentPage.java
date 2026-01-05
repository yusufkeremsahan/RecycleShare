package view;

import dao.AddressDAO;
import dao.LocationDAO;
import dao.UserDAO;
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

import java.util.Optional; // Silme onayı için gerekli

public class ResidentPage {

    private String userEmail;
    private WasteDAO wasteDAO = new WasteDAO();
    private UserDAO userDAO = new UserDAO();
    private AddressDAO addressDAO = new AddressDAO();
    private LocationDAO locationDAO = new LocationDAO();

    private TableView<Waste> table = new TableView<>();
    private TableView<UserDAO.UserScore> tableTop = new TableView<>();
    private Stage stage;
    private Label lblMsg;

    // Form alanlarını sınıf seviyesine çıkardım ki "Temizle" metodunda erişebilelim
    private ComboBox<String> cmbCity, cmbDistrict, cmbNeigh, cmbSavedAddresses;
    private TextField txtStreet, txtBuildNo, txtFloor, txtDoor, txtAddrTitle, txtAmount;
    private TextArea txtDirections;
    private CheckBox chkSave;

    public ResidentPage(String email) {
        this.userEmail = email;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Sakin Paneli (" + userEmail + ")");
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());

        BorderPane contentArea = new BorderPane();
        contentArea.setPadding(new Insets(20));

        // Sol: Form
        VBox leftCard = createFormCard();
        contentArea.setLeft(leftCard);
        BorderPane.setMargin(leftCard, new Insets(0, 15, 0, 0));

        // Sağ: Liderlik
        VBox rightCard = createLeaderboardCard();
        contentArea.setRight(rightCard);
        BorderPane.setMargin(rightCard, new Insets(0, 0, 0, 15));

        // Orta: Tablo
        VBox centerCard = createTableCard();
        contentArea.setCenter(centerCard);

        mainLayout.setCenter(contentArea);
        rootPane.getChildren().add(mainLayout);

        refreshTable();
        refreshLeaderboard();

        Scene scene = new Scene(rootPane, 1300, 760);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        VBox titleBox = new VBox(2);
        Label lblBrand = new Label("RecycleShare");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblBrand.setTextFill(Color.web("#2E7D32"));
        Label lblUser = new Label("Sakin: " + userEmail);
        lblUser.setFont(Font.font("Segoe UI", 12));
        lblUser.setTextFill(Color.GRAY);
        titleBox.getChildren().addAll(lblBrand, lblUser);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Çıkış Yap 🚪");
        styleDangerButton(btnLogout);
        btnLogout.setOnAction(e -> {
            stage.close();
            try { new LoginApp().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
        });
        header.getChildren().addAll(titleBox, spacer, btnLogout);
        return header;
    }

    // --- YENİLENMİŞ VE GELİŞMİŞ FORM KARTI ---
    private VBox createFormCard() {
        // Ana Kart Kutusu
        VBox mainCard = new VBox(10);
        mainCard.setPadding(new Insets(15));
        mainCard.setPrefWidth(350);
        styleCard(mainCard);

        // 1. BAŞLIK (Sabit - ScrollPane dışında)
        Label lblTitle = new Label("Atık & Adres Bilgileri 📍");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#2E7D32"));
        lblTitle.setPadding(new Insets(0, 0, 5, 0));

        // --- SCROLL EDİLEBİLİR İÇERİK ---
        VBox scrollContent = new VBox(10);
        scrollContent.setPadding(new Insets(5));

        // 2. KAYITLI ADRESLER VE SİLME BUTONU
        Label lblSaved = new Label("Kayıtlı Adreslerim:");
        lblSaved.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        cmbSavedAddresses = new ComboBox<>();
        cmbSavedAddresses.setMaxWidth(Double.MAX_VALUE);
        refreshAddressCombo(); // Combobox'ı dolduran yardımcı metot

        Button btnDeleteAddr = new Button("🗑️");
        btnDeleteAddr.setStyle("-fx-background-color: #ffebee; -fx-text-fill: red; -fx-border-color: #ffcdd2; -fx-border-radius: 5;");
        btnDeleteAddr.setTooltip(new Tooltip("Seçili adresi sil"));
        btnDeleteAddr.setDisable(true); // Başlangıçta pasif

        HBox addressBox = new HBox(5, cmbSavedAddresses, btnDeleteAddr);
        HBox.setHgrow(cmbSavedAddresses, Priority.ALWAYS);

        // 3. ADRES FORMU ALANLARI
        txtStreet = new TextField(); txtStreet.setPromptText("Cadde / Sokak"); styleField(txtStreet);

        cmbCity = new ComboBox<>(); cmbCity.setPromptText("İl"); cmbCity.setMaxWidth(Double.MAX_VALUE);
        cmbCity.getItems().addAll(locationDAO.getAllCities());

        cmbDistrict = new ComboBox<>(); cmbDistrict.setPromptText("İlçe"); cmbDistrict.setMaxWidth(Double.MAX_VALUE); cmbDistrict.setDisable(true);

        HBox rowCityDist = new HBox(5, cmbCity, cmbDistrict);
        HBox.setHgrow(cmbCity, Priority.ALWAYS); HBox.setHgrow(cmbDistrict, Priority.ALWAYS);

        cmbNeigh = new ComboBox<>(); cmbNeigh.setPromptText("Mahalle"); cmbNeigh.setMaxWidth(Double.MAX_VALUE); cmbNeigh.setDisable(true);

        txtBuildNo = new TextField(); txtBuildNo.setPromptText("Bina No"); styleField(txtBuildNo);
        txtFloor = new TextField(); txtFloor.setPromptText("Kat"); styleField(txtFloor);
        txtDoor = new TextField(); txtDoor.setPromptText("Daire"); styleField(txtDoor);
        HBox rowBuildInfo = new HBox(5, txtBuildNo, txtFloor, txtDoor);

        txtDirections = new TextArea();
        txtDirections.setPromptText("Adres Tarifi (İsteğe bağlı)");
        txtDirections.setPrefRowCount(2);
        txtDirections.setStyle("-fx-control-inner-background: #f9f9f9; -fx-border-color: #e0e0e0;");

        // Kayıt Checkbox ve Başlık
        chkSave = new CheckBox("Bu adresi kaydet");
        chkSave.setStyle("-fx-text-fill: #333; -fx-font-size: 12px;");

        txtAddrTitle = new TextField();
        txtAddrTitle.setPromptText("Adres Başlığı (Örn: Ev, İş)");
        styleField(txtAddrTitle);
        txtAddrTitle.setVisible(false); // Başlangıçta gizli
        txtAddrTitle.managedProperty().bind(txtAddrTitle.visibleProperty()); // Yer kaplamasın

        // --- ATIK DETAYLARI ---
        Separator sep = new Separator();
        Label lblWaste = new Label("Atık Detayı:");
        lblWaste.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.setPromptText("Kategori Seç");
        try { cmbCategory.getItems().addAll(wasteDAO.getCategories()); } catch (Exception ex) {}
        styleComboBox(cmbCategory);

        txtAmount = new TextField(); txtAmount.setPromptText("Miktar"); styleField(txtAmount);
        ComboBox<String> cmbUnit = new ComboBox<>(); cmbUnit.setPromptText("Birim"); styleComboBox(cmbUnit); cmbUnit.setPrefWidth(90);

        HBox amountBox = new HBox(5, txtAmount, cmbUnit);
        HBox.setHgrow(txtAmount, Priority.ALWAYS);

        // --- BUTONLAR ---
        Button btnAdd = new Button("SİPARİŞ OLUŞTUR ✅");
        stylePrimaryButton(btnAdd);

        lblMsg = new Label(); lblMsg.setWrapText(true);

        // --- OLAYLAR (EVENTS) ---

        // 1. Yeni Adres veya Kayıtlı Adres Seçimi
        cmbSavedAddresses.setOnAction(e -> {
            String selected = cmbSavedAddresses.getValue();
            if (selected == null) return;

            if (selected.equals("✨ Yeni Adres Oluştur...")) {
                clearAddressFields();
                btnDeleteAddr.setDisable(true);
            } else {
                // Veritabanından doldur
                AddressDAO.AddressDetails d = addressDAO.getAddressDetails(userEmail, selected);
                if (d != null) {
                    cmbCity.setValue(d.city);
                    // Cascade tetikleneceği için ilçe ve mahalleyi manuel set ediyoruz
                    cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(d.city));
                    cmbDistrict.setDisable(false);
                    cmbDistrict.setValue(d.district);

                    cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(d.district));
                    cmbNeigh.setDisable(false);
                    cmbNeigh.setValue(d.neighborhood);

                    txtStreet.setText(d.street);
                    txtBuildNo.setText(d.buildingNo);
                    txtFloor.setText(d.floorNo);
                    txtDoor.setText(d.doorNo);
                    txtDirections.setText(d.directions);

                    btnDeleteAddr.setDisable(false); // Silinebilir
                    chkSave.setSelected(false); // Zaten kayıtlı
                }
            }
        });

        // 2. Adres Silme
        btnDeleteAddr.setOnAction(e -> {
            String title = cmbSavedAddresses.getValue();
            if (title != null && !title.equals("✨ Yeni Adres Oluştur...")) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Adres Sil");
                alert.setHeaderText("'" + title + "' adresini silmek istediğinize emin misiniz?");
                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (addressDAO.deleteAddress(userEmail, title)) {
                        refreshAddressCombo();
                        clearAddressFields();
                        lblMsg.setText("Adres silindi.");
                        lblMsg.setTextFill(Color.BLACK);
                    }
                }
            }
        });

        // 3. Checkbox Görünürlüğü
        chkSave.selectedProperty().addListener((obs, oldVal, newVal) -> txtAddrTitle.setVisible(newVal));

        // 4. İl-İlçe-Mahalle Zinciri
        cmbCity.setOnAction(e -> {
            if (cmbCity.getValue() != null) {
                cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(cmbCity.getValue()));
                cmbDistrict.setDisable(false);
                cmbNeigh.getItems().clear(); cmbNeigh.setDisable(true);
            }
        });
        cmbDistrict.setOnAction(e -> {
            if (cmbDistrict.getValue() != null) {
                cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(cmbDistrict.getValue()));
                cmbNeigh.setDisable(false);
            }
        });

        // 5. Akıllı Birim
        cmbCategory.setOnAction(e -> {
            String sel = cmbCategory.getValue();
            if (sel != null) {
                cmbUnit.getItems().clear();
                switch (sel) {
                    case "Bitkisel Yağ": cmbUnit.getItems().addAll("LITRE", "KG"); cmbUnit.setValue("LITRE"); break;
                    case "Cam Şişe": case "Elektronik": case "Beyaz Eşya": cmbUnit.getItems().addAll("ADET", "KG"); cmbUnit.setValue("ADET"); break;
                    default: cmbUnit.getItems().addAll("KG", "ADET"); cmbUnit.setValue("KG"); break;
                }
            }
        });

        // 6. Ekleme Butonu
        btnAdd.setOnAction(e -> {
            try {
                // 1. ADIM: FORMDAKİ VERİLERİ HEMEN DEĞİŞKENLERE AL (UI silinmeden önce!)
                String city = cmbCity.getValue();
                String district = cmbDistrict.getValue();
                String neigh = cmbNeigh.getValue();
                String street = txtStreet.getText();
                String buildNo = txtBuildNo.getText();
                String floor = txtFloor.getText();
                String door = txtDoor.getText();
                String directions = txtDirections.getText();
                String category = cmbCategory.getValue();
                String unit = cmbUnit.getValue();
                String amountStr = txtAmount.getText();
                String addrTitle = txtAddrTitle.getText(); // Sadece kayıt varsa dolu olur

                // 2. ADIM: VALIDASYON (Değişkenler üzerinden kontrol et)
                if (city == null || district == null || neigh == null ||
                        street.isEmpty() || buildNo.isEmpty() || door.isEmpty() ||
                        category == null || amountStr.isEmpty()) {

                    lblMsg.setText("Lütfen zorunlu alanları (*) doldurun.");
                    lblMsg.setTextFill(Color.RED);
                    return;
                }

                // 3. ADIM: ADRES KAYDI (Varsa)
                if (chkSave.isSelected()) {
                    if (addrTitle.isEmpty()) {
                        lblMsg.setText("Kaydedilecek adres için bir başlık girin!"); lblMsg.setTextFill(Color.RED); return;
                    }
                    // Değişkenleri kullanarak kaydet
                    addressDAO.saveAddress(userEmail, addrTitle, city, district, neigh,
                            street, buildNo, floor, door, directions);

                    // DİKKAT: Bu metot UI'ı temizler ama artık sorun değil, verileri aldık!
                    refreshAddressCombo();
                }

                // 4. ADIM: SİPARİŞ OLUŞTURMA (Artık değişkenleri kullanıyoruz, UI'ı değil)
                String fullLoc = String.format("%s Mah. %s No:%s D:%s %s/%s",
                        neigh, street, buildNo, door, district, city);

                double amt = Double.parseDouble(amountStr);

                // 'city', 'district' değişkenlerini gönderiyoruz (cmbCity.getValue() yerine)
                if (wasteDAO.addWaste(userEmail, category, city, district, fullLoc, amt, unit)) {
                    lblMsg.setText("Sipariş başarıyla oluşturuldu! 🚀"); lblMsg.setTextFill(Color.GREEN);
                    txtAmount.clear();

                    // Adres kaydedilmediyse bile işlem bitince formu temizleyebiliriz (İsteğe bağlı)
                    if (!chkSave.isSelected()) {
                        // clearAddressFields(); // İstersen bunu açabilirsin
                    }

                    refreshTable();
                } else {
                    lblMsg.setText("Hata oluştu."); lblMsg.setTextFill(Color.RED);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                lblMsg.setText("Miktar sayı olmalı."); lblMsg.setTextFill(Color.RED);
            }
        });

        // Rapor Butonu
        Button btnReport = new Button("ETKİ RAPORUMU GÖR 🌍");
        styleInfoButton(btnReport);
        btnReport.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Rapor"); a.setHeaderText("Etki Raporu");
            a.setContentText(userDAO.getImpactReport(userEmail));
            a.getDialogPane().setMinHeight(250);
            a.showAndWait();
        });

        // İçeriği Topla
        scrollContent.getChildren().addAll(
                lblSaved, addressBox,
                txtStreet, rowCityDist, cmbNeigh, rowBuildInfo, txtDirections,
                chkSave, txtAddrTitle,
                sep, lblWaste, cmbCategory, amountBox,
                new Label(""), btnAdd, lblMsg, new Separator(), btnReport
        );

        // ScrollPane Ayarları
        ScrollPane scrollPane = new ScrollPane(scrollContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Ana Karta Ekle (Başlık Sabit, İçerik Kayan)
        mainCard.getChildren().addAll(lblTitle, scrollPane);

        return mainCard;
    }

    // Yardımcı: Adres Combobox Yenileme
    private void refreshAddressCombo() {
        cmbSavedAddresses.getItems().clear();
        cmbSavedAddresses.getItems().add("✨ Yeni Adres Oluştur...");
        cmbSavedAddresses.getItems().addAll(addressDAO.getUserAddressTitles(userEmail));
        cmbSavedAddresses.getSelectionModel().selectFirst(); // "Yeni Adres" seçili gelsin
    }

    // Yardımcı: Alanları Temizle
    private void clearAddressFields() {
        txtStreet.clear();
        txtBuildNo.clear();
        txtFloor.clear();
        txtDoor.clear();
        txtDirections.clear();
        txtAddrTitle.clear();
        chkSave.setSelected(false);
        cmbCity.getSelectionModel().clearSelection();
        cmbDistrict.getItems().clear(); cmbDistrict.setDisable(true);
        cmbNeigh.getItems().clear(); cmbNeigh.setDisable(true);
    }

    // --- TABLO VE DİĞERLERİ (AYNI) ---
    private VBox createTableCard() {
        VBox card = new VBox(10); card.setPadding(new Insets(15)); styleCard(card); VBox.setVgrow(card, Priority.ALWAYS);
        Label lbl = new Label("📋 Atık Geçmişim"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        TableColumn<Waste, String> c1 = new TableColumn<>("Kategori"); c1.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Waste, String> c2 = new TableColumn<>("İlçe"); c2.setCellValueFactory(new PropertyValueFactory<>("district"));
        TableColumn<Waste, Double> c3 = new TableColumn<>("Miktar"); c3.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Waste, String> c4 = new TableColumn<>("Birim"); c4.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<Waste, String> c5 = new TableColumn<>("Durum"); c5.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(c1, c2, c3, c4, c5);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().addAll(lbl, table);
        return card;
    }

    private VBox createLeaderboardCard() {
        VBox card = new VBox(10); card.setPadding(new Insets(15)); card.setPrefWidth(250); styleCard(card);
        card.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
        Label lbl = new Label("🏆 Liderlik"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); lbl.setTextFill(Color.web("#FF8F00"));

        TableColumn<UserDAO.UserScore, String> c1 = new TableColumn<>("İsim"); c1.setCellValueFactory(new PropertyValueFactory<>("name"));
        TableColumn<UserDAO.UserScore, Integer> c2 = new TableColumn<>("Puan"); c2.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableTop.getColumns().addAll(c1, c2);
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableTop, Priority.ALWAYS);
        card.getChildren().addAll(lbl, tableTop);
        return card;
    }

    private void refreshTable() { table.setItems(FXCollections.observableArrayList(wasteDAO.getMyWastes(userEmail))); }
    private void refreshLeaderboard() { tableTop.setItems(FXCollections.observableArrayList(userDAO.getTopUsers())); }

    private void styleCard(VBox b) { b.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);"); }
    private void styleField(TextInputControl t) { t.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 8;"); }
    private void styleComboBox(ComboBox<?> c) { c.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;"); c.setMaxWidth(Double.MAX_VALUE); }
    private void stylePrimaryButton(Button b) { b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"); b.setMaxWidth(Double.MAX_VALUE); b.setPadding(new Insets(10)); }
    private void styleInfoButton(Button b) { b.setStyle("-fx-background-color: #0288D1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"); b.setMaxWidth(Double.MAX_VALUE); b.setPadding(new Insets(10)); }
    private void styleDangerButton(Button b) { b.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-border-color: #FFCDD2; -fx-border-radius: 20; -fx-padding: 5 15 5 15;"); }
}