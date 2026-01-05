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
        stage.setTitle("RecycleShare - Toplayıcı Paneli (" + userEmail + ")");
        StackPane rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");
        BorderPane mainLayout = new BorderPane();
        mainLayout.setTop(createHeader());

        VBox contentCard = createContentCard();
        BorderPane.setMargin(contentCard, new Insets(20));
        mainLayout.setCenter(contentCard);

        rootPane.getChildren().add(mainLayout);
        setupTable();
        refreshTable();
        Scene scene = new Scene(rootPane, 1000, 700);
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
        Label lblUser = new Label("Toplayıcı: " + userEmail);
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

    private VBox createContentCard() {
        VBox card = new VBox(15);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");

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

        btnSearch.setOnAction(e -> {
            if (isViewingAvailable) table.setItems(FXCollections.observableArrayList(wasteDAO.searchWastesByDistrict(txtSearch.getText())));
            else showAlert("Bilgi", "Arama sadece 'Müsait Atıklar' modunda çalışır.");
        });
        btnRefresh.setOnAction(e -> { txtSearch.clear(); refreshTable(); });
        topRow.getChildren().addAll(txtSearch, btnSearch, btnRefresh);

        HBox tabRow = new HBox(0);
        tabRow.setAlignment(Pos.CENTER);
        tglAvailable = new ToggleButton("Müsait Atıklar");
        tglReserved = new ToggleButton("Rezerve Ettiklerim");
        ToggleGroup group = new ToggleGroup();
        tglAvailable.setToggleGroup(group); tglReserved.setToggleGroup(group);
        tglAvailable.setSelected(true);
        styleTabButton(tglAvailable, true); styleTabButton(tglReserved, false);
        tglAvailable.setOnAction(e -> switchMode(true)); tglReserved.setOnAction(e -> switchMode(false));
        tabRow.getChildren().addAll(tglAvailable, tglReserved);

        VBox.setVgrow(table, Priority.ALWAYS);
        table.setStyle("-fx-base: #FFFFFF; -fx-control-inner-background: #FFFFFF; -fx-background-color: #FFFFFF; -fx-padding: 5;");

        btnAction = new Button("SEÇİLENİ REZERVE ET 🚛");
        stylePrimaryButton(btnAction);
        btnAction.setMaxWidth(Double.MAX_VALUE);
        btnAction.setPrefHeight(50);
        btnAction.setOnAction(e -> handleMainAction());

        card.getChildren().addAll(topRow, new Separator(), tabRow, table, btnAction);
        return card;
    }

    private void switchMode(boolean showAvailable) {
        isViewingAvailable = showAvailable;
        if (showAvailable) {
            btnAction.setText("SEÇİLENİ REZERVE ET 🚛");
            btnAction.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
            tglAvailable.setSelected(true);
        } else {
            btnAction.setText("TESLİM AL VE PUANLA ✅");
            btnAction.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");
            tglReserved.setSelected(true);
        }
        refreshTable();
    }

    // CollectorPage.java içindeki handleMainAction metodunu bununla değiştir:

    private void handleMainAction() {
        Waste selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Uyarı", "Lütfen listeden bir atık seçiniz!");
            return;
        }

        if (isViewingAvailable) {
            // --- YENİ KONTROL MANTIĞI ---
            // "Bu atığı rezerve etmeme izin var mı?" diye soruyoruz
            if (!wasteDAO.isReservationAllowed(userEmail, selected.getId())) {
                showAlert("İşlem Engellendi ⛔",
                        "Şu an başka bir kullanıcıyla aktif rezervasyonunuz var.\n" +
                                "Aynı anda sadece TEK BİR KULLANICININ atıklarını toplayabilirsiniz.\n" +
                                "Mevcut işinizi tamamlayın veya aynı kullanıcının diğer ilanlarına bakın.");
                return;
            }

            // İzin varsa rezerve et
            if (wasteDAO.reserveWaste(selected.getId(), userEmail)) {
                showAlert("Başarılı", "Atık rezerve edildi! Aynı kullanıcının diğer atıklarını da alabilirsiniz.");
                refreshTable();
            } else {
                showAlert("Hata", "Rezervasyon işlemi başarısız.");
            }
        } else {
            // Puanlama / Tamamlama kısmı
            handleCompletion(selected);
        }
    }

    private void handleCompletion(Waste waste) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Puanlama");
        dialog.setHeaderText("Teslimat Değerlendirmesi");
        ButtonType saveBtn = new ButtonType("Kaydet", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        ComboBox<Integer> cmbClean = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5)); cmbClean.getSelectionModel().select(4);
        ComboBox<Integer> cmbAcc = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5)); cmbAcc.getSelectionModel().select(4);
        ComboBox<Integer> cmbPunc = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5)); cmbPunc.getSelectionModel().select(4);

        grid.add(new Label("Temizlik:"), 0, 0); grid.add(cmbClean, 1, 0);
        grid.add(new Label("Miktar:"), 0, 1); grid.add(cmbAcc, 1, 1);
        grid.add(new Label("Zamanlama:"), 0, 2); grid.add(cmbPunc, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isPresent() && res.get() == saveBtn) {
            if (wasteDAO.completeCollection(waste.getId(), cmbClean.getValue(), cmbAcc.getValue(), cmbPunc.getValue())) {
                showAlert("Tamamlandı", "Puan kaydedildi."); refreshTable();
            }
        }
    }

    private void setupTable() {
        table.getColumns().clear();
        TableColumn<Waste, String> c1 = new TableColumn<>("Kategori"); c1.setCellValueFactory(new PropertyValueFactory<>("category"));
        TableColumn<Waste, String> c2 = new TableColumn<>("Mahalle"); c2.setCellValueFactory(new PropertyValueFactory<>("district"));
        TableColumn<Waste, Double> c3 = new TableColumn<>("Miktar"); c3.setCellValueFactory(new PropertyValueFactory<>("amount"));
        TableColumn<Waste, String> c4 = new TableColumn<>("Birim"); c4.setCellValueFactory(new PropertyValueFactory<>("unit"));
        TableColumn<Waste, String> c5 = new TableColumn<>("Durum"); c5.setCellValueFactory(new PropertyValueFactory<>("status"));

        table.getColumns().addAll(c1, c2, c3, c4, c5);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        c1.setMinWidth(100); c2.setMinWidth(100); c3.setMinWidth(70); c4.setMinWidth(50); c5.setMinWidth(150);
    }

    private void refreshTable() {
        if (isViewingAvailable) table.setItems(FXCollections.observableArrayList(wasteDAO.getAvailableWastes()));
        else table.setItems(FXCollections.observableArrayList(wasteDAO.getMyReservations(userEmail)));
    }

    private void styleField(TextField t) { t.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 8;"); }
    private void stylePrimaryButton(Button b) { b.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;"); }
    private void styleSecondaryButton(Button b) { b.setStyle("-fx-background-color: #EEEEEE; -fx-text-fill: #333; -fx-border-color: #DDD; -fx-border-radius: 5;"); }
    private void styleDangerButton(Button b) { b.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #D32F2F; -fx-border-color: #FFCDD2; -fx-border-radius: 20;"); }
    private void styleTabButton(ToggleButton b, boolean isLeft) {
        String r = isLeft ? "8 0 0 8" : "0 8 8 0";
        b.styleProperty().bind(javafx.beans.binding.Bindings.when(b.selectedProperty())
                .then("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: " + r + ";")
                .otherwise("-fx-background-color: white; -fx-text-fill: #2E7D32; -fx-border-color: #2E7D32; -fx-background-radius: " + r + "; -fx-border-radius: " + r + ";"));
        b.setPrefWidth(160); b.setPrefHeight(35);
    }
    private void showAlert(String t, String c) { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(t); a.setContentText(c); a.showAndWait(); }
}