package com.insightagent.ui;

import com.insightagent.agent.AgentAnswer;
import com.insightagent.agent.AnalystAgent;
import com.insightagent.data.DatasetService;
import com.insightagent.data.DuckDbService;
import com.insightagent.data.QueryResult;
import com.insightagent.data.SqlGuard;
import com.insightagent.domain.AppUser;
import com.insightagent.domain.ChatMessage;
import com.insightagent.domain.ChatMessage.Role;
import com.insightagent.domain.Dataset;
import com.insightagent.repo.ChatMessageRepository;
import com.insightagent.security.CurrentUser;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Route(value = "chat", layout = MainLayout.class)
@PageTitle("Ask AI | InsightAgent")
@PermitAll
public class ChatView extends VerticalLayout implements HasUrlParameter<Long> {

    private final AnalystAgent agent;
    private final DatasetService datasets;
    private final DuckDbService duck;
    private final SqlGuard guard;
    private final ChatMessageRepository history;
    private final AppUser user;

    private final ComboBox<Dataset> picker = new ComboBox<>("Dataset");
    private final VerticalLayout messages = new VerticalLayout();
    private final Scroller scroller = new Scroller(messages);
    private final TextField input = new TextField();
    private final Button send = new Button("Ask");
    private final ProgressBar progress = new ProgressBar();

    public ChatView(AnalystAgent agent, DatasetService datasets, DuckDbService duck,
                    SqlGuard guard, ChatMessageRepository history, CurrentUser currentUser) {
        this.agent = agent;
        this.datasets = datasets;
        this.duck = duck;
        this.guard = guard;
        this.history = history;
        this.user = currentUser.get();
        setSizeFull();

        List<Dataset> items = datasets.list(user);
        picker.setItems(items);
        picker.setItemLabelGenerator(Dataset::getName);
        picker.setWidth("360px");
        picker.addValueChangeListener(e -> loadHistory(e.getValue()));

        messages.setPadding(false);
        messages.setSpacing(true);
        scroller.setSizeFull();
        scroller.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-m)");

        input.setPlaceholder("e.g. Which region had the highest revenue in Q1, and why did March drop?");
        input.setWidthFull();
        send.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        send.addClickShortcut(Key.ENTER);
        send.addClickListener(e -> submit());

        progress.setIndeterminate(true);
        progress.setVisible(false);

        HorizontalLayout bar = new HorizontalLayout(input, send);
        bar.setWidthFull();
        bar.expand(input);

