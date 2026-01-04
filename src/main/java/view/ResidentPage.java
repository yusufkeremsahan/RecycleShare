package view;

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
import javafx.stage.Stage;

public class ResidentPage {

    private String username;
    private WasteDAO wasteDAO = new WasteDAO();
    private UserDAO userDAO = new UserDAO();

    private TableView<Waste> table = new TableView<>();
    private TableView<UserDAO.UserScore> tableTop = new TableView<>();

    private Stage stage;

    // UI Referansları
    private Label lblMsg; // Mesajları göstermek için

    public ResidentPage(String username) {
        this.username = username;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Sakin Paneli (" + username + ")");

        // 1. ARKA PLAN (Yeşil Gradyan)
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        // 2. ANA DÜZEN
        BorderPane mainLayout = new BorderPane();

        // --- ÜST BAR (HEADER) ---
        mainLayout.setTop(createHeader());

        // --- İÇERİK ALANI (3 Kartlı Yapı) ---
        // Sol: Form, Orta: Tablo, Sağ: Liderlik
        BorderPane contentArea = new BorderPane();
        contentArea.setPadding(new Insets(20));

        // Sol Panel (Form)
        VBox leftCard = createFormCard();
        contentArea.setLeft(leftCard);
        BorderPane.setMargin(leftCard, new Insets(0, 15, 0, 0)); // Sağdan boşluk

        // Sağ Panel (Liderlik)
        VBox rightCard = createLeaderboardCard();
        contentArea.setRight(rightCard);
        BorderPane.setMargin(rightCard, new Insets(0, 0, 0, 15)); // Soldan boşluk

        // Orta Panel (Atık Tablosu)
        VBox centerCard = createTableCard();
        contentArea.setCenter(centerCard);

        mainLayout.setCenter(contentArea);
        rootPane.getChildren().add(mainLayout);

        // --- BAŞLANGIÇ VERİLERİ ---
        refreshTable();
        refreshLeaderboard();

        Scene scene = new Scene(rootPane, 1100, 650); // Geniş bir ekran
        stage.setScene(scene);
        stage.show();
    }

