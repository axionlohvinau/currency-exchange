import { requestApi, sendForm } from "./api.js";

const statusElement = document.querySelector("#status");
const dataOutput = document.querySelector("#data-output");
const exchangeResult = document.querySelector("#exchange-result");

const currenciesButton = document.querySelector("#currencies-button");
const ratesButton = document.querySelector("#rates-button");
const exchangeForm = document.querySelector("#exchange-form");

function showStatus(message, isError = false) {
    statusElement.textContent = message;
    statusElement.dataset.error = String(isError);
}

async function showData(path, button) {
    const container = document.querySelector(
        path === "currencies"
            ? "#currencies-table"
            : "#rates-table"
    );

    button.disabled = true;
    showStatus("Загрузка…");

    try {
        const data = await requestApi(path);

        renderTable(data, container);
        showStatus(`Загружено записей: ${data.length}`);
    } catch (error) {
        showStatus(error.message, true);
    } finally {
        button.disabled = false;
    }
}

currenciesButton.addEventListener("click", () => {
    showData("currencies", currenciesButton);
});

ratesButton.addEventListener("click", () => {
    showData("exchangeRates", ratesButton);
});

exchangeForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const button = exchangeForm.querySelector("button");

    button.disabled = true;
    exchangeResult.textContent = "";

    showStatus("Рассчитываем…");

    try {
        const formData = new FormData(exchangeForm);
        const params = new URLSearchParams(formData);

        const result = await requestApi(`exchange?${params}`);

        exchangeResult.textContent =
            `${result.amount} ${result.baseCurrency.code} = ` +
            `${result.convertedAmount} ${result.targetCurrency.code}`;

        showStatus(`Курс: ${result.rate}`);
    } catch (error) {
        showStatus(error.message, true);
    } finally {
        button.disabled = false;
    }
});

async function loadCurrencyOptions() {
    const currencies = await requestApi("currencies");

    const selects = document.querySelectorAll("[data-currency-select]");

    for (const select of selects) {
        const previousValue = select.value;

        const placeholder = new Option("Выберите валюту", "");
        select.replaceChildren(placeholder);

        for (const currency of currencies) {
            const option = new Option(
                `${currency.code} — ${currency.name}`,
                currency.code
            );

            select.add(option);
        }

        const previousStillExists = currencies.some(
            (currency) => currency.code === previousValue
        );

        if (previousStillExists) {
            select.value = previousValue;
        }
    }
}

function bindForm(
    formId,
    action,
    successMessage,
    { reset = false, refreshCurrencies = false } = {}
) {
    const form = document.querySelector(`#${formId}`);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();

        const button = form.querySelector('button[type="submit"]');

        button.disabled = true;
        dataOutput.hidden = true;
        detailsSection.hidden = true;

        showStatus("Выполняем запрос…");

        try {
            const formData = new FormData(form);
            const result = await action(formData);

            renderTable([result], dataOutput);

            dataOutput.hidden = false;
            detailsSection.hidden = false;

            showStatus(successMessage);

            if (reset) {
                form.reset();
            }

            if (refreshCurrencies) {
                try {
                    await loadCurrencyOptions();
                } catch {
                    showStatus(
                        "Валюта сохранена, но обновить списки не удалось. Обновите страницу.",
                        true
                    );
                }
            }
        } catch (error) {
            showStatus(error.message, true);
        } finally {
            button.disabled = false;
        }
    });
}

bindForm(
    "currency-form",
    (formData) => sendForm("currencies", "POST", formData),
    "Валюта добавлена.",
    {
        reset: true,
        refreshCurrencies: true
    }
);

bindForm(
    "rate-form",
    (formData) => sendForm("exchangeRates", "POST", formData),
    "Курс добавлен.",
    {
        reset: true
    }
);

bindForm(
    "update-rate-form",
    (formData) => {
        const baseCode = formData.get("baseCurrencyCode");
        const targetCode = formData.get("targetCurrencyCode");

        const body = new URLSearchParams({
            rate: formData.get("rate")
        });

        return sendForm(
            `exchangeRate/${baseCode}${targetCode}`,
            "PATCH",
            body
        );
    },
    "Курс обновлён."
);

bindForm(
    "find-currency-form",
    (formData) => {
        const code = formData.get("code");

        return requestApi(`currency/${encodeURIComponent(code)}`);
    },
    "Валюта найдена."
);

bindForm(
    "find-rate-form",
    (formData) => {
        const baseCode = formData.get("baseCurrencyCode");
        const targetCode = formData.get("targetCurrencyCode");

        return requestApi(`exchangeRate/${baseCode}${targetCode}`);
    },
    "Курс найден."
);

loadCurrencyOptions().catch(() => {
    showStatus(
        "Не удалось загрузить валюты. Проверьте сервер и обновите страницу.",
        true
    );
});

const navigationButtons = document.querySelectorAll("[data-panel]");
const panels = document.querySelectorAll(".panel");
const detailsSection = document.querySelector("#details-section");

for (const button of navigationButtons) {
    button.addEventListener("click", () => {
        const selectedPanel = button.dataset.panel;

        for (const panel of panels) {
            panel.hidden = panel.id !== selectedPanel;
        }

        for (const navigationButton of navigationButtons) {
            const isSelected =
                navigationButton.dataset.panel === selectedPanel;

            navigationButton.setAttribute(
                "aria-pressed",
                String(isSelected)
            );
        }

        detailsSection.hidden = true;
        showStatus("");
    });
}

function renderTable(records, container) {
    container.replaceChildren();

    if (records.length === 0) {
        const message = document.createElement("p");

        message.className = "empty-message";
        message.textContent = "Записей пока нет.";

        container.append(message);
        return;
    }

    const isRate = "baseCurrency" in records[0];

    const columns = isRate
        ? [
            { title: "ID", value: (record) => record.id },
            {
                title: "Из валюты",
                value: (record) => record.baseCurrency.code
            },
            {
                title: "В валюту",
                value: (record) => record.targetCurrency.code
            },
            { title: "Курс", value: (record) => record.rate }
        ]
        : [
            { title: "ID", value: (record) => record.id },
            { title: "Код", value: (record) => record.code },
            { title: "Название", value: (record) => record.name },
            { title: "Символ", value: (record) => record.sign }
        ];

    const table = document.createElement("table");
    const head = document.createElement("thead");
    const headerRow = document.createElement("tr");
    const body = document.createElement("tbody");

    for (const column of columns) {
        const cell = document.createElement("th");

        cell.scope = "col";
        cell.textContent = column.title;

        headerRow.append(cell);
    }

    head.append(headerRow);

    for (const record of records) {
        const row = document.createElement("tr");

        for (const column of columns) {
            const cell = document.createElement("td");

            cell.textContent = String(column.value(record));
            row.append(cell);
        }

        body.append(row);
    }

    table.append(head, body);
    container.append(table);
}