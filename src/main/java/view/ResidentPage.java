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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

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

    private ComboBox<String> cmbCity, cmbDistrict, cmbNeigh, cmbSavedAddresses;
    private ComboBox<String> cmbCategory, cmbUnit;
    private TextField txtStreet, txtBuildNo, txtFloor, txtDoor, txtAddrTitle, txtAmount;
    private TextArea txtDirections;
    private CheckBox chkSave;

    public ResidentPage(String email) {
        this.userEmail = email;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Sakin Paneli");

        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        stage.getIcons().add(new Image("file:logo4.png"));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());

        BorderPane contentArea = new BorderPane();
        contentArea.setPadding(new Insets(20));

        VBox leftCard = createFormCard();
        contentArea.setLeft(leftCard);
        BorderPane.setMargin(leftCard, new Insets(0, 20, 0, 0));

        VBox rightCard = createLeaderboardCard();
        contentArea.setRight(rightCard);
        BorderPane.setMargin(rightCard, new Insets(0, 0, 0, 20));

        VBox centerCard = createTableCard();
        contentArea.setCenter(centerCard);

        mainLayout.setCenter(contentArea);
        rootPane.getChildren().add(mainLayout);

        refreshTable();
        refreshLeaderboard();

        Scene scene = new Scene(rootPane, 1360, 800);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 40, 15, 40));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15);
        header.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        ImageView logoView = new ImageView();
        try {
            logoView.setImage(new Image("file:logo4.png"));
            logoView.setFitHeight(50);
            logoView.setPreserveRatio(true);
        } catch (Exception e) {}

        VBox titleBox = new VBox(2);
        Label lblBrand = new Label("RecycleShare");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblBrand.setTextFill(Color.web("#2E7D32"));

        Label lblUser = new Label("Hoş geldin, " + userEmail);
        lblUser.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        lblUser.setTextFill(Color.web("#757575"));

        titleBox.getChildren().addAll(lblBrand, lblUser);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("Güvenli Çıkış");
        btnLogout.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15;");
        btnLogout.setOnAction(e -> {
            stage.close();
            try { new LoginApp().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
        });

        header.getChildren().addAll(logoView, titleBox, spacer, btnLogout);
        return header;
    }

    private VBox createFormCard() {
        VBox mainCard = new VBox(15);
        mainCard.setPadding(new Insets(25));
        mainCard.setPrefWidth(380);
        styleCard(mainCard);

        Label lblTitle = new Label("Yeni Atık Bildirimi");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        VBox content = new VBox(12);

        cmbSavedAddresses = new ComboBox<>();
        cmbSavedAddresses.setMaxWidth(Double.MAX_VALUE);
        refreshAddressCombo();

        Button btnDeleteAddr = new Button("Sil");
        btnDeleteAddr.setStyle("-fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-cursor: hand;");
        btnDeleteAddr.setDisable(true);

        HBox addrBox = new HBox(10, cmbSavedAddresses, btnDeleteAddr);
        HBox.setHgrow(cmbSavedAddresses, Priority.ALWAYS);

        txtStreet = new TextField(); txtStreet.setPromptText("Cadde / Sokak"); styleField(txtStreet);
        cmbCity = new ComboBox<>(); cmbCity.setPromptText("İl"); cmbCity.setMaxWidth(Double.MAX_VALUE);
        cmbCity.getItems().addAll(locationDAO.getAllCities());

        cmbDistrict = new ComboBox<>(); cmbDistrict.setPromptText("İlçe"); cmbDistrict.setDisable(true); cmbDistrict.setMaxWidth(Double.MAX_VALUE);
        cmbNeigh = new ComboBox<>(); cmbNeigh.setPromptText("Mahalle"); cmbNeigh.setDisable(true); cmbNeigh.setMaxWidth(Double.MAX_VALUE);

        HBox rowLoc = new HBox(10, cmbCity, cmbDistrict);
        HBox.setHgrow(cmbCity, Priority.ALWAYS); HBox.setHgrow(cmbDistrict, Priority.ALWAYS);

        txtBuildNo = new TextField(); txtBuildNo.setPromptText("Bina"); styleField(txtBuildNo);
        txtFloor = new TextField(); txtFloor.setPromptText("Kat"); styleField(txtFloor);
        txtDoor = new TextField(); txtDoor.setPromptText("Daire"); styleField(txtDoor);
        HBox rowBuild = new HBox(10, txtBuildNo, txtFloor, txtDoor);

        txtDirections = new TextArea(); txtDirections.setPromptText("Adres Tarifi"); txtDirections.setPrefRowCount(2);
        chkSave = new CheckBox("Bu adresi kaydet");
        txtAddrTitle = new TextField(); txtAddrTitle.setPromptText("Adres Başlığı"); styleField(txtAddrTitle);
        txtAddrTitle.setVisible(false); txtAddrTitle.managedProperty().bind(txtAddrTitle.visibleProperty());

        cmbCategory = new ComboBox<>(); cmbCategory.setPromptText("Kategori Seçiniz");
        try { cmbCategory.getItems().addAll(wasteDAO.getCategories()); } catch (Exception ex) {}

        txtAmount = new TextField(); txtAmount.setPromptText("Miktar"); styleField(txtAmount);
        cmbUnit = new ComboBox<>(); cmbUnit.setPromptText("Birim"); cmbUnit.setPrefWidth(100);
        HBox rowAmt = new HBox(10, txtAmount, cmbUnit);

        Button btnAdd = new Button("SİPARİŞ OLUŞTUR"); stylePrimaryButton(btnAdd);
        lblMsg = new Label();
        Button btnReport = new Button("ETKİ RAPORUNU GÖRÜNTÜLE"); styleSecondaryButton(btnReport);

        setupFormEvents(btnAdd, btnDeleteAddr, btnReport);

        content.getChildren().addAll(new Label("Teslimat Adresi"), addrBox, txtStreet, rowLoc, cmbNeigh, rowBuild, txtDirections, chkSave, txtAddrTitle,
                new Separator(), new Label("Atık Bilgileri"), cmbCategory, rowAmt, btnAdd, lblMsg, btnReport);

        mainCard.getChildren().addAll(lblTitle, content);
        return mainCard;
    }

    private void setupFormEvents(Button btnAdd, Button btnDelete, Button btnReport) {
        cmbSavedAddresses.setOnAction(e -> {
            String sel = cmbSavedAddresses.getValue();
            if (sel == null) return;
            if (sel.equals("✨ Yeni Adres Oluştur...")) { clearAddressFields(); btnDelete.setDisable(true); }
            else {
                AddressDAO.AddressDetails d = addressDAO.getAddressDetails(userEmail, sel);
                if (d != null) { fillForm(d); btnDelete.setDisable(false); chkSave.setSelected(false); }
            }
        });

        btnDelete.setOnAction(e -> {
            if (addressDAO.deleteAddress(userEmail, cmbSavedAddresses.getValue())) { refreshAddressCombo(); clearAddressFields(); }
        });

        chkSave.selectedProperty().addListener((obs, o, n) -> txtAddrTitle.setVisible(n));

        cmbCity.setOnAction(e -> {
            if (cmbCity.getValue() != null) { cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(cmbCity.getValue())); cmbDistrict.setDisable(false); }
        });

        cmbDistrict.setOnAction(e -> {
            if (cmbDistrict.getValue() != null) { cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(cmbDistrict.getValue())); cmbNeigh.setDisable(false); }
        });

        cmbCategory.setOnAction(e -> {
            String sel = cmbCategory.getValue();
            if (sel != null) {
                cmbUnit.getItems().clear();
                if (sel.equals("Bitkisel Yağ")) cmbUnit.getItems().addAll("LITRE", "KG");
                else if (sel.matches("Cam Şişe|Metal Kutu|Atık Pil|Lastik|Beyaz Eşya")) cmbUnit.getItems().addAll("ADET", "KG");
                else cmbUnit.getItems().add("KG");
                cmbUnit.getSelectionModel().selectFirst();
            }
        });

        btnAdd.setOnAction(e -> handleAddWaste());
        btnReport.setOnAction(e -> showCustomReportDialog());
    }

    private void showCustomReportDialog() {
        String rawReport = userDAO.getImpactReport(userEmail);
        String formattedReport = rawReport.replace("\\n", "\n");

        Stage reportStage = new Stage();
        reportStage.initModality(Modality.APPLICATION_MODAL);
        reportStage.setTitle("Kişisel Etki Raporu");

        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: white; -fx-border-color: #2E7D32; -fx-border-width: 2;");

        VBox header = new VBox(5);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #2E7D32;");
        Label lblT = new Label("Geri Dönüşüm Karnesi 🌱");
        lblT.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblT.setTextFill(Color.WHITE);
        header.getChildren().add(lblT);

        TextArea txtReport = new TextArea(formattedReport);
        txtReport.setEditable(false);
        txtReport.setFont(Font.font("Consolas", 14));
        txtReport.setStyle("-fx-control-inner-background: #FAFAFA; -fx-padding: 10;");

        Button btnClose = new Button("Kapat ✖");
        btnClose.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold;");
        btnClose.setOnAction(e -> reportStage.close());

        layout.setTop(header);
        layout.setCenter(txtReport);
        HBox f = new HBox(btnClose); f.setPadding(new Insets(10)); f.setAlignment(Pos.CENTER_RIGHT);
        layout.setBottom(f);

        reportStage.setScene(new Scene(layout, 550, 650));
        layout.setEffect(new DropShadow(15, Color.BLACK));
        reportStage.show();
    }

    private void handleAddWaste() {
        try {
            String cat = cmbCategory.getValue();
            String city = cmbCity.getValue();
            String dist = cmbDistrict.getValue();
            String neigh = cmbNeigh.getValue();
            String fullLoc = String.format("%s Mah. %s No:%s D:%s %s/%s", neigh, txtStreet.getText(), txtBuildNo.getText(), txtDoor.getText(), dist, city);

            if (chkSave.isSelected() && !txtAddrTitle.getText().isEmpty()) {
                addressDAO.saveAddress(userEmail, txtAddrTitle.getText(), city, dist, neigh, txtStreet.getText(), txtBuildNo.getText(), txtFloor.getText(), txtDoor.getText(), txtDirections.getText());
                refreshAddressCombo();
            }

            if (wasteDAO.addWaste(userEmail, cat, city, dist, fullLoc, Double.parseDouble(txtAmount.getText()), cmbUnit.getValue())) {
                lblMsg.setText("Başarılı!"); lblMsg.setTextFill(Color.GREEN); refreshTable();
            }
        } catch (Exception ex) { lblMsg.setText("Hata: Bilgileri kontrol edin."); lblMsg.setTextFill(Color.RED); }
    }

    private VBox createTableCard() {
        VBox card = new VBox(15); card.setPadding(new Insets(25)); styleCard(card);
        Label lbl = new Label("Geçmiş İşlemlerim"); lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableColumn<Waste, String> c1 = new TableColumn<>("Kategori"); c1.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Waste, String> c2 = new TableColumn<>("Bölge"); c2.setCellValueFactory(new PropertyValueFactory<>("district"));
        TableColumn<Waste, Double> c3 = new TableColumn<>("Miktar"); c3.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Waste, String> c4 = new TableColumn<>("Birim"); c4.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<Waste, String> c5 = new TableColumn<>("Durum"); c5.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<Waste, String> c6 = new TableColumn<>("Tarih"); c6.setCellValueFactory(new PropertyValueFactory<>("dateInfo"));

        table.getColumns().addAll(c6, c1, c2, c3, c4, c5);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        card.getChildren().addAll(lbl, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return card;
    }

    private VBox createLeaderboardCard() {
        VBox card = new VBox(15); card.setPadding(new Insets(25)); card.setPrefWidth(300);
        card.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 15;");

        Label lbl = new Label("🏆 Liderlik Tablosu");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); lbl.setTextFill(Color.web("#E65100"));

        TableColumn<UserDAO.UserScore, String> c1 = new TableColumn<>("İsim");
        c1.setMinWidth(200);

        // --- YILDIZLI EMOJİ KULLANIMI ---
        c1.setCellFactory(column -> new TableCell<UserDAO.UserScore, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    UserDAO.UserScore user = getTableRow().getItem();
                    HBox box = new HBox(5);
                    box.setAlignment(Pos.CENTER_LEFT);

                    // Kullanıcı ismini ekle
                    Label lblName = new Label(user.getName());
                    box.getChildren().add(lblName);

                    // Eğer 5+ işlemi varsa yıldız emojisi ekle
                    if (user.isHasStar()) {
                        Label lblStar = new Label("⭐"); // Emoji burada metin olarak ekleniyor
                        lblStar.setStyle("-fx-font-size: 16px;"); // Emojinin boyutunu buradan ayarlayabilirsin
                        box.getChildren().add(lblStar);
                    }

                    setGraphic(box);
                }
            }
        });

        TableColumn<UserDAO.UserScore, Double> c2 = new TableColumn<>("Puan");
        c2.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableTop.getColumns().addAll(c1, c2);
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableTop, Priority.ALWAYS);
        card.getChildren().addAll(lbl, tableTop);
        return card;
    }

    private void refreshAddressCombo() {
        cmbSavedAddresses.getItems().setAll("✨ Yeni Adres Oluştur...");
        cmbSavedAddresses.getItems().addAll(addressDAO.getUserAddressTitles(userEmail));
        cmbSavedAddresses.getSelectionModel().selectFirst();
    }

    private void clearAddressFields() {
        txtStreet.clear(); txtBuildNo.clear(); txtFloor.clear(); txtDoor.clear(); txtDirections.clear(); txtAddrTitle.clear();
        chkSave.setSelected(false); cmbCity.getSelectionModel().clearSelection(); cmbDistrict.setDisable(true);
    }

    private void fillForm(AddressDAO.AddressDetails d) {
        cmbCity.setValue(d.city); cmbDistrict.setValue(d.district); cmbNeigh.setValue(d.neighborhood);
        txtStreet.setText(d.street); txtBuildNo.setText(d.buildingNo); txtFloor.setText(d.floorNo); txtDoor.setText(d.doorNo); txtDirections.setText(d.directions);
    }

    private void refreshTable() { table.setItems(FXCollections.observableArrayList(wasteDAO.getMyWastes(userEmail))); }

    private void refreshLeaderboard() {
        // UserDAO'da güncellediğimiz metodu çağırıyoruz
        tableTop.setItems(FXCollections.observableArrayList(userDAO.getTopUsersWithStars()));
    }

    private void styleCard(VBox b) { b.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 4);"); }
    private void styleField(TextInputControl t) { t.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10;"); }
    private void stylePrimaryButton(Button b) { b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;"); b.setMaxWidth(Double.MAX_VALUE); b.setPadding(new Insets(12)); }
    private void styleSecondaryButton(Button b) { b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;"); b.setMaxWidth(Double.MAX_VALUE); b.setPadding(new Insets(12)); }
}