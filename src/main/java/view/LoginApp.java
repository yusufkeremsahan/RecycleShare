package view;

import dao.UserDAO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class LoginApp extends Application {

    private UserDAO userDAO = new UserDAO();
    private Stage primaryStage;
    private StackPane rootPane;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("RecycleShare");
        rootPane = new StackPane();
        rootPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2E7D32, #81C784);");

        primaryStage.getIcons().add(new Image("file:logo4.png"));
        showLoginScreen();

        // Kart 420px olduğu için sahneyi biraz daha geniş (550) ve uzun (750) tuttuk
        Scene scene = new Scene(rootPane, 490, 730);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // YARDIMCI METOT: Logoyu güvenli şekilde yükle
    // Not: Bilgisayarındaki dosyalar .jpg ise burayı "file:logo"+no+".jpg" yapmalısın.
    private ImageView getLogo(int no, double width) {
        ImageView logo = new ImageView();
        try {
            // Varsayılan olarak png arıyor, jpg ise uzantıyı değiştirmeyi unutma
            logo.setImage(new Image("file:logo" + no + ".png"));
            logo.setFitWidth(width);
            logo.setPreserveRatio(true);
        } catch (Exception e) {
            System.out.println("Logo yüklenemedi: " + e.getMessage());
        }
        return logo;
    }

    private void showLoginScreen() {
        VBox card = createCard();

        // 1. LOGO (Genişlik 350px)
        ImageView logoView = getLogo(1, 350);

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("E-posta Adresi");
        styleField(txtEmail);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre");
        styleField(txtPass);

        Button btnLogin = new Button("GİRİŞ YAP");
        styleButton(btnLogin);

        Label lblMsg = new Label();
        // Mesaj label'ı için sabit bir yükseklik verelim ki metin gelince kayma yapmasın
        lblMsg.setMinHeight(20);

        btnLogin.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            String pass = txtPass.getText().trim();
            String role = userDAO.login(email, pass);
            if (role != null) {
                lblMsg.setText("Giriş başarılı!"); lblMsg.setTextFill(Color.GREEN);
                openApp(role, email);
            } else {
                lblMsg.setText("Hatalı e-posta veya şifre!"); lblMsg.setTextFill(Color.RED);
                shakeAnimation(card);
            }
        });

        Hyperlink linkRegister = new Hyperlink("Hemen Kaydol");
        linkRegister.setTextFill(Color.web("#2E7D32"));
        linkRegister.setOnAction(e -> showRegisterScreen());
        TextFlow flow = new TextFlow(new Text("Hesabın yok mu? "), linkRegister);
        flow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        // Boşlukları (Label"") VBox spacing zaten hallediyor ama ekstra boşluk istersen Region kullanabilirsin.
        // Burada basitlik adına senin yapını korudum.
        card.getChildren().addAll(logoView, new Label(""), txtEmail, txtPass, new Label(""), btnLogin, lblMsg, new Label(""), flow);

        rootPane.getChildren().clear();
        rootPane.getChildren().add(card);
    }

    private void showRegisterScreen() {
        VBox card = createCard();

        // 3. LOGO (Kayıt ekranı için logo no:3, yine 350px istedin)
        ImageView logoView = getLogo(3, 350);

        TextField txtEmail = new TextField();
        txtEmail.setPromptText("E-posta Adresi (Örn: ali@mail.com)");
        styleField(txtEmail);

        TextField txtFullname = new TextField();
        txtFullname.setPromptText("Ad Soyad");
        styleField(txtFullname);

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre Belirle");
        styleField(txtPass);

        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("Mahalle Sakini", "Geri Dönüşüm Toplayıcısı");
        cmbRole.setPromptText("Hesap Türü");
        cmbRole.setMaxWidth(Double.MAX_VALUE);
        cmbRole.setStyle("-fx-background-color: #f9f9f9;");

        Button btnRegister = new Button("KAYIT OL");
        styleButton(btnRegister);

        Label lblMsg = new Label();
        lblMsg.setMinHeight(20);

        btnRegister.setOnAction(e -> {
            String email = txtEmail.getText().trim();
            String pass = txtPass.getText().trim();
            String name = txtFullname.getText().trim();
            String selectedRole = cmbRole.getValue();

            if (email.isEmpty() || pass.isEmpty() || name.isEmpty() || selectedRole == null) {
                lblMsg.setText("Tüm alanları doldurun!"); lblMsg.setTextFill(Color.RED);
                shakeAnimation(card); return;
            }
            String dbRole = "";
            if (selectedRole.equals("Mahalle Sakini")) {
                dbRole = "SAKIN";
            } else if (selectedRole.equals("Geri Dönüşüm Toplayıcısı")) {
                dbRole = "TOPLAYICI";
            }
            if (userDAO.register(email, pass, name, dbRole)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Kayıt başarılı! Giriş yapabilirsiniz.");
                alert.showAndWait(); showLoginScreen();
            } else {
                lblMsg.setText("Bu e-posta zaten kayıtlı!"); lblMsg.setTextFill(Color.RED);
            }
        });

        Hyperlink linkLogin = new Hyperlink("Giriş Yap");
        linkLogin.setTextFill(Color.web("#2E7D32"));
        linkLogin.setOnAction(e -> showLoginScreen());
        TextFlow flow = new TextFlow(new Text("Zaten üye misin? "), linkLogin);
        flow.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(logoView, new Label(""), txtEmail, txtFullname, txtPass, cmbRole, new Label(""), btnRegister, lblMsg, new Label(""), flow);

        rootPane.getChildren().clear();
        rootPane.getChildren().add(card);
    }

    private VBox createCard() {
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);

        // --- BOYUT SABİTLEME ---
        // 350px logo + 30px sağ/sol padding = 410px içerik alanı gerekir.
        // Kartı 420px genişliğinde, 700px yüksekliğinde sabitliyoruz.
        card.setPrefSize(420, 700);
        card.setMinSize(420, 700);
        card.setMaxSize(420, 700);

        card.setPadding(new Insets(30));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 5);");
        return card;
    }

    private void styleField(TextField f) {
        f.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: #e0e0e0; -fx-border-radius: 5; -fx-padding: 10;");
    }

    private void styleButton(Button b) {
        b.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPadding(new Insets(10));
    }

    private void openApp(String role, String email) {
        try {
            primaryStage.close();
            if (role.equals("SAKIN")) new ResidentPage(email).show();
            else if (role.equals("TOPLAYICI")) new CollectorPage(email).show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void shakeAnimation(VBox node) {
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(50), node);
        tt.setByX(10); tt.setCycleCount(4); tt.setAutoReverse(true); tt.play();
    }

    public static void launch(Class<? extends Application> appClass, String[] args) {
        Application.launch(appClass, args);
    }
}