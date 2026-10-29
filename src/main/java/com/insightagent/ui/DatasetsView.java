package com.insightagent.ui;

import com.insightagent.data.DatasetService;
import com.insightagent.domain.AppUser;
import com.insightagent.domain.Dataset;
import com.insightagent.security.CurrentUser;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Datasets | InsightAgent")
@PermitAll
public class DatasetsView extends VerticalLayout {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault());

    private final DatasetService datasets;
    private final AppUser user;
    private final Grid<Dataset> grid = new Grid<>(Dataset.class, false);
    private final Paragraph empty = new Paragraph("No datasets yet. Upload a CSV or Excel file to start (try samples/sales.csv).");

    public DatasetsView(DatasetService datasets, CurrentUser currentUser) {
        this.datasets = datasets;
        this.user = currentUser.get();
        setSizeFull();

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(".csv", ".xlsx", ".xls");
        upload.setMaxFileSize(50 * 1024 * 1024);
        upload.setDropLabel(new Paragraph("Drop a CSV or Excel file here (max 50 MB)"));
        upload.addSucceededListener(e -> {
            try {
                Dataset ds = datasets.upload(user, e.getFileName(), buffer.getInputStream());
                notify("Loaded " + ds.getRowCount() + " rows into table " + ds.getTableName(), NotificationVariant.LUMO_SUCCESS);
                upload.clearFileList();
                refresh();
            } catch (Exception ex) {
                notify("Could not load file: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        upload.addFileRejectedListener(e -> notify(e.getErrorMessage(), NotificationVariant.LUMO_ERROR));

        grid.addColumn(Dataset::getName).setHeader("File").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(Dataset::getTableName).setHeader("Table").setAutoWidth(true);
        grid.addColumn(Dataset::getRowCount).setHeader("Rows").setAutoWidth(true);
        grid.addColumn(Dataset::getColumnCount).setHeader("Columns").setAutoWidth(true);
        grid.addColumn(d -> FMT.format(d.getUploadedAt())).setHeader("Uploaded").setAutoWidth(true);
        grid.addComponentColumn(this::actions).setAutoWidth(true).setFlexGrow(0);
        grid.setSizeFull();

        add(new H2("Your datasets"), upload, empty, grid);
        expand(grid);
        refresh();
    }

    private HorizontalLayout actions(Dataset ds) {
        Button view = new Button("View", e -> UI.getCurrent().navigate(DatasetDetailView.class, ds.getId()));
        view.addThemeVariants(ButtonVariant.LUMO_SMALL);

        Button ask = new Button("Ask AI", e -> UI.getCurrent().navigate(ChatView.class, ds.getId()));
        ask.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(ds));
        delete.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        delete.setAriaLabel("Delete");

        return new HorizontalLayout(view, ask, delete);
    }

    private void confirmDelete(Dataset ds) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Delete " + ds.getName() + "?");
        dialog.add(new Paragraph("This removes the data and its chat history."));
        Button cancel = new Button("Cancel", e -> dialog.close());
        Button confirm = new Button("Delete", e -> {
            datasets.delete(ds);
            dialog.close();
            refresh();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void refresh() {
        List<Dataset> items = datasets.list(user);
        grid.setItems(items);
        empty.setVisible(items.isEmpty());
        grid.setVisible(!items.isEmpty());
    }

    private static void notify(String msg, NotificationVariant variant) {
        Notification.show(msg, 5000, Notification.Position.BOTTOM_START).addThemeVariants(variant);
    }
}
