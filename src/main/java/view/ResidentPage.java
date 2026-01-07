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
import javafx.stage.StageStyle;
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

    // --- SINIF DEĞİŞKENLERİ ---
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
        // Yeşil Gradyan Arka Plan
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

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
        header.setSpacing(15); // Logo ile yazı arasına boşluk
        header.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // 1. KÜÇÜK LOGO
        ImageView logoView = new ImageView();
        try {
            logoView.setImage(new Image("file:logo4.png"));
            logoView.setFitHeight(50); // Yüksekliği header'a uyduruyoruz
            logoView.setPreserveRatio(true);
        } catch (Exception e) {
        }

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
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle("-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15;"));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 8 15;"));

        btnLogout.setOnAction(e -> {
            stage.close();
            try {
                new LoginApp().start(new Stage());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // Header'a logoyu en başa ekliyoruz
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
        lblTitle.setTextFill(Color.web("#333"));

        VBox content = new VBox(12);

        Label lblAddr = new Label("Teslimat Adresi");
        lblAddr.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        cmbSavedAddresses = new ComboBox<>();
        cmbSavedAddresses.setMaxWidth(Double.MAX_VALUE);
        refreshAddressCombo();

        Button btnDeleteAddr = new Button("Sil");
        btnDeleteAddr.setStyle("-fx-background-color: transparent; -fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-cursor: hand;");
        btnDeleteAddr.setDisable(true);

        HBox addrBox = new HBox(10, cmbSavedAddresses, btnDeleteAddr);
        HBox.setHgrow(cmbSavedAddresses, Priority.ALWAYS);

        txtStreet = new TextField();
        txtStreet.setPromptText("Cadde / Sokak");
        styleField(txtStreet);

        cmbCity = new ComboBox<>();
        cmbCity.setPromptText("İl");
        cmbCity.setMaxWidth(Double.MAX_VALUE);
        cmbCity.getItems().addAll(locationDAO.getAllCities());

        cmbDistrict = new ComboBox<>();
        cmbDistrict.setPromptText("İlçe");
        cmbDistrict.setMaxWidth(Double.MAX_VALUE);
        cmbDistrict.setDisable(true);
        cmbNeigh = new ComboBox<>();
        cmbNeigh.setPromptText("Mahalle");
        cmbNeigh.setMaxWidth(Double.MAX_VALUE);
        cmbNeigh.setDisable(true);

        HBox rowLoc = new HBox(10, cmbCity, cmbDistrict);
        HBox.setHgrow(cmbCity, Priority.ALWAYS);
        HBox.setHgrow(cmbDistrict, Priority.ALWAYS);

        txtBuildNo = new TextField();
        txtBuildNo.setPromptText("Bina");
        styleField(txtBuildNo);
        txtFloor = new TextField();
        txtFloor.setPromptText("Kat");
        styleField(txtFloor);
        txtDoor = new TextField();
        txtDoor.setPromptText("Daire");
        styleField(txtDoor);
        HBox rowBuild = new HBox(10, txtBuildNo, txtFloor, txtDoor);

        txtDirections = new TextArea();
        txtDirections.setPromptText("Adres Tarifi (İsteğe bağlı)");
        txtDirections.setPrefRowCount(2);
        txtDirections.setStyle("-fx-control-inner-background: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5;");

        chkSave = new CheckBox("Bu adresi kaydet");
        chkSave.setStyle("-fx-text-fill: #333; -fx-font-size: 13px; -fx-font-weight: bold;");

        txtAddrTitle = new TextField();
        txtAddrTitle.setPromptText("Adres Başlığı (Örn: Evim)");
        styleField(txtAddrTitle);
        txtAddrTitle.setVisible(false);
        txtAddrTitle.managedProperty().bind(txtAddrTitle.visibleProperty());

        Separator sep = new Separator();
        Label lblWaste = new Label("Atık Bilgileri");
        lblWaste.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        cmbCategory = new ComboBox<>();
        cmbCategory.setPromptText("Kategori Seçiniz");
        try {
            cmbCategory.getItems().addAll(wasteDAO.getCategories());
        } catch (Exception ex) {
        }
        styleComboBox(cmbCategory);

        txtAmount = new TextField();
        txtAmount.setPromptText("Miktar");
        styleField(txtAmount);

        cmbUnit = new ComboBox<>();
        cmbUnit.setPromptText("Birim");
        styleComboBox(cmbUnit);
        cmbUnit.setPrefWidth(100);

        HBox rowAmt = new HBox(10, txtAmount, cmbUnit);
        HBox.setHgrow(txtAmount, Priority.ALWAYS);

        Button btnAdd = new Button("SİPARİŞ OLUŞTUR");
        stylePrimaryButton(btnAdd);

        lblMsg = new Label();
        lblMsg.setWrapText(true);
        lblMsg.setStyle("-fx-font-size: 12px;");

        Button btnReport = new Button("ETKİ RAPORUNU GÖRÜNTÜLE");
        styleSecondaryButton(btnReport);

        setupFormEvents(btnAdd, btnDeleteAddr, btnReport);

        content.getChildren().addAll(
                lblAddr, addrBox,
                txtStreet, rowLoc, cmbNeigh, rowBuild, txtDirections, chkSave, txtAddrTitle,
                sep, lblWaste, cmbCategory, rowAmt,
                new Region(), btnAdd, lblMsg, btnReport
        );

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        mainCard.getChildren().addAll(lblTitle, scroll);
        return mainCard;
    }

    private void setupFormEvents(Button btnAdd, Button btnDelete, Button btnReport) {
        cmbSavedAddresses.setOnAction(e -> {
            String selected = cmbSavedAddresses.getValue();
            if (selected == null) return;
            if (selected.equals("✨ Yeni Adres Oluştur...")) {
                clearAddressFields();
                btnDelete.setDisable(true);
            } else {
                AddressDAO.AddressDetails d = addressDAO.getAddressDetails(userEmail, selected);
                if (d != null) {
                    fillForm(d);
                    btnDelete.setDisable(false);
                    chkSave.setSelected(false);
                }
            }
        });

        btnDelete.setOnAction(e -> {
            String title = cmbSavedAddresses.getValue();
            if (title != null && !title.startsWith("✨")) {
                if (addressDAO.deleteAddress(userEmail, title)) {
                    refreshAddressCombo();
                    clearAddressFields();
                }
            }
        });

        chkSave.selectedProperty().addListener((obs, o, n) -> txtAddrTitle.setVisible(n));

        cmbCity.setOnAction(e -> {
            if (cmbCity.getValue() != null) {
                cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(cmbCity.getValue()));
                cmbDistrict.setDisable(false);
                cmbNeigh.getItems().clear();
                cmbNeigh.setDisable(true);
            }
        });

        cmbDistrict.setOnAction(e -> {
            if (cmbDistrict.getValue() != null) {
                cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(cmbDistrict.getValue()));
                cmbNeigh.setDisable(false);
            }
        });

        cmbCategory.setOnAction(e -> {
            String sel = cmbCategory.getValue();
            if (sel != null) {
                cmbUnit.getItems().clear();
                switch (sel) {
                    case "Bitkisel Yağ":
                        cmbUnit.getItems().addAll("LITRE", "KG");
                        cmbUnit.setValue("LITRE");
                        break;
                    case "Cam Şişe":
                    case "Metal Kutu":
                    case "Atık Pil":
                    case "Lastik":
                    case "Beyaz Eşya":
                        cmbUnit.getItems().addAll("ADET", "KG");
                        cmbUnit.setValue("ADET");
                        break;
                    default:
                        cmbUnit.getItems().addAll("KG");
                        cmbUnit.setValue("KG");
                        break;
                }
            }
        });

        btnAdd.setOnAction(e -> handleAddWaste());

        // DEĞİŞİKLİK: Alert yerine Özel Tasarım Pencereyi Çağırıyoruz
        btnReport.setOnAction(e -> showCustomReportDialog());
    }

    // --- YENİ EKLENEN ÖZEL RAPOR PENCERESİ METODU ---

    private void showCustomReportDialog() {
        // 1. Veriyi Çek ve FORMATLA (Bug Fix: \\n -> \n)
        String rawReport = userDAO.getImpactReport(userEmail);
        // Veritabanından gelen kaçış karakterlerini gerçek satır başlarına çeviriyoruz
        String formattedReport = rawReport.replace("\\n", "\n");

        // Yeni Pencere (Stage)
        Stage reportStage = new Stage();
        reportStage.initModality(Modality.APPLICATION_MODAL);
        reportStage.initOwner(stage);
        reportStage.setTitle("Kişisel Etki Raporu");

        // Ana Düzen (BorderPane)
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: white; -fx-border-color: #2E7D32; -fx-border-width: 2;");

        // --- A. HEADER (BAŞLIK) ---
        HBox header = new HBox(15); // İkon ve yazı arası boşluk
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(25));
        // Yeşil Gradyan Arka Plan
        header.setStyle("-fx-background-color: linear-gradient(to right, #2E7D32, #43A047);");

        // Başlık İkonu (Emoji kullanarak basit çözüm)
        Label lblIcon = new Label("🌱");
        lblIcon.setFont(Font.font("Segoe UI Emoji", 40));
        lblIcon.setTextFill(Color.WHITE);

        // Başlık Metinleri
        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Geri Dönüşüm Karnesi");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setTextFill(Color.WHITE);

        Label lblSubTitle = new Label("Dünyaya katkılarınızın özeti");
        lblSubTitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        lblSubTitle.setTextFill(Color.web("#E8F5E9")); // Açık yeşilimsi beyaz

        titleBox.getChildren().addAll(lblTitle, lblSubTitle);
        header.getChildren().addAll(lblIcon, titleBox);

        // --- B. İÇERİK (RAPOR METNİ) ---
        // TextArea kullanıyoruz ama CSS ile "Kağıt" gibi gösteriyoruz
        TextArea txtReport = new TextArea(formattedReport);
        txtReport.setEditable(false);
        txtReport.setWrapText(true);
        // Monospaced font kullanarak tablo hizalamalarını düzgün gösteriyoruz
        txtReport.setFont(Font.font("Consolas", FontWeight.NORMAL, 14));

        // CSS Makyajı: Kenarlıkları kaldır, arka planı kağıt rengi yap, dolgu ekle
        txtReport.setStyle(
                "-fx-control-inner-background: #FAFAFA; " + // Hafif kırık beyaz (Kağıt rengi)
                        "-fx-background-color: transparent; " +
                        "-fx-border-color: transparent; " +
                        "-fx-focus-color: transparent; " +
                        "-fx-faint-focus-color: transparent; " +
                        "-fx-padding: 10px;" // İçeriden boşluk
        );

        // İçeriği ortalayıp kenarlardan boşluk verelim
        VBox contentBox = new VBox(txtReport);
        contentBox.setPadding(new Insets(20));
        contentBox.setStyle("-fx-background-color: white;");
        VBox.setVgrow(txtReport, Priority.ALWAYS);

        // --- C. FOOTER (ALT KISIM) ---
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(15, 25, 15, 25));
        footer.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #E0E0E0; -fx-border-width: 1 0 0 0;");

        Button btnClose = new Button("Kapat ✖");
        // Kırmızı yerine daha modern bir gri/siyah veya koyu yeşil tonu da olabilir ama isteğin üzerine kırmızıya yakın duralım
        btnClose.setStyle(
                "-fx-background-color: #D32F2F; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 20; " + // Yuvarlak buton
                        "-fx-cursor: hand; " +
                        "-fx-padding: 8 20;"
        );

        // Hover Efekti
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 8 20;"));

        btnClose.setOnAction(e -> reportStage.close());
        footer.getChildren().add(btnClose);

        // Yerleştirme
        layout.setTop(header);
        layout.setCenter(contentBox);
        layout.setBottom(footer);

        // Sahne Ayarı (Biraz daha geniş)
        Scene scene = new Scene(layout, 600, 700);
        reportStage.setScene(scene);

        // DropShadow (Gölgelendirme) efekti ekleyelim ki pop-up olduğu belli olsun
        layout.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.4)));

        reportStage.showAndWait();
    }

    private void handleAddWaste() {
        try {
            String city = cmbCity.getValue();
            String dist = cmbDistrict.getValue();
            String neigh = cmbNeigh.getValue();
            String street = txtStreet.getText();
            String build = txtBuildNo.getText();
            String door = txtDoor.getText();
            String cat = cmbCategory.getValue();
            String unit = cmbUnit.getValue();

            if (city == null || dist == null || neigh == null || street.isEmpty() || build.isEmpty() || door.isEmpty() || cat == null || unit == null || txtAmount.getText().isEmpty()) {
                lblMsg.setText("Lütfen zorunlu alanları doldurun.");
                lblMsg.setTextFill(Color.RED);
                return;
            }

            if (chkSave.isSelected() && !txtAddrTitle.getText().isEmpty()) {
                addressDAO.saveAddress(userEmail, txtAddrTitle.getText(), city, dist, neigh, street, build, txtFloor.getText(), door, txtDirections.getText());
                refreshAddressCombo();
            }

            String fullLoc = String.format("%s Mah. %s No:%s D:%s %s/%s", neigh, street, build, door, dist, city);

            if (wasteDAO.addWaste(userEmail, cat, city, dist, fullLoc, Double.parseDouble(txtAmount.getText()), unit)) {
                lblMsg.setText("Başarılı!");
                lblMsg.setTextFill(Color.GREEN);
                refreshTable();
                txtAmount.clear();
            } else {
                lblMsg.setText("Hata oluştu.");
                lblMsg.setTextFill(Color.RED);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            lblMsg.setText("Hata: Miktar sayı olmalı.");
            lblMsg.setTextFill(Color.RED);
        }
    }

    private VBox createTableCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        styleCard(card);
        VBox.setVgrow(card, Priority.ALWAYS);

        Label lbl = new Label("Geçmiş İşlemlerim");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TableColumn<Waste, String> c1 = new TableColumn<>("Kategori");
        c1.setCellValueFactory(new PropertyValueFactory<>("category"));
        c1.setReorderable(false);
        c1.setResizable(false);

        TableColumn<Waste, String> c2 = new TableColumn<>("Bölge");
        c2.setCellValueFactory(new PropertyValueFactory<>("district"));
        c2.setReorderable(false);
        c2.setResizable(false);

        TableColumn<Waste, Double> c3 = new TableColumn<>("Miktar");
        c3.setCellValueFactory(new PropertyValueFactory<>("amount"));
        c3.setReorderable(false);
        c3.setResizable(false);

        TableColumn<Waste, String> c4 = new TableColumn<>("Birim");
        c4.setCellValueFactory(new PropertyValueFactory<>("unit"));
        c4.setReorderable(false);
        c4.setResizable(false);

        TableColumn<Waste, String> c5 = new TableColumn<>("Durum");
        c5.setCellValueFactory(new PropertyValueFactory<>("status"));
        c5.setReorderable(false);
        c5.setResizable(false);
        c5.setMinWidth(120);

        TableColumn<Waste, String> cDate = new TableColumn<>("Tarih");
        cDate.setCellValueFactory(new PropertyValueFactory<>("dateInfo"));
        cDate.setReorderable(false);
        cDate.setResizable(false);
        cDate.setMinWidth(130);

        table.getColumns().addAll(cDate, c1, c2, c3, c4, c5);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-base: #fff; -fx-font-size: 13px;");

        VBox.setVgrow(table, Priority.ALWAYS);
        card.getChildren().addAll(lbl, table);
        return card;
    }

    private VBox createLeaderboardCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(25));
        card.setPrefWidth(280);
        styleCard(card);
        card.setStyle("-fx-background-color: #FFF3E0; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Label lbl = new Label("🏆 Liderlik Tablosu");
        lbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lbl.setTextFill(Color.web("#E65100"));

        TableColumn<UserDAO.UserScore, String> c1 = new TableColumn<>("İsim");
        c1.setCellValueFactory(new PropertyValueFactory<>("name"));
        c1.setReorderable(false);
        c1.setResizable(false);

        TableColumn<UserDAO.UserScore, Double> c2 = new TableColumn<>("Puan");
        c2.setCellValueFactory(new PropertyValueFactory<>("score"));
        c2.setReorderable(false);
        c2.setResizable(false);
        c2.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableTop.getColumns().addAll(c1, c2);
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableTop.setStyle("-fx-background-color: transparent;");

        VBox.setVgrow(tableTop, Priority.ALWAYS);
        card.getChildren().addAll(lbl, tableTop);
        return card;
    }

    private void refreshAddressCombo() {
        cmbSavedAddresses.getItems().clear();
        cmbSavedAddresses.getItems().add("✨ Yeni Adres Oluştur...");
        cmbSavedAddresses.getItems().addAll(addressDAO.getUserAddressTitles(userEmail));
        cmbSavedAddresses.getSelectionModel().selectFirst();
    }

    private void clearAddressFields() {
        txtStreet.clear();
        txtBuildNo.clear();
        txtFloor.clear();
        txtDoor.clear();
        txtDirections.clear();
        txtAddrTitle.clear();
        chkSave.setSelected(false);
        cmbCity.getSelectionModel().clearSelection();
        cmbDistrict.getItems().clear();
        cmbDistrict.setDisable(true);
        cmbNeigh.getItems().clear();
        cmbNeigh.setDisable(true);
    }

    private void fillForm(AddressDAO.AddressDetails d) {
        cmbCity.setValue(d.city);
        cmbDistrict.getItems().setAll(locationDAO.getDistrictsByCity(d.city));
        cmbDistrict.setValue(d.district);
        cmbDistrict.setDisable(false);
        cmbNeigh.getItems().setAll(locationDAO.getNeighborhoodsByDistrict(d.district));
        cmbNeigh.setValue(d.neighborhood);
        cmbNeigh.setDisable(false);
        txtStreet.setText(d.street);
        txtBuildNo.setText(d.buildingNo);
        txtFloor.setText(d.floorNo);
        txtDoor.setText(d.doorNo);
        txtDirections.setText(d.directions);
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(wasteDAO.getMyWastes(userEmail)));
    }

    private void refreshLeaderboard() {
        tableTop.setItems(FXCollections.observableArrayList(userDAO.getTopUsers()));
    }

    private void styleCard(VBox b) {
        b.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 4);");
    }

    private void styleField(TextInputControl t) {
        t.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10;");
    }

    private void styleComboBox(ComboBox<?> c) {
        c.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 2;");
    }

    private void stylePrimaryButton(Button b) {
        b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(12));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;"));
    }

    private void styleSecondaryButton(Button b) {
        b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(12));
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 6; -fx-cursor: hand;"));
    }
}