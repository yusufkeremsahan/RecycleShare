package view;

import dao.AddressDAO;
import dao.LocationDAO; // YENİ: Veritabanından İl/İlçe çekmek için
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

public class ResidentPage {

    private String userEmail;
    private WasteDAO wasteDAO = new WasteDAO();
    private UserDAO userDAO = new UserDAO();
    private AddressDAO addressDAO = new AddressDAO();
    private LocationDAO locationDAO = new LocationDAO(); // YENİ DAO

    private TableView<Waste> table = new TableView<>();
    private TableView<UserDAO.UserScore> tableTop = new TableView<>();
    private Stage stage;
    private Label lblMsg;

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

        // Sol: Form (Genişletildi ve Scroll Eklendi)
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

        // Ekran boyutu formu sığdırmak için biraz artırıldı
        Scene scene = new Scene(rootPane, 1250, 750);
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

    // --- FORM KARTI  ---


    private VBox createFormCard() {
        VBox cardContent = new VBox(10);
        cardContent.setPadding(new Insets(15));
        cardContent.setPrefWidth(340);

        Label lblTitle = new Label("Atık & Adres Bilgileri 📍");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.web("#2E7D32"));

        // 0. KAYITLI ADRESLER
        ComboBox<String> cmbSavedAddresses = new ComboBox<>();
        cmbSavedAddresses.setPromptText("Kayıtlı Adreslerimden Seç...");
        cmbSavedAddresses.setMaxWidth(Double.MAX_VALUE);
        cmbSavedAddresses.getItems().addAll(addressDAO.getUserAddressTitles(userEmail));

        // 1. SATIR: CADDE / SOKAK
        TextField txtStreet = new TextField();
        txtStreet.setPromptText("Cadde / Sokak");
        styleField(txtStreet);

        // 2. SATIR: İL ve İLÇE
        ComboBox<String> cmbCity = new ComboBox<>();
        cmbCity.setPromptText("İl");
        cmbCity.getItems().addAll(locationDAO.getAllCities());
        cmbCity.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cmbDistrict = new ComboBox<>();
        cmbDistrict.setPromptText("İlçe");
        cmbDistrict.setMaxWidth(Double.MAX_VALUE);
        cmbDistrict.setDisable(true);

        HBox rowCityDist = new HBox(10, cmbCity, cmbDistrict);
        HBox.setHgrow(cmbCity, Priority.ALWAYS);
        HBox.setHgrow(cmbDistrict, Priority.ALWAYS);

        // 3. SATIR: MAHALLE
        ComboBox<String> cmbNeigh = new ComboBox<>();
        cmbNeigh.setPromptText("Mahalle Seçiniz");
        cmbNeigh.setMaxWidth(Double.MAX_VALUE);
        cmbNeigh.setDisable(true);

        // 4. SATIR: BİNA NO | KAT | DAİRE
        TextField txtBuildNo = new TextField(); txtBuildNo.setPromptText("Bina No"); styleField(txtBuildNo);
        TextField txtFloor = new TextField(); txtFloor.setPromptText("Kat"); styleField(txtFloor);
        TextField txtDoor = new TextField(); txtDoor.setPromptText("Daire"); styleField(txtDoor);

        HBox rowBuildInfo = new HBox(5, txtBuildNo, txtFloor, txtDoor);

        // 5. SATIR: ADRES TARİFİ
        TextArea txtDirections = new TextArea();
        txtDirections.setPromptText("Adres Tarifi (İsteğe bağlı)");
        txtDirections.setPrefRowCount(2);
        txtDirections.setStyle("-fx-control-inner-background: #f9f9f9; -fx-border-color: #e0e0e0;");

        // 6. SATIR: KAYIT SEÇENEĞİ VE BAŞLIK
        CheckBox chkSave = new CheckBox("Adresimi Kaydet");
        chkSave.setStyle("-fx-text-fill: black; -fx-font-size: 12px;"); // BUG FİX: Yazı rengi siyah yapıldı

        TextField txtAddrTitle = new TextField();
        txtAddrTitle.setPromptText("Adres Başlığı (Ev, İş)");
        styleField(txtAddrTitle);
        txtAddrTitle.visibleProperty().bind(chkSave.selectedProperty());