    // ==========================================
    // 1. HEADER (ÜST BAR)
    // ==========================================
    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95); -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        VBox titleBox = new VBox(2);
        Label lblBrand = new Label("RecycleShare");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        lblBrand.setTextFill(Color.web("#2E7D32"));

        Label lblUser = new Label("Sakin: " + username);
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

    // ==========================================
    // 2. SOL KART: ATIK EKLEME FORMU
    // ==========================================
    private VBox createFormCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        styleCard(card);

        Label lblTitle = new Label("Atık Bildir ♻️");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblTitle.setTextFill(Color.web("#2E7D32"));

        // Kategori
        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.setPromptText("Kategori Seç");
        cmbCategory.getItems().addAll(wasteDAO.getCategories());
        styleComboBox(cmbCategory);

        // Mahalle
        TextField txtDistrict = new TextField();
        txtDistrict.setPromptText("Mahalle (Örn: Konak)");
        styleField(txtDistrict);

        // Miktar ve Birim (Yan Yana)
        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Miktar");
        styleField(txtAmount);

        ComboBox<String> cmbUnit = new ComboBox<>();
        cmbUnit.getItems().addAll("KG", "ADET", "LITRE", "M2");
        cmbUnit.setPromptText("Birim");
        cmbUnit.getSelectionModel().selectFirst();
        styleComboBox(cmbUnit);
        cmbUnit.setPrefWidth(100);

        HBox amountBox = new HBox(10);
        amountBox.getChildren().addAll(txtAmount, cmbUnit);
        HBox.setHgrow(txtAmount, Priority.ALWAYS);

        // Ekle Butonu
        Button btnAdd = new Button("LİSTEYE EKLE ➕");
        stylePrimaryButton(btnAdd);

        lblMsg = new Label();
        lblMsg.setWrapText(true);

        // Ekleme Aksiyonu
        btnAdd.setOnAction(e -> {
            try {
                String cat = cmbCategory.getValue();
                String dist = txtDistrict.getText();
                double amount = Double.parseDouble(txtAmount.getText());
                String unit = cmbUnit.getValue();

                if (cat == null || dist.isEmpty()) {
                    lblMsg.setText("Lütfen tüm alanları doldurun.");
                    lblMsg.setTextFill(Color.RED);
                    return;
                }

                if (wasteDAO.addWaste(username, cat, dist, amount, unit)) {
                    lblMsg.setText("Başarıyla Eklendi!");
                    lblMsg.setTextFill(Color.GREEN);
                    txtDistrict.clear(); txtAmount.clear();
                    refreshTable();
                } else {
                    lblMsg.setText("Hata oluştu.");
                    lblMsg.setTextFill(Color.RED);
                }
            } catch (NumberFormatException ex) {
                lblMsg.setText("Miktar sayı olmalı!");
                lblMsg.setTextFill(Color.RED);
            } catch (Exception ex) {
                lblMsg.setText("Hatalı giriş!");
                lblMsg.setTextFill(Color.RED);
            }
        });

        Separator sep = new Separator();

        // Rapor Butonu
        Button btnReport = new Button("ETKİ RAPORUMU GÖR 🌍");
        styleInfoButton(btnReport);
        btnReport.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Kişisel Etki Raporu");
            alert.setHeaderText("Tebrikler " + username + "!");
            alert.setContentText(userDAO.getImpactReport(username));
            alert.getDialogPane().setMinHeight(200);
            alert.showAndWait();
        });

        card.getChildren().addAll(lblTitle,
                new Label("Atık Türü:"), cmbCategory,
                new Label("Adres/Mahalle:"), txtDistrict,
                new Label("Miktar:"), amountBox,
                new Label(""), btnAdd, lblMsg,
                new Label(""), sep, btnReport);

        return card;
    }

    // ==========================================
    // 3. ORTA KART: ATIK TABLOSU
    // ==========================================
    private VBox createTableCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        styleCard(card);
        VBox.setVgrow(card, Priority.ALWAYS); // Kartın uzamasına izin ver

        Label lblTitle = new Label("📋 Atık Geçmişim");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.DARKGRAY);

        setupMainTable();
        VBox.setVgrow(table, Priority.ALWAYS); // Tablonun uzamasına izin ver

        card.getChildren().addAll(lblTitle, table);
        return card;
    }

    // ==========================================
    // 4. SAĞ KART: LİDERLİK TABLOSU
    // ==========================================
    private VBox createLeaderboardCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(260);
        styleCard(card);
        card.setStyle("-fx-background-color: #FFF8E1; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);"); // Hafif sarımsı arka plan

        Label lblTitle = new Label("🏆 En Çevreciler");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTitle.setTextFill(Color.web("#FF8F00")); // Altın sarısı/Turuncu başlık

        setupLeaderboardTable();
        VBox.setVgrow(tableTop, Priority.ALWAYS);

        card.getChildren().addAll(lblTitle, tableTop);
        return card;
    }

    // ==========================================
    // TABLO AYARLARI
    // ==========================================
    private void setupMainTable() {
        TableColumn<Waste, String> colCat = new TableColumn<>("Kategori");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Waste, String> colDist = new TableColumn<>("Mahalle");
        colDist.setCellValueFactory(new PropertyValueFactory<>("district"));

        TableColumn<Waste, Double> colAmount = new TableColumn<>("Miktar");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Waste, String> colUnit = new TableColumn<>("Birim");
        colUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));

        TableColumn<Waste, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colCat, colDist, colAmount, colUnit, colStatus);

        // Genişlik Ayarları (İsteğin üzerine korundu)
        colCat.setMaxWidth(100); colCat.setMinWidth(80);
        colAmount.setMaxWidth(90); colAmount.setMinWidth(70);
        colUnit.setMaxWidth(60); colUnit.setMinWidth(50);
        colStatus.setMinWidth(150);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().forEach(col -> { col.setReorderable(false); col.setResizable(false); });

        // Tablo Stili (Kenarlıkları kaldır)
        table.setStyle("-fx-base: white; -fx-background-color: white;");
    }

    private void setupLeaderboardTable() {
        TableColumn<UserDAO.UserScore, String> colName = new TableColumn<>("İsim");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<UserDAO.UserScore, Integer> colScore = new TableColumn<>("Puan");
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableTop.getColumns().addAll(colName, colScore);
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableTop.getColumns().forEach(col -> { col.setReorderable(false); col.setResizable(false); });

        // Sarı tema uyumu
        tableTop.setStyle("-fx-base: #FFF8E1; -fx-control-inner-background: #FFF8E1; -fx-background-color: #FFF8E1;");
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(wasteDAO.getMyWastes(username)));
    }

    private void refreshLeaderboard() {
        tableTop.setItems(FXCollections.observableArrayList(userDAO.getTopUsers()));
    }

    // ==========================================
    // STİL YARDIMCILARI
    // ==========================================
    private void styleCard(VBox box) {
        box.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
    }

    private void styleField(TextField txt) {
        txt.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
        txt.setFont(Font.font("Segoe UI", 13));
    }

    private void styleComboBox(ComboBox<?> cmb) {
        cmb.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5;");
        cmb.setMaxWidth(Double.MAX_VALUE);
    }

    private void stylePrimaryButton(Button btn) {
        btn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(10));
    }

    private void styleInfoButton(Button btn) {
        btn.setStyle("-fx-background-color: #0288D1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPadding(new Insets(10));
    }

    private void styleDangerButton(Button btn) {
        btn.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-border-color: #FFCDD2; -fx-border-radius: 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-padding: 5 15 5 15;");
    }
}