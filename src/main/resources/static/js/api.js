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

    _parseError(text) {
        if (!text) return {};
        try { return cqParseJsonBody(text); } catch { return {}; }
    },

    _checkAuth(status) {
        if (status === 401 || status === 403) {
            this._failSession('Сессия истекла или нет доступа. Войдите снова.');
        }
    },

    async _send(method, path, body) {
        const init = { method, headers: this.headers() };
        if (body !== undefined) init.body = JSON.stringify(body);
        const res = await fetch(this.base + path, init);
        const text = await res.text();
        this._checkAuth(res.status);
        if (!res.ok) {
            const err = this._parseError(text);
            throw new Error(err.error || 'Ошибка запроса');
        }
        return text.trim() ? cqParseJsonBody(text) : null;
    },

    get(path) { return this._send('GET', path); },
    post(path, body) { return this._send('POST', path, body ?? {}); },
    put(path, body) { return this._send('PUT', path, body ?? {}); },
    delete(path) { return this._send('DELETE', path); },

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
