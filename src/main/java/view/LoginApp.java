package view;

import dao.UserDAO;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginApp extends Application {

    private UserDAO userDAO = new UserDAO();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RecycleShare - Hoşgeldiniz");

        // --- SEKME YAPISI (Giriş / Kayıt) ---
        TabPane tabPane = new TabPane();

        // Sekme 1: GİRİŞ YAP
        Tab loginTab = new Tab("Giriş Yap", createLoginContent(primaryStage));
        loginTab.setClosable(false); // Kapatılamaz olsun

        // Sekme 2: KAYIT OL
        Tab registerTab = new Tab("Kayıt Ol", createRegisterContent(tabPane));
        registerTab.setClosable(false);

        tabPane.getTabs().addAll(loginTab, registerTab);

        Scene scene = new Scene(tabPane, 400, 450);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // --- GİRİŞ FORMU TASARIMI ---
    private VBox createLoginContent(Stage primaryStage) {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        Label title = new Label("Giriş Yap");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Kullanıcı Adı");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre");

        Button btnLogin = new Button("Giriş Yap 🚀");
        btnLogin.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold;");

        Label lblMsg = new Label();

        btnLogin.setOnAction(e -> {
            String role = userDAO.login(txtUser.getText(), txtPass.getText());
            if (role != null) {
                lblMsg.setTextFill(Color.GREEN);
                lblMsg.setText("Başarılı! Yönlendiriliyor...");

                // Yönlendirme Mantığı
                try {
                    primaryStage.close(); // Login ekranını kapat
                    if (role.equals("SAKIN")) {
                        new ResidentPage(txtUser.getText()).show();
                    } else if (role.equals("TOPLAYICI")) {
                        new CollectorPage(txtUser.getText()).show();
                    }
                } catch (Exception ex) { ex.printStackTrace(); }

            } else {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Hatalı kullanıcı adı veya şifre!");
            }
        });

        vbox.getChildren().addAll(title, new Label("Kullanıcı Adı:"), txtUser, new Label("Şifre:"), txtPass, btnLogin, lblMsg);
        return vbox;
    }

    // --- KAYIT FORMU TASARIMI ---
    private VBox createRegisterContent(TabPane tabPane) {
        VBox vbox = new VBox(15);
        vbox.setAlignment(Pos.CENTER);
        vbox.setPadding(new Insets(20));

        Label title = new Label("Yeni Hesap Oluştur");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        TextField txtUser = new TextField();
        txtUser.setPromptText("Kullanıcı Adı Seçin");

        PasswordField txtPass = new PasswordField();
        txtPass.setPromptText("Şifre Belirleyin");

        TextField txtFullname = new TextField();
        txtFullname.setPromptText("Ad Soyad");

        // Rol Seçimi (ComboBox)
        ComboBox<String> cmbRole = new ComboBox<>();
        cmbRole.getItems().addAll("SAKIN", "TOPLAYICI");
        cmbRole.setPromptText("Rol Seçiniz");

        Button btnRegister = new Button("Kayıt Ol ✅");
        Label lblMsg = new Label();

        btnRegister.setOnAction(e -> {
            String u = txtUser.getText();
            String p = txtPass.getText();
            String f = txtFullname.getText();
            String r = cmbRole.getValue();

            if (u.isEmpty() || p.isEmpty() || f.isEmpty() || r == null) {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Tüm alanları doldurun!");
                return;
            }

            boolean success = userDAO.register(u, p, f, r);
            if (success) {
                lblMsg.setTextFill(Color.GREEN);
                lblMsg.setText("Kayıt Başarılı! Giriş sekmesine geçin.");
                // Formu temizle
                txtUser.clear(); txtPass.clear(); txtFullname.clear();
                // Giriş sekmesine otomatik geçiş yapabiliriz (Opsiyonel)
                tabPane.getSelectionModel().select(0);
            } else {
                lblMsg.setTextFill(Color.RED);
                lblMsg.setText("Bu kullanıcı adı zaten alınmış!");
            }
        });

        vbox.getChildren().addAll(title, txtUser, txtPass, txtFullname, cmbRole, btnRegister, lblMsg);
        return vbox;
    }
}