        if (items.isEmpty()) {
            add(new Paragraph("Upload a dataset first."), new RouterLink("Go to datasets", DatasetsView.class));
            return;
        }
        add(picker, scroller, progress, bar);
        expand(scroller);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter Long datasetId) {
        if (datasetId != null) {
            datasets.get(datasetId, user).ifPresent(picker::setValue);
        }
        if (picker.getValue() == null) {
            picker.getListDataView().getItems().findFirst().ifPresent(picker::setValue);
        }
    }

    private void loadHistory(Dataset ds) {
        messages.removeAll();
        if (ds == null) return;
        List<ChatMessage> past = history.findByDatasetOrderByCreatedAtAsc(ds);
        if (past.isEmpty()) {
            messages.add(hint("Ask anything about " + ds.getName() + ". The agent writes SQL, runs it, fixes its own mistakes and explains the result."));
        }
        for (ChatMessage m : past) {
            messages.add(m.getRole() == Role.USER
                    ? userBubble(m.getContent())
                    : answerBubble(ds, m.getContent(), m.getSqlQuery(), null));
        }
        scrollToBottom();
    }

    private void submit() {
        String question = input.getValue().trim();
        Dataset ds = picker.getValue();
        if (question.isEmpty() || ds == null) return;

        input.clear();
        setBusy(true);
        messages.add(userBubble(question));
        history.save(new ChatMessage(ds, Role.USER, question, null));

        VerticalLayout stepList = new VerticalLayout();
        stepList.setPadding(false);
        stepList.setSpacing(false);
        Details steps = new Details("Agent is working...", stepList);
        steps.setOpened(true);
        messages.add(steps);
        scrollToBottom();

        UI ui = UI.getCurrent();
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return agent.ask(user, ds, question,
                                step -> ui.access(() -> {
                                    stepList.add(stepLine(step));
                                    scrollToBottom();
                                }));
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .whenComplete((answer, error) -> ui.access(() -> {
                    setBusy(false);
                    if (error != null) {
                        steps.setSummaryText("Agent failed");
                        messages.add(errorBubble(friendlyError(error)));
                    } else {
                        steps.setSummaryText(summary(answer));
                        steps.setOpened(false);
                        history.save(new ChatMessage(ds, Role.ASSISTANT, answer.answer(), answer.sql()));
                        messages.add(answerBubble(ds, answer.answer(), answer.sql(), answer.result()));
                    }
                    scrollToBottom();
                }));
    }

    private static String summary(AgentAnswer a) {
        String s = "Agent steps: " + a.queries() + " queries";
        if (a.failures() > 0) s += ", " + a.failures() + " self-corrected";
        return s + ", " + String.format("%.1f", a.millis() / 1000.0) + "s";
    }

    // ---------- message components ----------

    private Component userBubble(String text) {
        Div d = bubble(text);
        d.getStyle().set("align-self", "flex-end")
                .set("background", "var(--lumo-primary-color-10pct)");
        return d;
    }

    private Component answerBubble(Dataset ds, String text, String sql, QueryResult result) {
        Div d = bubble(text);
        d.getStyle().set("background", "var(--lumo-contrast-5pct)").set("max-width", "100%");

        if (sql != null) {
            Pre code = new Pre(sql);
            code.getStyle().set("white-space", "pre-wrap").set("margin", "0");
            d.add(new Details("SQL used", code));
        }
        if (result != null && !result.rows().isEmpty()) {
            d.add(ResultGrid.of(result));
        } else if (result == null && sql != null) {
            // answer loaded from history: re-run its SQL on demand
            Button rerun = new Button("Show result table");
            rerun.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Div slot = new Div(rerun);
            rerun.addClickListener(e -> {
                slot.removeAll();
                try {
                    QueryResult r = duck.query(datasets.path(ds), guard.check(sql), 200);
                    slot.add(ResultGrid.of(r));
                } catch (Exception ex) {
                    slot.add(new Span("Could not re-run: " + ex.getMessage()));
                }
            });
            d.add(slot);
        }
        return d;
    }

    private Component errorBubble(String text) {
        Div d = bubble(text);
        d.getStyle().set("background", "var(--lumo-error-color-10pct)")
                .set("color", "var(--lumo-error-text-color)");
        return d;
    }

    private static Div bubble(String text) {
        Paragraph p = new Paragraph(text);
        p.getStyle().set("white-space", "pre-wrap").set("margin", "0");
        Div d = new Div(p);
        d.getStyle().set("padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("max-width", "80%")
                .set("display", "flex").set("flex-direction", "column")
                .set("gap", "var(--lumo-space-s)");
        return d;
    }

    private static Component stepLine(String step) {
        Pre line = new Pre("> " + step);
        line.getStyle().set("margin", "0").set("white-space", "pre-wrap")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");
        return line;
    }

    private static Component hint(String text) {
        Paragraph p = new Paragraph(text);
        p.getStyle().set("color", "var(--lumo-secondary-text-color)");
        return p;
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        send.setEnabled(!busy);
        input.setEnabled(!busy);
        picker.setEnabled(!busy);
        if (!busy) input.focus();
    }

    private void scrollToBottom() {
        scroller.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private static String friendlyError(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        String msg = String.valueOf(root.getMessage());
        if (msg.contains("401") || msg.toLowerCase().contains("api key")) {
            return "The LLM rejected the request. Check OPENAI_API_KEY (and OPENAI_BASE_URL if you use Groq).";
        }
        return "Something went wrong: " + msg;
    }
}
