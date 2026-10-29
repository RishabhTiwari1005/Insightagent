package com.insightagent.ui;

import com.insightagent.data.QueryResult;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

import java.util.List;

/** Grid with columns built at runtime from any query result. */
public final class ResultGrid {

    private ResultGrid() {
    }

    public static Grid<List<String>> of(QueryResult result) {
        Grid<List<String>> grid = new Grid<>();
        for (int i = 0; i < result.columns().size(); i++) {
            final int idx = i;
            grid.addColumn(row -> row.get(idx))
                    .setHeader(result.columns().get(i))
                    .setAutoWidth(true)
                    .setResizable(true)
                    .setSortable(true);
        }
        grid.setItems(result.rows());
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        grid.setWidthFull();
        grid.setHeight(Math.min(360, 40 + result.rows().size() * 34) + "px");
        return grid;
    }
}
