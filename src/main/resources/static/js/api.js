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
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (res.status === 404) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.error || 'Не найдено. Выполните mvn package и перезапустите Spring Boot (старый процесс на порту 8080 нужно остановить).');
        }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return res.json();
    },

    async post(path, body) {
        const res = await fetch(this.base + path, {
            method: 'POST',
            headers: this.headers(),
            body: JSON.stringify(body)
        });
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return res.json();
    },

    async delete(path) {
        const res = await fetch(this.base + path, {
            method: 'DELETE',
            headers: this.headers()
        });
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
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
        if (res.status === 401 || res.status === 403) {
            localStorage.clear();
            window.location.href = '/index.html';
            return;
        }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            if (this._handleAuthError(err)) return;
            throw new Error(err.error || 'Ошибка запроса');
        }
        return res.json();
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