        // --- EVENTS ---
        cmbCity.setOnAction(e -> {
            String selectedCity = cmbCity.getValue();
            if (selectedCity != null) {
                cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(selectedCity));
                cmbDistrict.setDisable(false);
                cmbNeigh.getItems().clear(); cmbNeigh.setDisable(true);
            }
        });

        cmbDistrict.setOnAction(e -> {
            String selectedDist = cmbDistrict.getValue();
            if (selectedDist != null) {
                cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(selectedDist));
                cmbNeigh.setDisable(false);
            }
        });

        cmbSavedAddresses.setOnAction(e -> {
            String title = cmbSavedAddresses.getValue();
            if (title != null) {
                AddressDAO.AddressDetails d = addressDAO.getAddressDetails(userEmail, title);
                if (d != null) {
                    cmbCity.setValue(d.city);
                    cmbDistrict.setValue(d.district);
                    cmbNeigh.setValue(d.neighborhood);
                    txtStreet.setText(d.street);
                    txtBuildNo.setText(d.buildingNo);
                    txtFloor.setText(d.floorNo);
                    txtDoor.setText(d.doorNo);
                    txtDirections.setText(d.directions);
                }
            }
        });

        Separator sep = new Separator();

        // --- ATIK BİLGİLERİ ---
        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.setPromptText("Kategori");
        try { cmbCategory.getItems().addAll(wasteDAO.getCategories()); } catch (Exception ex) {}
        styleComboBox(cmbCategory);

        TextField txtAmount = new TextField(); txtAmount.setPromptText("Miktar"); styleField(txtAmount);
        ComboBox<String> cmbUnit = new ComboBox<>(); cmbUnit.setPromptText("Birim"); styleComboBox(cmbUnit); cmbUnit.setPrefWidth(80);

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

        HBox amountBox = new HBox(5, txtAmount, cmbUnit);
        HBox.setHgrow(txtAmount, Priority.ALWAYS);

        Button btnAdd = new Button("LİSTEYE EKLE ➕");
        stylePrimaryButton(btnAdd);
        lblMsg = new Label(); lblMsg.setWrapText(true);

        // --- EKLEME VE VALIDASYON ---
        btnAdd.setOnAction(e -> {
            try {
                String city = cmbCity.getValue();
                String dist = cmbDistrict.getValue();
                String neigh = cmbNeigh.getValue();
                String cat = cmbCategory.getValue();
                String unit = cmbUnit.getValue();
                String street = txtStreet.getText();
                String buildNo = txtBuildNo.getText();
                String door = txtDoor.getText();

                // BUG FİX: Detaylı adres alanları da zorunlu hale getirildi
                if (city == null || dist == null || neigh == null || cat == null || txtAmount.getText().isEmpty() ||
                        street.isEmpty() || buildNo.isEmpty() || door.isEmpty()) {

                    lblMsg.setText("Lütfen tüm zorunlu alanları (Cadde, Bina, Kapı No dahil) doldurun!");
                    lblMsg.setTextFill(Color.RED);
                    return;
                }

                if (chkSave.isSelected() && !txtAddrTitle.getText().isEmpty()) {
                    addressDAO.saveAddress(userEmail, txtAddrTitle.getText(), city, dist, neigh,
                            street, buildNo, txtFloor.getText(), door, txtDirections.getText());
                    cmbSavedAddresses.getItems().setAll(addressDAO.getUserAddressTitles(userEmail));
                }

                String fullLoc = String.format("%s Mah. %s No:%s D:%s %s/%s",
                        neigh, street, buildNo, door, dist, city);

                double amt = Double.parseDouble(txtAmount.getText());

                if (wasteDAO.addWaste(userEmail, cat, city, dist, fullLoc, amt, unit)) {
                    lblMsg.setText("Başarılı!"); lblMsg.setTextFill(Color.GREEN);
                    txtAmount.clear(); refreshTable();
                } else {
                    lblMsg.setText("Hata!"); lblMsg.setTextFill(Color.RED);
                }
            } catch (Exception ex) { ex.printStackTrace(); lblMsg.setText("Hata!"); }
        });

        Button btnReport = new Button("RAPORU GÖR 🌍");
        styleInfoButton(btnReport);
        btnReport.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Rapor"); a.setHeaderText("Etki Raporu");
            // BUG FİX: Artık SQL tarafında fonksiyon var, burası çalışacak
            a.setContentText(userDAO.getImpactReport(userEmail));
            a.getDialogPane().setMinHeight(250); // Rapor uzun olabilir, pencereyi büyütelim
            a.showAndWait();
        });

        cardContent.getChildren().addAll(lblTitle, cmbSavedAddresses,
                txtStreet, rowCityDist, cmbNeigh, rowBuildInfo, txtDirections,
                chkSave, txtAddrTitle, sep,
                new Label("Atık Detayı:"), cmbCategory, amountBox, btnAdd, lblMsg, new Separator(), btnReport);

        ScrollPane scrollPane = new ScrollPane(cardContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox cardContainer = new VBox(scrollPane);
        styleCard(cardContainer);
        cardContainer.setPrefWidth(360);
        return cardContainer;
    }

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