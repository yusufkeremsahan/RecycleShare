package view;

import dao.UserDAO;
import dao.WasteDAO;
import model.Waste;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ResidentPage {

    private String username;
    private WasteDAO wasteDAO = new WasteDAO();
    private UserDAO userDAO = new UserDAO(); // Enler listesi için
    private TableView<Waste> table = new TableView<>();
    private Stage stage; // Kapatmak için stage referansı lazım

    public ResidentPage(String username) {
        this.username = username;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Sakin Paneli (" + username + ")");

        // --- ÜST PANEL (Çıkış Butonu) ---
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(10));
        topBar.setStyle("-fx-background-color: #eee;");
        topBar.setSpacing(10);

        Label lblWelcome = new Label("Hoşgeldin, " + username);
        lblWelcome.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Button btnLogout = new Button("Çıkış Yap 🚪");
        btnLogout.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white;");

        // ÇIKIŞ YAPMA MANTIĞI (Feature 2)
        btnLogout.setOnAction(e -> {
            stage.close(); // Mevcut sayfayı kapat
            try {
                new LoginApp().start(new Stage()); // Login ekranını yeniden başlat
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        HBox spacer = new HBox(); // Sağa yaslamak için boşluk
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        topBar.getChildren().addAll(lblWelcome, spacer, btnLogout);


        // --- SOL PANEL (FORM) ---
        VBox formPanel = new VBox(10);
        formPanel.setPadding(new Insets(15));
        formPanel.setStyle("-fx-background-color: #f9f9f9;");
        formPanel.setPrefWidth(250);

        Label lblTitle = new Label("Atık Ekle");
        lblTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        ComboBox<String> cmbCategory = new ComboBox<>();
        cmbCategory.setPromptText("Kategori Seç");
        cmbCategory.getItems().addAll(wasteDAO.getCategories());

        TextField txtDistrict = new TextField();
        txtDistrict.setPromptText("Mahalle Giriniz");

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("Miktar (kg)");

        Button btnAdd = new Button("Listeye Ekle");
        btnAdd.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white;");
        btnAdd.setMaxWidth(Double.MAX_VALUE);

        Label lblMsg = new Label();

        btnAdd.setOnAction(e -> {
            try {
                String cat = cmbCategory.getValue();
                String dist = txtDistrict.getText();
                double amount = Double.parseDouble(txtAmount.getText());
                if (wasteDAO.addWaste(username, cat, dist, amount)) {
                    lblMsg.setText("Eklendi!");
                    lblMsg.setStyle("-fx-text-fill: green;");
                    refreshTable();
                } else {
                    lblMsg.setText("Hata oluştu.");
                }
            } catch (Exception ex) {
                lblMsg.setText("Hatalı giriş!");
            }
        });

        // Rapor Butonu
        Button btnReport = new Button("Etki Raporumu Gör 🌍");
        btnReport.setStyle("-fx-background-color: #0288D1; -fx-text-fill: white;");
        btnReport.setMaxWidth(Double.MAX_VALUE);
        btnReport.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Rapor");
            alert.setHeaderText(null);
            alert.setContentText(userDAO.getImpactReport(username));
            alert.showAndWait();
        });

        formPanel.getChildren().addAll(lblTitle, new Label("Tür:"), cmbCategory,
                new Label("Mahalle:"), txtDistrict,
                new Label("Miktar:"), txtAmount,
                new Label(""), btnAdd, btnReport, lblMsg);


        // --- SAĞ PANEL (ENLER TABLOSU) --- (Feature 3)
        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setPrefWidth(200);
        rightPanel.setStyle("-fx-background-color: #fff3e0;"); // Hafif turuncu

        Label lblTop = new Label("🏆 EN ÇEVRECİLER");
        lblTop.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TableView<UserDAO.UserScore> tableTop = new TableView<>();
        TableColumn<UserDAO.UserScore, String> colName = new TableColumn<>("İsim");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<UserDAO.UserScore, Integer> colScore = new TableColumn<>("Puan");
        colScore.setCellValueFactory(new PropertyValueFactory<>("score"));

        tableTop.getColumns().addAll(colName, colScore);
        tableTop.setItems(FXCollections.observableArrayList(userDAO.getTopUsers())); // Veriyi çek
        tableTop.setPrefHeight(300);

        rightPanel.getChildren().addAll(lblTop, tableTop);


        // --- ORTA PANEL (ATIKLARIM TABLOSU) ---
        TableColumn<Waste, String> colCat = new TableColumn<>("Kategori");
        colCat.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Waste, String> colDist = new TableColumn<>("Mahalle");
        colDist.setCellValueFactory(new PropertyValueFactory<>("district"));

        TableColumn<Waste, Double> colAmount = new TableColumn<>("Miktar");
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));

        TableColumn<Waste, String> colStatus = new TableColumn<>("Durum");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(colCat, colDist, colAmount, colStatus);
        refreshTable();

        // --- ANA DÜZEN ---
        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setLeft(formPanel);
        root.setCenter(table);
        root.setRight(rightPanel); // Enler tablosunu sağa koyduk

        Scene scene = new Scene(root, 1000, 600); // Ekranı biraz büyüttük
        stage.setScene(scene);
        stage.show();
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(wasteDAO.getMyWastes(username)));
    }
}