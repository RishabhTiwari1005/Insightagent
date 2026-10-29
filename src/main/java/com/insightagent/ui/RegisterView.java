package com.insightagent.ui;

import com.insightagent.security.UserService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;

@Route("register")
@PageTitle("Sign up | InsightAgent")
@AnonymousAllowed
public class RegisterView extends VerticalLayout {

    public RegisterView(UserService users) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        TextField username = new TextField("Username");
        PasswordField password = new PasswordField("Password");
        password.setHelperText("At least 8 characters");
        PasswordField confirm = new PasswordField("Confirm password");

        Button create = new Button("Create account");
        create.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        create.addClickShortcut(Key.ENTER);
        create.addClickListener(e -> {
            if (!password.getValue().equals(confirm.getValue())) {
                error("Passwords don't match");
                return;
            }
            try {
                users.register(username.getValue(), password.getValue());
                UI.getCurrent().navigate("login",
                        new QueryParameters(Map.of("registered", List.of("true"))));
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });

        VerticalLayout form = new VerticalLayout(username, password, confirm, create);
        form.setWidth("320px");
        form.setPadding(false);
        form.getChildren().forEach(c -> c.getElement().getStyle().set("width", "100%"));

        add(new H1("Create account"), form, new RouterLink("Already have an account? Log in", LoginView.class));
    }

    private static void error(String msg) {
        Notification.show(msg, 4000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
