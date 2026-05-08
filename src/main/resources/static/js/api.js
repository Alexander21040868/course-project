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

    _failSession(msg) {
        localStorage.clear();
        window.location.href = '/index.html';
        throw new Error(msg);
    },

    _handleAuthError(err) {
        if (err && err.error && err.error.includes('не найден')) {
            this._failSession('Пользователь не найден. Войдите снова.');
        }
    },

    async get(path) {
        const res = await fetch(this.base + path, { headers: this.headers() });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
        if (res.status === 404) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            throw new Error(err.error || 'Не найдено. Перезапустите приложение после mvn package.');
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            this._handleAuthError(err);
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
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            this._handleAuthError(err);
            throw new Error(err.error || 'Ошибка запроса');
        }
        return text.trim() ? cqParseJsonBody(text) : null;
    },

    async put(path, body) {
        const res = await fetch(this.base + path, {
            method: 'PUT',
            headers: this.headers(),
            body: JSON.stringify(body)
        });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
        if (!res.ok) {
            let err = {};
            try {
                err = cqParseJsonBody(text);
            } catch {
                err = {};
            }
            this._handleAuthError(err);
            throw new Error(err.error || 'Ошибка запроса');
        }
        return text.trim() ? cqParseJsonBody(text) : null;
    },

    async delete(path) {
        const res = await fetch(this.base + path, {
            method: 'DELETE',
            headers: this.headers()
        });
        const text = await res.text();
        if (res.status === 401 || res.status === 403) {
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
        if (!res.ok) {
            let err = {};
            try {
                err = text ? cqParseJsonBody(text) : {};
            } catch {
                err = {};
            }
            this._handleAuthError(err);
            throw new Error(err.error || 'Ошибка удаления');
        }
    },

    async getBlob(path) {
        const h = {};
        const t = this.token();
        if (t) h['Authorization'] = 'Bearer ' + t;
        const res = await fetch(this.base + path, { headers: h });
        if (res.status === 401 || res.status === 403) {
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
        if (!res.ok) throw new Error('Ошибка загрузки файла');
        return res.blob();
    }
};
