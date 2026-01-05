package view;

import dao.UserDAO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class LoginApp extends Application {

    private UserDAO userDAO = new UserDAO();
    private Stage primaryStage;
    private StackPane rootPane; // Sahne geçişleri için ana konteyner

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("RecycleShare");

        // --- ARKA PLAN TASARIMI ---
        // Yeşil tonlarında modern bir gradyan arka plan
        rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        // İlk açılışta Giriş Ekranını göster
        showLoginScreen();

        Scene scene = new Scene(rootPane, 450, 550); // Biraz daha uzun bir pencere
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // ==========================================
    // 1. GİRİŞ EKRANI (LOGIN CARD)
    // ==========================================
    private void showLoginScreen() {
        // Beyaz Kart Paneli
        VBox card = createCard();

        // Başlık
        Label lblTitle = new Label("RecycleShare");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblTitle.setTextFill(Color.web("#2E7D32"));

        Label lblSubtitle = new Label("Geri Dönüşümün Merkezi");
        lblSubtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        lblSubtitle.setTextFill(Color.GRAY);

        // Form Elemanları
        TextField txtUser = new TextField();
        txtUser.setPromptText("Kullanıcı Adı");
        styleField(txtUser);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre");
        styleField(txtPass);

        Button btnLogin = new Button("GİRİŞ YAP");
        styleButton(btnLogin);

        Label lblMsg = new Label();
        lblMsg.setFont(Font.font(12));

        // --- GİRİŞ MANTIĞI ---
        btnLogin.setOnAction(e -> {
            String cleanUser = txtUser.getText().trim();
            String cleanPass = txtPass.getText().trim();

            String role = userDAO.login(cleanUser, cleanPass);
            if (role != null) {
                lblMsg.setTextFill(Color.GREEN);
                lblMsg.setText("Giriş başarılı! Yönlendiriliyor...");
                openApp(role, cleanUser);
            } else {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Hatalı kullanıcı adı veya şifre!");
                shakeAnimation(card); // Hata olursa kartı titret (Basit animasyon)
            }
        });

        // --- ALT KISIM: "Hesabın yok mu? Kaydol" ---
        Text txtInfo = new Text("Hesabın yok mu? ");
        txtInfo.setFill(Color.DARKGRAY);

        Hyperlink linkRegister = new Hyperlink("Hemen Kaydol");
        linkRegister.setTextFill(Color.web("#2E7D32"));
        linkRegister.setStyle("-fx-border-color: transparent; -fx-padding: 0;");
        linkRegister.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        // Kaydol'a basınca ekranı değiştir
        linkRegister.setOnAction(e -> showRegisterScreen());

        TextFlow flow = new TextFlow(txtInfo, linkRegister);
        flow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Kart içine ekle
        card.getChildren().addAll(lblTitle, lblSubtitle, new Label(""), txtUser, txtPass, new Label(""), btnLogin, lblMsg, new Label(""), flow);

        // Root pane'i temizle ve yeni kartı ekle
        rootPane.getChildren().clear();
        rootPane.getChildren().add(card);
    }

    // ==========================================
    // 2. KAYIT EKRANI (REGISTER CARD)
    // ==========================================
    private void showRegisterScreen() {
        VBox card = createCard();

        Label lblTitle = new Label("Aramıza Katıl");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        lblTitle.setTextFill(Color.web("#2E7D32"));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Kullanıcı Adı Belirle");
        styleField(txtUser);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre");
        styleField(txtPass);

        TextField txtFullname = new TextField();
        txtFullname.setPromptText("Ad Soyad");
        styleField(txtFullname);

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("SAKIN", "TOPLAYICI");
        cmbRole.setPromptText("Hesap Türü Seçin");
        cmbRole.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5;");
        cmbRole.setMaxWidth(Double.MAX_VALUE);

        Button btnRegister = new Button("KAYIT OL");
        styleButton(btnRegister);

        Label lblMsg = new Label();

        // --- KAYIT MANTIĞI ---
        btnRegister.setOnAction(e -> {
            String u = txtUser.getText();
            String p = txtPass.getText();
            String f = txtFullname.getText();
            String r = cmbRole.getValue();

            if (u.isEmpty() || p.isEmpty() || f.isEmpty() || r == null) {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Lütfen tüm alanları doldurun!");
                shakeAnimation(card);
                return;
            }

            if (userDAO.register(u, p, f, r)) {
                // Başarılı olursa Alert ile bildirip Giriş'e dönelim
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Başarılı");
                alert.setHeaderText(null);
                alert.setContentText("Kayıt oluşturuldu! Giriş yapabilirsiniz.");
                alert.showAndWait();
                showLoginScreen();
            } else {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Bu kullanıcı adı zaten kullanılıyor.");
            }
        });

        // --- ALT KISIM: "Zaten üye misin? Giriş Yap" ---
        Text txtInfo = new Text("Zaten hesabın var mı? ");
        txtInfo.setFill(Color.DARKGRAY);

        Hyperlink linkLogin = new Hyperlink("Giriş Yap");
        linkLogin.setTextFill(Color.web("#2E7D32"));
        linkLogin.setStyle("-fx-border-color: transparent; -fx-padding: 0;");
        linkLogin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        linkLogin.setOnAction(e -> showLoginScreen());

        TextFlow flow = new TextFlow(txtInfo, linkLogin);
        flow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(lblTitle, new Label(""), txtUser, txtPass, txtFullname, cmbRole, new Label(""), btnRegister, lblMsg, new Label(""), flow);

        rootPane.getChildren().clear();
        rootPane.getChildren().add(card);
    }

    // ==========================================
    // YARDIMCI TASARIM METOTLARI
    // ==========================================

    // Beyaz Kart Oluşturucu
    private VBox createCard() {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(320); // Kart genişliği
        card.setMaxHeight(450);
        card.setPadding(new Insets(30));

        // Kart Stili: Beyaz, Yuvarlak Köşe, Gölge
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
        return card;
    }

    // Input Alanları Stili
    private void styleField(TextField field) {
        field.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 10;");
        field.setFont(Font.font("Segoe UI", 13));
    }

    // Buton Stili
    private void styleButton(Button btn) {
        btn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        btn.setPadding(new Insets(10, 20, 10, 20));
        btn.setMaxWidth(Double.MAX_VALUE);

        // Hover Efekti
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1B5E20; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;"));
    }

    // Uygulamayı Açma Mantığı
    private void openApp(String role, String username) {
        try {
            primaryStage.close();
            if (role.equals("SAKIN")) {
                new ResidentPage(username).show();
            } else if (role.equals("TOPLAYICI")) {
                new CollectorPage(username).show();
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    // Hata durumunda kartı sallama animasyonu (Basit)
    private void shakeAnimation(VBox node) {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(50), node);
        tt.setByX(10);
        tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }
}