package com.insightagent.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.List;
import java.util.Map;

@Route("login")
@PageTitle("Log in | InsightAgent")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm login = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        login.setAction("login");
        login.setForgotPasswordButtonVisible(false);

        Paragraph tagline = new Paragraph("Upload data. Ask in plain English. Get answers.");
        tagline.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        add(new H1("InsightAgent"), tagline, login,
                new RouterLink("New here? Create an account", RegisterView.class));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
        if (params.containsKey("error")) {
            login.setError(true);
        }
        if (params.containsKey("registered")) {
            Notification.show("Account created, log in now", 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        }
    }
}
