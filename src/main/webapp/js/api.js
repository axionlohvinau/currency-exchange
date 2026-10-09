export async function requestApi(path, options = {}) {
    const response = await fetch(path, options);
    const text = await response.text();

    let data;

    try {
        data = JSON.parse(text);
    } catch {
        throw new Error(
            `Сервер вернул неожиданный ответ. HTTP ${response.status}`
        );
    }

    if (!response.ok) {
        throw new Error(
            data.message || `Ошибка HTTP ${response.status}`
        );
    }

    return data;
}

export function sendForm(path, method, formData) {
    return requestApi(path, {
        method,
        headers: {
            "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8"
        },
        body: new URLSearchParams(formData)
    });
}