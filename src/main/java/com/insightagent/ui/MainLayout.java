package com.insightagent.ui;

import com.insightagent.security.CurrentUser;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import jakarta.annotation.security.PermitAll;

@PermitAll
public class MainLayout extends AppLayout {

    public MainLayout(CurrentUser currentUser) {
        H1 logo = new H1("InsightAgent");
        logo.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        Span user = new Span(currentUser.get().getUsername());
        user.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button logout = new Button("Log out", e -> currentUser.logout());
        logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), logo, user, logout);
        header.setWidthFull();
        header.expand(logo);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.getStyle().set("padding-right", "var(--lumo-space-m)");
        addToNavbar(header);

        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem("Datasets", DatasetsView.class, VaadinIcon.DATABASE.create()));
        nav.addItem(new SideNavItem("Ask AI", ChatView.class, VaadinIcon.CHAT.create()));
        addToDrawer(new Scroller(nav));
    }
}
