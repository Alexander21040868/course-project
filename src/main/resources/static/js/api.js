/**
 * Иногда в теле ответа после основного JSON попадает лишний текст (два объекта подряд, хвост от прокси/расширения).
 * Тогда JSON.parse падает с "Unexpected non-whitespace character after JSON".
 */
function cqExtractFirstJsonValue(text) {
    const s = String(text).trimStart();
    if (!s.length) return null;
    const stack = [];
    let inStr = false;
    let escape = false;
    for (let i = 0; i < s.length; i++) {
        const c = s[i];
        if (escape) {
            escape = false;
            continue;
        }
        if (inStr) {
            if (c === '\\') {
                escape = true;
                continue;
            }
            if (c === '"') inStr = false;
            continue;
        }
        if (c === '"') {
            inStr = true;
            continue;
        }
        if (c === '{' || c === '[') {
            stack.push(c);
            continue;
        }
        if (c === '}' || c === ']') {
            if (!stack.length) return null;
            const top = stack.pop();
            if ((c === '}' && top !== '{') || (c === ']' && top !== '[')) return null;
            if (!stack.length) {
                try {
                    return JSON.parse(s.slice(0, i + 1));
                } catch {
                    return null;
                }
            }
        }
    }
    return null;
}

function cqParseJsonBody(text) {
    const raw = String(text);
    try {
        return JSON.parse(raw);
    } catch (e) {
        const recovered = cqExtractFirstJsonValue(raw);
        if (recovered !== null) {
            console.warn('CQ API: в ответе после JSON был лишний текст; используется первый JSON-объект.');
            return recovered;
        }
        throw e;
    }
}

const API = {
    base: '/api',

    token() {
        return localStorage.getItem('cq_token');
    },

    headers() {
        const h = { 'Content-Type': 'application/json' };
        const t = this.token();
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    },

    _handleAuthError(err) {
        if (err && err.error && err.error.includes('не найден')) {
            localStorage.clear();
            window.location.href = '/index.html';
            return true;
        }
        return false;
    },

    async get(path) {
        const res = await fetch(this.base + path, { headers: this.headers() });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (res.status === 404) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            throw new Error(err.error || 'Не найдено. Выполните mvn package и перезапустите Spring Boot (старый процесс на порту 8080 нужно остановить).');
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return cqParseJsonBody(text);
    },

    async post(path, body) {
        const res = await fetch(this.base + path, {
            method: 'POST',
            headers: this.headers(),
            body: JSON.stringify(body)
        });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return cqParseJsonBody(text);
    },

    async delete(path) {
        const res = await fetch(this.base + path, {
            method: 'DELETE',
            headers: this.headers()
        });
        const text = await res.text();
        if (!res.ok) {
            let err = {};
            try {
                err = text ? cqParseJsonBody(text) : {};
            } catch {
                err = {};
            }
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка удаления');
        }
    },

    async put(path, body) {
        const res = await fetch(this.base + path, {
            method: 'PUT',
            headers: this.headers(),
            body: JSON.stringify(body)
        });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return cqParseJsonBody(text);
    },

    /** Бинарный ответ (PDF и т.п.) */
    async getBlob(path) {
        const h = {};
        const t = this.token();
        if (t) h['Authorization'] = 'Bearer ' + t;
        const res = await fetch(this.base + path, { headers: h });
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return null;
        }
        if (!res.ok) throw new Error('Ошибка загрузки файла');
        return res.blob();
    }
};
