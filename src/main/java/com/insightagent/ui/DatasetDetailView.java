package com.insightagent.ui;

import com.insightagent.data.ColumnInfo;
import com.insightagent.data.DatasetService;
import com.insightagent.domain.AppUser;
import com.insightagent.domain.Dataset;
import com.insightagent.security.CurrentUser;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;

import java.util.List;

@Route(value = "dataset", layout = MainLayout.class)
@PermitAll
public class DatasetDetailView extends VerticalLayout implements HasUrlParameter<Long>, HasDynamicTitle {

    private final DatasetService datasets;
    private final AppUser user;
    private String title = "Dataset | InsightAgent";

    public DatasetDetailView(DatasetService datasets, CurrentUser currentUser) {
        this.datasets = datasets;
        this.user = currentUser.get();
    }

    @Override
    public void setParameter(BeforeEvent event, Long id) {
        Dataset ds = datasets.get(id, user).orElse(null);
        if (ds == null) {
            event.rerouteTo(DatasetsView.class);
            return;
        }
        title = ds.getName() + " | InsightAgent";
        removeAll();

        Paragraph stats = new Paragraph(ds.getRowCount() + " rows, " + ds.getColumnCount()
                + " columns, table \"" + ds.getTableName() + "\"");
        stats.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        Button ask = new Button("Ask questions about this data",
                e -> UI.getCurrent().navigate(ChatView.class, ds.getId()));
        ask.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new H2(ds.getName()), stats, ask);

        try {
            List<ColumnInfo> schema = datasets.schema(ds);
            Grid<ColumnInfo> schemaGrid = new Grid<>();
            schemaGrid.addColumn(ColumnInfo::name).setHeader("Column");
            schemaGrid.addColumn(ColumnInfo::type).setHeader("Type");
            schemaGrid.setItems(schema);
            schemaGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
            schemaGrid.setAllRowsVisible(true);
            schemaGrid.setMaxWidth("600px");

            add(new H3("Schema"), schemaGrid, new H3("Preview (first 50 rows)"), ResultGrid.of(datasets.preview(ds, 50)));
        } catch (Exception e) {
            add(new Paragraph("Could not read dataset: " + e.getMessage()));
        }
    }

    @Override
    public String getPageTitle() {
        return title;
    }
}
