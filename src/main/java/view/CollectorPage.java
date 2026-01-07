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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Optional;

public class CollectorPage {

    private String userEmail;
    private WasteDAO wasteDAO = new WasteDAO();
    private TableView<Waste> table = new TableView<>();
    private Stage stage;
    private boolean isViewingAvailable = true;

    private ToggleButton tglAvailable;
    private ToggleButton tglReserved;
    private Button btnAction;

    public CollectorPage(String email) {
        this.userEmail = email;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("RecycleShare - Toplayıcı Operasyon Paneli");

        StackPane rootPane = new StackPane();
        // Yeşil Gradyan Arka Plan
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        stage.getIcons().add(new Image("file:logo4.png"));

        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());

        VBox contentCard = createContentCard();
        BorderPane.setMargin(contentCard, new Insets(20));
        mainLayout.setCenter(contentCard);

        rootPane.getChildren().add(mainLayout);

        setupTable();
        refreshTable();

        Scene scene = new Scene(rootPane, 1280, 800);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createHeader() {
        HBox header = new HBox();
        header.setPadding(new Insets(15, 40, 15, 40));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(15); // Boşluk
        header.setStyle("-fx-background-color: #ffffff; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // 1. KÜÇÜK LOGO
        ImageView logoView = new ImageView();
        try {
            logoView.setImage(new Image("file:logo4.png"));
            logoView.setFitHeight(50);
            logoView.setPreserveRatio(true);
        } catch (Exception e) { }

        VBox titleBox = new VBox(2);
        Label lblBrand = new Label("RecycleShare");
        lblBrand.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblBrand.setTextFill(Color.web("#2E7D32"));

        Label lblUser = new Label("Operasyon: " + userEmail);
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
            try { new LoginApp().start(new Stage()); } catch (Exception ex) { ex.printStackTrace(); }
        });

        // Header'a logoyu en başa ekliyoruz
        header.getChildren().addAll(logoView, titleBox, spacer, btnLogout);
        return header;
    }
    private VBox createContentCard() {
        VBox card = new VBox(20);
        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 0, 4);");

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Mahalle veya Bölge Ara...");
        txtSearch.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10; -fx-font-size: 14px;");
        txtSearch.setPrefWidth(400);

        Button btnSearch = new Button("Ara 🔍");
        btnSearch.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand; -fx-padding: 10 20;");

        Button btnRefresh = new Button("Yenile 🔄");
        btnRefresh.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #333; -fx-border-color: #ddd; -fx-border-radius: 5; -fx-cursor: hand; -fx-padding: 10 20;");

        btnSearch.setOnAction(e -> {
            if (isViewingAvailable) table.setItems(FXCollections.observableArrayList(wasteDAO.searchWastesByDistrict(txtSearch.getText())));
            else showAlert("Bilgi", "Arama sadece 'Müsait Atıklar' listesinde çalışır.");
        });
        btnRefresh.setOnAction(e -> { txtSearch.clear(); refreshTable(); });

        topRow.getChildren().addAll(txtSearch, btnSearch, btnRefresh);

        HBox tabRow = new HBox(0);
        tabRow.setAlignment(Pos.CENTER_LEFT);
        tglAvailable = new ToggleButton("Müsait Atıklar");
        // DEĞİŞİKLİK: Yazı Güncellendi
        tglReserved = new ToggleButton("Rezerve Ettiklerim");

        ToggleGroup group = new ToggleGroup();
        tglAvailable.setToggleGroup(group); tglReserved.setToggleGroup(group);
        tglAvailable.setSelected(true);
        styleTabButton(tglAvailable, true); styleTabButton(tglReserved, false);

        tglAvailable.setOnAction(e -> switchMode(true));
        tglReserved.setOnAction(e -> switchMode(false));
        tabRow.getChildren().addAll(tglAvailable, tglReserved);

        VBox.setVgrow(table, Priority.ALWAYS);
        table.setStyle("-fx-base: #ffffff; -fx-font-size: 14px; -fx-selection-bar: #e8f5e9; -fx-selection-bar-non-focused: #f0f0f0;");

        btnAction = new Button("SEÇİLEN GÖREVİ AL 🚛");
        stylePrimaryButton(btnAction);
        btnAction.setOnAction(e -> handleMainAction());

        card.getChildren().addAll(topRow, new Separator(), tabRow, table, btnAction);
        return card;
    }

    private void setupTable() {
        table.getColumns().clear();

        TableColumn<Waste, String> cName = new TableColumn<>("Atık Sahibi");
        cName.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
        cName.setReorderable(false); cName.setResizable(false);
        cName.setMinWidth(150);

        TableColumn<Waste, String> cCat = new TableColumn<>("Kategori");
        cCat.setCellValueFactory(new PropertyValueFactory<>("category"));
        cCat.setReorderable(false); cCat.setResizable(false);
        cCat.setMinWidth(120);

        TableColumn<Waste, String> cLoc = new TableColumn<>("Adres Detayı");
        cLoc.setCellValueFactory(new PropertyValueFactory<>("fullLocation"));
        cLoc.setReorderable(false); cLoc.setResizable(false);
        cLoc.setMinWidth(350);

        TableColumn<Waste, Double> cAmt = new TableColumn<>("Miktar");
        cAmt.setCellValueFactory(new PropertyValueFactory<>("amount"));
        cAmt.setReorderable(false); cAmt.setResizable(false);

        TableColumn<Waste, String> cUnit = new TableColumn<>("Birim");
        cUnit.setCellValueFactory(new PropertyValueFactory<>("unit"));
        cUnit.setReorderable(false); cUnit.setResizable(false);

        TableColumn<Waste, String> cStat = new TableColumn<>("Durum");
        cStat.setCellValueFactory(new PropertyValueFactory<>("status"));
        cStat.setReorderable(false); cStat.setResizable(false);
        cStat.setMinWidth(120);


        TableColumn<Waste, String> cDate = new TableColumn<>("Tarih");
        cDate.setCellValueFactory(new PropertyValueFactory<>("dateInfo"));
        cDate.setReorderable(false); cDate.setResizable(false);
        cDate.setMinWidth(130);


        TableColumn<Waste, String> cTime = new TableColumn<>("Kalan Süre ⏳");
        cTime.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));
        cTime.setReorderable(false); cTime.setResizable(false);
        cTime.setMinWidth(140);
        cTime.setStyle("-fx-text-fill: #D32F2F; -fx-font-weight: bold;"); // Kırmızı ve Kalın

        // Tabloya ekle:
        table.getColumns().addAll(cDate, cName, cCat, cLoc, cAmt, cUnit, cStat, cTime);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void switchMode(boolean showAvailable) {
        isViewingAvailable = showAvailable;
        if (showAvailable) {
            btnAction.setText("SEÇİLEN GÖREVİ AL 🚛");
            stylePrimaryButton(btnAction);
            tglAvailable.setSelected(true);
        } else {
            btnAction.setText("TESLİM ALINDI & PUANLA ✅");
            btnAction.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 15;");
            tglReserved.setSelected(true);
        }
        refreshTable();
    }

    private void styleTabButton(ToggleButton b, boolean isLeft) {
        String radius = isLeft ? "5 0 0 5" : "0 5 5 0";
        b.setPrefWidth(180); b.setPrefHeight(40);
        b.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));

        b.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
            } else {
                b.setStyle("-fx-background-color: white; -fx-text-fill: #2E7D32; -fx-border-color: #2E7D32; -fx-border-width: 1; -fx-background-radius: " + radius + "; -fx-border-radius: " + radius + ";");
            }
        });
        if(b.isSelected()) b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: " + radius + ";");
        else b.setStyle("-fx-background-color: white; -fx-text-fill: #2E7D32; -fx-border-color: #2E7D32; -fx-border-width: 1; -fx-background-radius: " + radius + "; -fx-border-radius: " + radius + ";");
    }

    private void stylePrimaryButton(Button b) {
        b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 15;");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 15;"));
        b.setOnMouseExited(e -> b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 16px; -fx-padding: 15;"));
    }

    private void handleMainAction() {
        Waste selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Uyarı", "Lütfen listeden bir işlem seçiniz!"); return; }

        if (isViewingAvailable) {
            if (!wasteDAO.isReservationAllowed(userEmail, selected.getId())) {
                showAlert("İşlem Engellendi ⛔", "Aynı anda sadece tek bir adresteki atıkları toplayabilirsiniz."); return;
            }
            if (wasteDAO.reserveWaste(selected.getId(), userEmail)) {
                showAlert("Başarılı", "Görev atandı!"); refreshTable();
            } else { showAlert("Hata", "İşlem başarısız."); }
        } else {
            handleCompletion(selected);
        }
    }

    private void handleCompletion(Waste waste) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Teslimat Onayı");
        dialog.setHeaderText("Lütfen Sakin'i Değerlendirin");

        ButtonType loginButtonType = new ButtonType("ONAYLA VE PUANLA", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15); grid.setPadding(new Insets(20));

        ComboBox<Integer> cmbClean = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5)); cmbClean.setValue(5);
        ComboBox<Integer> cmbAcc = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5)); cmbAcc.setValue(5);
        ComboBox<Integer> cmbPunc = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5)); cmbPunc.setValue(5);

        grid.add(new Label("Temizlik:"), 0, 0); grid.add(cmbClean, 1, 0);
        grid.add(new Label("Miktar Tutarlılığı:"), 0, 1); grid.add(cmbAcc, 1, 1);
        grid.add(new Label("Zamanlama/İletişim:"), 0, 2); grid.add(cmbPunc, 1, 2);

        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == loginButtonType) {
            if (wasteDAO.completeCollection(waste.getId(), cmbClean.getValue(), cmbAcc.getValue(), cmbPunc.getValue())) {
                showAlert("Tamamlandı", "Puanlar kaydedildi ve stok güncellendi."); refreshTable();
            }
        }
    }

    private void refreshTable() {
        if (isViewingAvailable) table.setItems(FXCollections.observableArrayList(wasteDAO.getAvailableWastes()));
        else table.setItems(FXCollections.observableArrayList(wasteDAO.getMyReservations(userEmail)));
    }

    private void showAlert(String t, String c) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.showAndWait(); }
}