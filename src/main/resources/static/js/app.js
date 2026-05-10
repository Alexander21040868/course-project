(function () {
    const KEY = 'cq_mute';
    window.CQSound = {
        _ctx: null,
        muted() { return localStorage.getItem(KEY) === '1'; },
        setMuted(m) { localStorage.setItem(KEY, m ? '1' : '0'); },
        _ensureCtx() {
            if (!this._ctx) this._ctx = new (window.AudioContext || window.webkitAudioContext)();
            return this._ctx;
        },
        async _resume() {
            const c = this._ensureCtx();
            if (c.state === 'suspended') await c.resume();
        },
        _tone(freq, dur, type = 'square', vol = 0.07) {
            if (this.muted()) return;
            const c = this._ensureCtx();
            const o = c.createOscillator();
            const g = c.createGain();
            o.type = type;
            o.frequency.value = freq;
            const t0 = c.currentTime;
            g.gain.setValueAtTime(vol, t0);
            g.gain.exponentialRampToValueAtTime(0.001, t0 + dur);
            o.connect(g);
            g.connect(c.destination);
            o.start(t0);
            o.stop(t0 + dur);
        },
        async success() {
            await this._resume();
            this._tone(523, 0.1);
            setTimeout(() => this._tone(784, 0.12), 70);
        },
        async fail() {
            await this._resume();
            this._tone(146, 0.22, 'sawtooth', 0.055);
        },
        async achievement() {
            await this._resume();
            this._tone(660, 0.08);
            setTimeout(() => this._tone(988, 0.1), 60);
        },
        async levelUp() {
            await this._resume();
            this._tone(392, 0.1);
            setTimeout(() => this._tone(523, 0.1), 90);
            setTimeout(() => this._tone(784, 0.18), 190);
        }
    };
})();

document.addEventListener('DOMContentLoaded', () => {
    if (!localStorage.getItem('cq_token')) { window.location.href = '/index.html'; return; }

    const state = {
        currentView: 'quests', currentLessonId: null, currentTaskId: null, profile: null,
        lastSubmitOutput: '', libraryArticles: [], selectedArticleId: null,
        lessonPage: 0, catalogPage: 0,
        questTeachers: null, selectedQuestTeacher: null
    };
    const $ = id => document.getElementById(id);

    function normalizeDatetimeLocalForApi(v) {
        if (v == null) return null;
        let s = String(v).trim();
        if (!s) return null;
        const junkYear = /^(\d{4})\d{2}-(\d{2}-\d{2}T\d{2}:\d{2})$/;
        if (junkYear.test(s)) s = s.replace(junkYear, '$1-$2');
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s)) return s + ':00';
        if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/.test(s)) return s.replace(/\.\d+/, '').slice(0, 19);
        return s;
    }

    async function refreshTaskChallengeSelect(preferredId) {
        const sel = $('taskChallengeSelect');
        if (!sel) return;
        const want = preferredId != null && preferredId !== '' ? String(preferredId) : sel.value;
        try {
            if (!state.profile) await loadProfile();
            const me = state.profile && state.profile.username;
            const list = await API.get('/admin/challenges') || [];
            const now = Date.now();
            const opts = ['<option value="">— нет —</option>'];
            for (const c of list) {
                if (!me || c.createdByName !== me) continue;
                const endMs = c.endTime ? new Date(c.endTime).getTime() : NaN;
                if (Number.isFinite(endMs) && endMs < now) continue;
                opts.push(`<option value="${c.id}">${esc(c.title || '')} #${c.id}</option>`);
            }
            sel.innerHTML = opts.join('');
            if (want && [...sel.options].some(o => o.value === want)) sel.value = want;
            else sel.value = '';
        } catch (e) { console.error(e); }
    }

    function syncMuteBtn() {
        const b = $('muteBtn');
        if (b) b.textContent = CQSound.muted() ? '🔇 Тишина' : '🔊 Звук';
    }
    syncMuteBtn();
    $('muteBtn').addEventListener('click', () => {
        CQSound.setMuted(!CQSound.muted());
        syncMuteBtn();
    });

    function applyTheme(light) {
        document.body.classList.toggle('light', light);
        localStorage.setItem('cq_theme', light ? 'light' : 'dark');
        const cmLink = $('cmThemeLight');
        if (cmLink) cmLink.media = light ? 'all' : 'none';
        $('themeToggle').textContent = light ? '🌙 Тёмная' : '☀️ Светлая';
        if (state.cm) state.cm.setOption('theme', light ? 'eclipse' : 'material-darker');
    }
    applyTheme(localStorage.getItem('cq_theme') === 'light');
    $('themeToggle').addEventListener('click', () => applyTheme(!document.body.classList.contains('light')));

    document.querySelectorAll('.nav-item[data-view]').forEach(btn =>
        btn.addEventListener('click', () => navigateTo(btn.dataset.view)));
    $('charCardLink').addEventListener('click', () => navigateTo('profile'));
    $('backToQuests').addEventListener('click', () => navigateTo('quests'));
    $('backToLesson').addEventListener('click', () => openLesson(state.currentLessonId));
    $('logoutBtn').addEventListener('click', () => { localStorage.clear(); window.location.href = '/index.html'; });

    document.querySelectorAll('.admin-tab').forEach(btn => btn.addEventListener('click', () => {
        document.querySelectorAll('.admin-tab').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.admin-panel').forEach(p => p.classList.remove('active'));
        btn.classList.add('active');
        $('atab-' + btn.dataset.atab).classList.add('active');
        if (btn.dataset.atab === 'students') loadGroup();
        if (btn.dataset.atab === 'content') { loadMyLessonsIndex(); refreshTaskChallengeSelect(); }
    }));

    let catalogTimer;
    $('catalogSearch').addEventListener('input', () => {
        clearTimeout(catalogTimer);
        catalogTimer = setTimeout(() => { state.catalogPage = 0; loadCatalog($('catalogSearch').value || '', 0); }, 300);
    });

    const titles = {
        quests:'Карта подземелий', library:'Библиотека', catalog:'Каталог задач', lesson:'Подземелье', task:'Битва',
        challenges:'Арена', leaderboard:'Лидерборд', achievements:'Зал славы',
        profile:'Личный кабинет', admin:'Штаб мастера'
    };

    function navigateTo(view) {
        state.currentView = view;
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        $('view-' + view).classList.add('active');
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
        const nb = document.querySelector(`.nav-item[data-view="${view}"]`);
        if (nb) nb.classList.add('active');
        $('viewTitle').textContent = titles[view] || '';

        if (view === 'quests') { loadLessons(); loadDailyTask(); }
        if (view === 'library') loadLibrary($('librarySearch').value || '');
        if (view === 'catalog') { state.catalogPage = 0; loadCatalog($('catalogSearch').value || '', 0); }
        if (view === 'achievements') loadAchievements();
        if (view === 'challenges') loadChallenges();
        if (view === 'leaderboard') loadLeaderboard();
        if (view === 'profile') loadFullProfile();
        if (view === 'admin') { loadMyLessonsIndex(); refreshTaskChallengeSelect(); }
    }

    function showView(view) {
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        $('view-' + view).classList.add('active');
    }

    async function loadProfile() {
        try {
            const p = await API.get('/profile');
            state.profile = p;
            state.questTeachers = null;
            $('charName').textContent = p.username;
            $('charRole').textContent = p.role === 'TEACHER' ? 'Мастер' : getTitle(p.level);
            $('xpText').textContent = p.rating + ' elo · ' + p.xp + ' xp';
            $('levelBadge').textContent = 'LVL ' + p.level;
            $('solvedStat').textContent = p.solvedCount;
            $('levelStat').textContent = p.level;
            const nextLvl = p.level * p.level * 100;
            const prevLvl = (p.level - 1) * (p.level - 1) * 100;
            const range = nextLvl - prevLvl;
            const pct = range > 0 ? Math.min(100, ((p.xp - prevLvl) / range) * 100) : 100;
            $('xpFill').style.width = Math.max(5, pct) + '%';
            if (p.role === 'TEACHER') $('adminNav').style.display = '';
            $('avatar').textContent = p.role === 'TEACHER' ? '🧙' : '⚔';
            refreshNotifCount();
        } catch (e) { console.error(e); }
    }

    async function refreshNotifCount() {
        try {
            const [c, ic] = await Promise.all([
                API.get('/notifications/count').catch(() => ({ unread: 0 })),
                API.get('/group-invites/incoming/count').catch(() => ({ count: 0 }))
            ]);
            const n = (c?.unread || 0) + (ic?.count || 0);
            const b = $('notifBadge');
            if (n > 0) { b.textContent = n > 9 ? '9+' : n; b.style.display = ''; }
            else b.style.display = 'none';
        } catch (e) {}
    }

    function inviteHtml(inv) {
        const groupTag = inv.groupName ? ` → ${esc(inv.groupName)}` : '';
        return `<div class="notif-item invite" data-iid="${inv.id}">
            <strong>📩 Приглашение от ${esc(inv.teacherUsername)}${groupTag}</strong>
            <p>Учитель приглашает вас в свою группу.</p>
            <div class="invite-actions">
                <button class="btn-admin" data-act="accept">Принять</button>
                <button class="btn-admin btn-admin-ghost" data-act="decline">Отклонить</button>
            </div>
        </div>`;
    }

    async function openNotifDropdown() {
        const dd = $('notifDropdown');
        try {
            const [invites, list] = await Promise.all([
                API.get('/group-invites/incoming').catch(() => []),
                API.get('/notifications').catch(() => [])
            ]);
            const sections = [];
            if (invites && invites.length) {
                sections.push(`<div class="notif-section-title">Приглашения</div>` + invites.map(inviteHtml).join(''));
            }
            if (list && list.length) {
                sections.push(`<div class="notif-section-title">Уведомления</div>` + list.map(n => `
                    <div class="notif-item" data-nid="${n.id}">
                        <strong>${esc(n.title)}</strong>
                        <p>${esc(n.message)}</p>
                    </div>`).join(''));
            }
            dd.innerHTML = sections.length ? sections.join('') : '<div class="notif-empty">Нет новых уведомлений</div>';

            dd.querySelectorAll('.notif-item.invite .invite-actions button').forEach(btn => {
                btn.addEventListener('click', async (ev) => {
                    ev.stopPropagation();
                    const item = btn.closest('.notif-item.invite');
                    const id = item.dataset.iid;
                    try {
                        await API.post('/group-invites/' + id + '/' + btn.dataset.act, {});
                        item.remove();
                        refreshNotifCount();
                    } catch (e) { alert(e.message); }
                });
            });
            dd.querySelectorAll('.notif-item:not(.invite)').forEach(el => el.addEventListener('click', async () => {
                try {
                    await fetch(API.base + '/notifications/' + el.dataset.nid + '/read', {
                        method: 'PUT', headers: API.headers()
                    });
                    el.remove();
                    refreshNotifCount();
                    if (!$('notifDropdown').querySelector('.notif-item')) $('notifDropdown').innerHTML = '<div class="notif-empty">Нет новых уведомлений</div>';
                } catch (e) { console.error(e); }
            }));
            dd.style.display = 'block';
        } catch (e) { dd.innerHTML = '<div class="notif-empty">Ошибка загрузки</div>'; dd.style.display = 'block'; }
    }

    $('notifDropdown').addEventListener('click', e => e.stopPropagation());

    $('notifBell').addEventListener('click', e => {
        e.stopPropagation();
        const dd = $('notifDropdown');
        if (dd.style.display === 'block') { dd.style.display = 'none'; return; }
        openNotifDropdown();
    });
    document.addEventListener('click', () => { $('notifDropdown').style.display = 'none'; });

    let libTimer;
    $('librarySearch').addEventListener('input', () => {
        clearTimeout(libTimer);
        libTimer = setTimeout(() => loadLibrary($('librarySearch').value), 300);
    });

    function normalizeIdSearchQuery(s) {
        const t = (s || '').trim();
        return t.replace(/^#(?=\d+$)/, '');
    }

    async function loadLibrary(q) {
        const listEl = $('libraryList');
        const artEl = $('libraryArticle');
        listEl.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const qNorm = normalizeIdSearchQuery(q);
            const path = '/articles' + (qNorm ? '?q=' + encodeURIComponent(qNorm) : '');
            const looksArticleId = /^#?\d+$/.test((q || '').trim());
            const items = await API.get(path) || [];
            state.libraryArticles = items;
            if (!items.length) {
                const hint = looksArticleId
                    ? `Статьи с id ${qNorm} нет`
                    : 'Ничего не найдено';
                listEl.innerHTML = `<p class="empty-hint">${esc(hint)}</p>`;
                return;
            }
            listEl.innerHTML = items.map(a => `
                <button type="button" class="library-card ${state.selectedArticleId === a.id ? 'active' : ''}" data-aid="${a.id}">
                    <div class="library-card-meta">
                        <span class="lc-id" title="Номер для правки в «Управление»">#${a.id}</span>
                        <span class="lc-cat">${esc(a.category)}</span>
                    </div>
                    <div class="lc-title">${esc(a.title)}</div>
                </button>`).join('');
            listEl.querySelectorAll('.library-card').forEach(btn => btn.addEventListener('click', () => openArticle(+btn.dataset.aid)));
            if (looksArticleId && items.length === 1) {
                await openArticle(items[0].id);
            } else if (state.selectedArticleId && items.some(x => x.id === state.selectedArticleId)) {
                await openArticle(state.selectedArticleId);
            } else {
                artEl.innerHTML = '<p class="empty-hint">Выберите статью слева</p>';
                state.selectedArticleId = null;
            }
        } catch (e) { listEl.innerHTML = `<p class="empty-hint">${esc(e.message)}</p>`; }
    }

    async function openArticle(id) {
        state.selectedArticleId = id;
        $('libraryList').querySelectorAll('.library-card').forEach(c => c.classList.toggle('active', +c.dataset.aid === id));
        const artEl = $('libraryArticle');
        artEl.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const a = await API.get('/articles/' + id);
            const raw = mdFromApi(a.content);
            let html = esc(raw);
            if (typeof marked !== 'undefined' && raw) {
                try {
                    const out = marked.parse(raw, { breaks: true });
                    html = typeof out?.then === 'function' ? await out : String(out);
                } catch {
                    html = `<p>${esc(raw)}</p>`;
                }
            } else if (raw) {
                html = `<p>${esc(raw)}</p>`;
            }
            artEl.innerHTML = `
                <header class="library-article-bar">
                    <span class="lc-id-badge" title="Этот номер указывайте в «Управление» для правки">#${a.id}</span>
                    <h2 class="library-article-h">${esc(a.title)}</h2>
                </header>
                <div class="md-content">${html}</div>`;
        } catch (e) { artEl.innerHTML = `<p class="empty-hint">${esc(e.message)}</p>`; }
    }

    function getTitle(level) {
        if (level >= 16) return 'Архимаг';
        if (level >= 11) return 'Мастер';
        if (level >= 8)  return 'Кодер';
        if (level >= 5)  return 'Подмастерье';
        if (level >= 3)  return 'Ученик';
        return 'Новобранец';
    }

    async function loadFullProfile() {
        const el = $('profileContent');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const p = await API.get('/profile');
            state.profile = p;
            el.innerHTML = `
                <div class="profile-header">
                    <div class="profile-avatar">${p.role === 'TEACHER' ? '🧙' : '⚔'}</div>
                    <div class="profile-info">
                        <h2>${esc(p.username)}</h2>
                        <span class="profile-title">${p.role === 'TEACHER' ? 'Мастер подземелий' : getTitle(p.level)}</span>
                    </div>
                </div>
                <div class="stats-grid">
                    <div class="stat-card"><div class="stat-val">${p.rating}</div><div class="stat-label">Рейтинг ELO</div></div>
                    <div class="stat-card"><div class="stat-val">${p.xp}</div><div class="stat-label">Опыт (XP)</div></div>
                    <div class="stat-card"><div class="stat-val">${p.solvedCount}/${p.totalTasks}</div><div class="stat-label">Решено</div></div>
                    <div class="stat-card"><div class="stat-val streak-val">🔥 ${p.streak}</div><div class="stat-label">Streak (макс: ${p.maxStreak})</div></div>
                </div>
                <h3 class="section-title">📊 Прогресс по урокам</h3>
                <div class="progress-list">${(p.lessonsProgress || []).map(l => `
                    <div class="progress-item">
                        <div class="progress-item-head">
                            <span>${l.teacherUsername ? esc(l.teacherUsername) + ' — ' : ''}${l.dungeonOrder != null ? '#' + l.dungeonOrder + ' — ' : ''}${esc(l.title)}</span>
                            <span class="progress-nums">${l.solved}/${l.total}</span>
                        </div>
                        <div class="progress-bar"><div class="progress-fill" style="width:${l.total>0?Math.round(l.solved*100/l.total):0}%"></div></div>
                    </div>`).join('')}</div>
                <h3 class="section-title">📜 Последние решения</h3>
                <div class="history-list">${(p.recentSubmissions || []).length ? p.recentSubmissions.map(s => `
                    <div class="history-item">
                        <span class="history-status">${s.status === 'CORRECT' ? '✅' : '❌'}</span>
                        <span class="history-task">${esc(s.taskTitle)}</span>
                        <span class="history-time">${new Date(s.submittedAt).toLocaleString('ru')}</span>
                    </div>`).join('') : '<p style="color:var(--text-dim);font-size:12px">Ещё нет решений</p>'}</div>`;
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>Ошибка: ${esc(e.message)}</p></div>`; }
    }

    async function loadCatalog(search, page) {
        const pg = page ?? state.catalogPage;
        state.catalogPage = pg;
        const el = $('catalogList');
        const pagerEl = $('catalogPager');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const q = new URLSearchParams({ page: String(pg), size: '12' });
            const searchTrim = (search || '').trim();
            if (searchTrim) q.set('search', normalizeIdSearchQuery(searchTrim));
            const data = await API.get('/tasks?' + q.toString());
            const tasks = data.content ?? data;
            const totalPages = data.totalPages ?? 1;
            const looksTaskId = /^#?\d+$/.test(searchTrim);
            if (!tasks.length) {
                const hint = looksTaskId
                    ? `Нет задачи с id ${normalizeIdSearchQuery(searchTrim)}`
                    : 'Задачи не найдены';
                el.innerHTML = `<div class="empty-state"><p>${esc(hint)}</p></div>`;
                pagerEl.style.display = 'none';
                return;
            }
            el.innerHTML = tasks.map(t => `
                <div class="task-item" data-id="${t.id}">
                    <div class="task-id-badge">#${t.id}</div>
                    <div class="task-status-icon ${t.solved?'solved':'unsolved'}">${t.solved?'✅':'⬜'}</div>
                    <div class="task-info">
                        <h4>${esc(t.title)}</h4>
                        <span class="task-diff diff-${t.difficulty.toLowerCase()}">${diffLabel(t.difficulty)}</span>
                        ${t.authorUsername ? `<span class="task-author-tag">автор: ${esc(t.authorUsername)}</span>` : ''}
                    </div>
                    <span class="task-xp">+${t.xpReward} XP</span>
                </div>`).join('');
            el.querySelectorAll('.task-item').forEach(i =>
                i.addEventListener('click', () => openTask(+i.dataset.id)));
            if (totalPages > 1) {
                pagerEl.style.display = 'flex';
                pagerEl.innerHTML = `
                    <button type="button" class="pager-btn" ${pg <= 0 ? 'disabled' : ''} data-cp="${pg - 1}">← Назад</button>
                    <span class="pager-info">${pg + 1} / ${totalPages}</span>
                    <button type="button" class="pager-btn" ${pg >= totalPages - 1 ? 'disabled' : ''} data-cp="${pg + 1}">Вперёд →</button>`;
                pagerEl.querySelectorAll('[data-cp]').forEach(b => b.addEventListener('click', () => {
                    if (b.disabled) return;
                    loadCatalog(search, +b.dataset.cp);
                }));
            } else pagerEl.style.display = 'none';
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; pagerEl.style.display = 'none'; }
    }

    async function loadLeaderboard(sort = 'rating') {
        const el = $('leaderboardContent');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const board = await API.get('/leaderboard?sort=' + sort);
            if (!board.length) { el.innerHTML = '<div class="empty-state"><p>Пока нет данных</p></div>'; return; }

            const sortBtns = `<div class="lb-sort">
                <button class="lb-btn ${sort==='rating'?'active':''}" data-sort="rating">По рейтингу</button>
                <button class="lb-btn ${sort==='xp'?'active':''}" data-sort="xp">По XP</button>
            </div>`;

            el.innerHTML = sortBtns +
                '<table class="results-table leaderboard-table"><tr><th>#</th><th>Герой</th><th>ELO</th><th>XP</th><th>LVL</th><th>Решено</th></tr>' +
                board.map(r => {
                    const medal = r.rank <= 3 ? ['','🥇','🥈','🥉'][r.rank] : r.rank;
                    const cls = r.rank <= 3 ? ' class="top-row"' : '';
                    return `<tr${cls}><td>${medal}</td><td>${esc(r.username)}</td><td>${r.rating}</td><td>${r.xp}</td><td>${r.level}</td><td>${r.solvedCount}</td></tr>`;
                }).join('') + '</table>';

            el.querySelectorAll('.lb-btn').forEach(b =>
                b.addEventListener('click', () => loadLeaderboard(b.dataset.sort)));
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    async function loadDailyTask() {
        try {
            const t = await API.get('/daily-task');
            if (!t) return;
            $('dailyTaskCard').style.display = '';
            $('dailyTaskCard').innerHTML = `
                <div class="daily-inner" data-id="${t.id}">
                    <span class="daily-badge">⚡ ЗАДАЧА ДНЯ · 2x XP</span>
                    <h3>#${t.id} ${esc(t.title)}</h3>
                    <span class="task-diff diff-${t.difficulty.toLowerCase()}">${diffLabel(t.difficulty)}</span>
                    <span class="task-xp">+${t.xpReward} XP</span>
                </div>`;
            $('dailyTaskCard').querySelector('.daily-inner').addEventListener('click', () => openTask(t.id));
        } catch (e) { $('dailyTaskCard').style.display = 'none'; }
    }

    async function syncQuestTeacherBar() {
        const bar = $('questTeacherBar');
        const sel = $('questTeacherSelect');
        if (!bar || !sel) return null;
        if (!state.profile || state.profile.role !== 'STUDENT') {
            bar.style.display = 'none';
            state.questTeachers = null;
            state.selectedQuestTeacher = null;
            return null;
        }
        if (!state.questTeachers) {
            try {
                state.questTeachers = await API.get('/profile/my-teachers') || [];
            } catch {
                state.questTeachers = [];
            }
        }
        const teachers = state.questTeachers;
        if (!teachers.length) {
            bar.style.display = 'none';
            state.selectedQuestTeacher = null;
            return null;
        }
        bar.style.display = '';
        let current = sessionStorage.getItem('cq_quest_teacher') || '';
        if (teachers.length === 1) current = teachers[0].username;
        if (!current || !teachers.some(t => t.username === current)) current = teachers[0].username;
        state.selectedQuestTeacher = current;
        sessionStorage.setItem('cq_quest_teacher', current);
        sel.innerHTML = teachers.map(t =>
            `<option value="${escAttr(t.username)}" ${t.username === current ? 'selected' : ''}>${esc(t.username)}</option>`
        ).join('');
        sel.onchange = () => {
            state.selectedQuestTeacher = sel.value;
            sessionStorage.setItem('cq_quest_teacher', sel.value);
            state.lessonPage = 0;
            loadLessons(0);
        };
        return state.selectedQuestTeacher;
    }

    async function loadLessons(page) {
        if (page !== undefined) state.lessonPage = page;
        const pg = state.lessonPage;
        const el = $('questMap');
        const pagerEl = $('questPager');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            if (state.profile?.role === 'STUDENT') {
                await syncQuestTeacherBar();
                if (!state.questTeachers?.length) {
                    el.innerHTML = '<div class="empty-state"><div class="empty-icon">🗺</div><p>Нет прикреплённых учителей — примите приглашение в уведомлениях.</p></div>';
                    pagerEl.style.display = 'none';
                    return;
                }
                if (state.questTeachers.length > 1 && !state.selectedQuestTeacher) {
                    el.innerHTML = '<div class="empty-state"><div class="empty-icon">🗺</div><p>Выберите учителя над картой.</p></div>';
                    pagerEl.style.display = 'none';
                    return;
                }
            }
            let q = '/lessons?page=' + pg + '&size=8';
            if (state.profile?.role === 'STUDENT' && state.selectedQuestTeacher) {
                q += '&teacherUsername=' + encodeURIComponent(state.selectedQuestTeacher);
            }
            const data = await API.get(q);
            const lessons = data.content ?? data;
            const totalPages = data.totalPages ?? 1;
            if (!lessons.length) {
                el.innerHTML = '<div class="empty-state"><div class="empty-icon">🗺</div><p>Подземелья не созданы.</p></div>';
                pagerEl.style.display = 'none';
                return;
            }
            el.innerHTML = lessons.map(l => `
                <div class="quest-card" data-id="${l.id}">
                    <div class="quest-order">ПОДЗЕМЕЛЬЕ #${l.orderIndex}</div>
                    <h3>${esc(l.title)}</h3>
                    <p>${esc(l.description || '')}</p>
                    <div class="quest-meta"><span>⚔ ${l.taskCount} задач</span></div>
                </div>`).join('');
            el.querySelectorAll('.quest-card').forEach(c => c.addEventListener('click', () => openLesson(+c.dataset.id)));
            if (totalPages > 1) {
                pagerEl.style.display = 'flex';
                pagerEl.innerHTML = `
                    <button type="button" class="pager-btn" ${pg <= 0 ? 'disabled' : ''} data-lp="${pg - 1}">← Назад</button>
                    <span class="pager-info">${pg + 1} / ${totalPages}</span>
                    <button type="button" class="pager-btn" ${pg >= totalPages - 1 ? 'disabled' : ''} data-lp="${pg + 1}">Вперёд →</button>`;
                pagerEl.querySelectorAll('[data-lp]').forEach(b => b.addEventListener('click', () => {
                    if (b.disabled) return;
                    loadLessons(+b.dataset.lp);
                }));
            } else pagerEl.style.display = 'none';
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; pagerEl.style.display = 'none'; }
    }

    async function openLesson(id) {
        state.currentLessonId = id;
        showView('lesson'); $('viewTitle').textContent = 'Подземелье';
        try {
            const lesson = await API.get('/lessons/' + id);
            $('lessonTitle').textContent = lesson.title;
            const raw = lesson.content || '';
            let lessonHtml = raw;
            if (raw && !raw.trimStart().startsWith('<')) {
                if (typeof marked !== 'undefined') {
                    try {
                        const out = marked.parse(raw, { breaks: true });
                        lessonHtml = typeof out?.then === 'function' ? await out : String(out);
                    } catch {
                        lessonHtml = `<p>${esc(raw)}</p>`;
                    }
                } else {
                    lessonHtml = `<p>${esc(raw)}</p>`;
                }
            }
            $('lessonContent').innerHTML = lessonHtml;
            const tasks = await API.get('/tasks/lesson/' + id);
            if (!tasks.length) { $('taskList').innerHTML = '<div class="empty-state"><p>Задачи не созданы.</p></div>'; return; }
            $('taskList').innerHTML = tasks.map(t => `
                <div class="task-item" data-id="${t.id}">
                    <div class="task-id-badge">#${t.id}</div>
                    <div class="task-status-icon ${t.solved?'solved':'unsolved'}">${t.solved?'✅':'⬜'}</div>
                    <div class="task-info"><h4>${esc(t.title)}</h4><span class="task-diff diff-${t.difficulty.toLowerCase()}">${diffLabel(t.difficulty)}</span></div>
                    <span class="task-xp">+${t.xpReward} XP</span>
                </div>`).join('');
            $('taskList').querySelectorAll('.task-item').forEach(i => i.addEventListener('click', () => openTask(+i.dataset.id)));
        } catch (e) { $('lessonContent').innerHTML = `<p>Ошибка: ${esc(e.message)}</p>`; }
    }

    async function openTask(id) {
        state.currentTaskId = id;
        state.lastSubmitOutput = '';
        showView('task'); $('viewTitle').textContent = 'Битва';
        $('resultPanel').classList.remove('visible','correct','wrong');
        $('hintAiPanel').style.display = 'none';
        $('hintLocalNote').style.display = 'none';
        try {
            const t = await API.get('/tasks/' + id);
            $('taskTitle').textContent = `#${t.id} ${t.title}`;
            $('taskDiff').textContent = diffLabel(t.difficulty);
            $('taskDiff').className = 'task-diff diff-' + t.difficulty.toLowerCase();
            $('taskXp').textContent = '+' + t.xpReward + ' XP';
            $('taskAuthor').textContent = t.authorUsername ? 'автор: ' + t.authorUsername : '';
            $('taskDesc').innerHTML = t.description || '';

            if (t.hints) { $('hintBox').style.display = ''; $('hintText').textContent = t.hints; }
            else { $('hintBox').style.display = 'none'; }

            if (t.sampleTests && t.sampleTests.length) {
                $('sampleTestsBlock').style.display = '';
                $('sampleTestsList').innerHTML = t.sampleTests.map(tc => `
                    <div class="sample-test">
                        <div class="st-row"><span class="st-label">Вход:</span><code>${esc(tc.input || '(нет)')}</code></div>
                        <div class="st-row"><span class="st-label">Выход:</span><code>${esc(tc.expectedOutput)}</code></div>
                    </div>`).join('');
            } else { $('sampleTestsBlock').style.display = 'none'; }

            const template = t.templateCode || '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}';
            if (state.cm) { state.cm.setValue(template); }
            else { $('codeEditor').value = template; }
            loadTaskHistory(id);
        } catch (e) { $('taskDesc').textContent = 'Ошибка: ' + e.message; }
    }

    async function loadTaskHistory(taskId) {
        try {
            const history = await API.get('/submissions/task/' + taskId);
            if (history.length) {
                $('taskHistoryBox').style.display = '';
                $('taskHistoryList').innerHTML = history.map((s, i) => `
                    <div class="submission-entry">
                        <div class="sub-header" data-idx="${i}">
                            <span class="history-status">${s.status === 'CORRECT' ? '✅' : '❌'}</span>
                            <span class="sub-info">#${s.id} · ${esc(s.output)}</span>
                            <span class="history-time">${new Date(s.submittedAt).toLocaleString('ru')}</span>
                            <span class="sub-toggle">▶ код</span>
                        </div>
                        <pre class="sub-code" id="sub-code-${i}" style="display:none">${esc(s.code)}</pre>
                    </div>`).join('');
                $('taskHistoryList').querySelectorAll('.sub-header').forEach(h => {
                    h.addEventListener('click', () => {
                        const code = $('sub-code-' + h.dataset.idx);
                        const visible = code.style.display !== 'none';
                        code.style.display = visible ? 'none' : 'block';
                        h.querySelector('.sub-toggle').textContent = visible ? '▶ код' : '▼ код';
                    });
                });
            } else { $('taskHistoryBox').style.display = 'none'; }
        } catch (e) { $('taskHistoryBox').style.display = 'none'; }
    }

    $('hintAiBtn').addEventListener('click', async () => {
        if (!state.currentTaskId) return;
        const code = state.cm ? state.cm.getValue() : $('codeEditor').value;
        try {
            const h = await API.post('/ai/hint', { taskId: state.currentTaskId, code, output: state.lastSubmitOutput || undefined });
            if (!h) return;
            $('hintLocalNote').style.display = '';
            const p = $('hintAiPanel');
            p.style.display = '';
            p.textContent = h.hint;
        } catch (e) { showToast('🤖', 'Подсказка', e.message); }
    });

    $('submitBtn').addEventListener('click', async () => {
        const code = (state.cm ? state.cm.getValue() : $('codeEditor').value).trim();
        if (!code) return;
        $('submitBtn').disabled = true; $('submitSpinner').style.display = '';
        try {
            const res = await API.post('/submissions', { taskId: state.currentTaskId, code });
            const rp = $('resultPanel');
            rp.classList.remove('correct','wrong');
            rp.classList.add('visible', res.status === 'CORRECT' ? 'correct' : 'wrong');
            $('resultHeader').textContent = res.status === 'CORRECT' ? '✅ ПОБЕДА!' : '❌ Неверно';
            $('resultBody').textContent = res.output;
            state.lastSubmitOutput = res.output || '';
            $('resultTests').innerHTML = (res.testResults || []).length
                ? '<div class="test-results">' + res.testResults.map(tr =>
                    `<span class="test-badge ${tr.passed?'pass':'fail'}">#${tr.testNumber} ${tr.passed?'✓':'✗'}</span>`
                ).join('') + '</div>' : '';
            if (res.xpEarned > 0) { $('resultXp').textContent = '+ ' + res.xpEarned + ' XP!'; $('resultXp').style.display = ''; }
            else { $('resultXp').style.display = 'none'; }
            if (res.status === 'CORRECT') CQSound.success(); else CQSound.fail();
            if (res.newAchievements?.length) {
                CQSound.achievement();
                res.newAchievements.forEach(a => showToast(a.icon, a.name, a.description));
            }
            loadProfile();
            loadTaskHistory(state.currentTaskId);
        } catch (e) {
            state.lastSubmitOutput = e.message || '';
            CQSound.fail();
            $('resultPanel').classList.add('visible','wrong');
            $('resultHeader').textContent = '⚠ Ошибка'; $('resultBody').textContent = e.message;
        } finally { $('submitBtn').disabled = false; $('submitSpinner').style.display = 'none'; }
    });

    async function loadChallenges() {
        const el = $('challengesList');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const chs = await API.get('/challenges');
            if (!chs.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">⚔</div><p>Нет активных челленджей.</p></div>'; refreshNotifCount(); return; }
            el.innerHTML = chs.map(c => {
                const status = c.active ? `⏱ Идёт • до ${formatMsk(c.endTime)} МСК`
                    : c.upcoming ? `🕒 Старт ${formatMsk(c.startTime)} МСК`
                    : '🏁 Завершён';
                const cardClass = c.active ? 'active' : c.upcoming ? 'upcoming' : 'ended';
                const canJoin = c.upcoming && !c.joined;
                return `<div class="challenge-card ${cardClass}">
                    <div class="ch-header"><h3>${esc(c.title)}</h3><span class="ch-bonus">+${c.bonusXp} XP</span></div>
                    <p class="ch-desc">${esc(c.description || '')}</p>
                    <div class="ch-meta">
                        <span>⚔ ${c.taskCount} задач</span>
                        <span>${status}</span>
                    </div>
                    ${c.joined && c.taskIds && c.taskIds.length ? `<div class="ch-task-ids">Номера задач: ${c.taskIds.map(id => '#' + id).join(', ')}</div>` : ''}
                    <div class="ch-actions">
                        ${canJoin ? `<button class="btn-join" data-id="${c.id}">${c.upcoming ? 'Зарегистрироваться' : 'Вступить'}</button>` : ''}
                        ${c.joined ? '<span class="ch-joined">✓ Вы зарегистрированы</span>' : ''}
                        <button class="btn-results" data-id="${c.id}">Таблица</button>
                    </div>
                    <div class="ch-results" id="ch-results-${c.id}" style="display:none"></div>
                </div>`;
            }).join('');
            refreshNotifCount();
            el.querySelectorAll('.btn-join').forEach(b => b.addEventListener('click', async () => {
                try { await API.post('/challenges/' + b.dataset.id + '/join', {}); loadChallenges(); } catch(e) { alert(e.message); }
            }));
            el.querySelectorAll('.btn-results').forEach(b => b.addEventListener('click', async () => {
                const r = $('ch-results-' + b.dataset.id);
                if (r.style.display !== 'none') { r.style.display = 'none'; return; }
                try {
                    const results = await API.get('/challenges/' + b.dataset.id + '/results');
                    r.innerHTML = results.length
                        ? '<table class="results-table"><tr><th>#</th><th>Герой</th><th>Решено</th><th>ELO</th><th>Δ</th></tr>' +
                          results.map(x => {
                              const dc = x.ratingDelta > 0 ? 'delta-pos' : x.ratingDelta < 0 ? 'delta-neg' : '';
                              const ds = x.ratingDelta > 0 ? '+'+x.ratingDelta : x.ratingDelta;
                              return `<tr><td>${x.rank}</td><td>${esc(x.username)}</td><td>${x.tasksSolved}</td><td>${x.rating}</td><td class="${dc}">${ds}</td></tr>`;
                          }).join('') + '</table>'
                        : '<p style="font-size:12px;color:var(--text-dim)">Нет участников</p>';
                    r.style.display = '';
                } catch(e) { r.innerHTML = '<p>Ошибка</p>'; r.style.display = ''; }
            }));
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    async function loadAchievements() {
        const el = $('achGrid');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            await loadProfile();
            const achs = state.profile.achievements;
            if (!achs.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">🏆</div><p>Решайте задачи!</p></div>'; return; }
            el.innerHTML = achs.map(a => `<div class="ach-card"><div class="ach-icon">${a.icon}</div><div class="ach-info"><h4>${esc(a.name)}</h4><p>${esc(a.description)}</p><div class="ach-xp">+${a.xpReward} XP</div></div></div>`).join('');
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    function formField(form, name) {
        const el = form && form.elements.namedItem(name);
        return el && 'value' in el ? el : null;
    }
    function setFormField(form, name, val) {
        const el = formField(form, name);
        if (el) el.value = val == null ? '' : String(val);
    }

    let groupsCache = [];

    async function loadGroup() {
        await renderGroups();
        await renderMyGroup();
        await renderStudentSearch('');
    }

    function fillGroupSelect(selectEl, includeAll, currentValue) {
        if (!selectEl) return;
        const opts = [];
        if (includeAll) opts.push('<option value="">Все подгруппы</option>');
        else opts.push('<option value="">Без подгруппы</option>');
        groupsCache.forEach(g => opts.push(`<option value="${g.id}">${esc(g.name)} (${g.memberCount})</option>`));
        selectEl.innerHTML = opts.join('');
        if (currentValue != null) selectEl.value = String(currentValue);
    }

    async function renderGroups() {
        const el = $('groupsGrid');
        if (!el) return;
        try {
            groupsCache = await API.get('/admin/groups') || [];
            if (!groupsCache.length) {
                el.innerHTML = '<div class="empty-row">Подгрупп пока нет. Создайте первую.</div>';
            } else {
                el.innerHTML = groupsCache.map(g => `
                    <div class="group-card">
                        <div class="group-card-head">
                            <div class="group-card-name">${esc(g.name)}</div>
                            <div class="group-card-count">${g.memberCount} уч.</div>
                        </div>
                        <div class="group-card-actions">
                            <button type="button" class="btn-admin btn-admin-ghost" data-del-group="${g.id}">Удалить</button>
                        </div>
                    </div>`).join('');
                el.querySelectorAll('[data-del-group]').forEach(b => b.addEventListener('click', async () => {
                    if (!confirm('Удалить подгруппу? Ученики останутся в вашей группе, но без подгруппы.')) return;
                    try {
                        await API.delete('/admin/groups/' + b.dataset.delGroup);
                        await loadGroup();
                    } catch (e) { alert(e.message); }
                }));
            }
            fillGroupSelect($('groupFilter'), true, $('groupFilter')?.value);
            fillGroupSelect($('inviteGroupSelect'), false, $('inviteGroupSelect')?.value);
        } catch (e) {
            el.innerHTML = `<p style="color:var(--text-dim)">${esc(e.message)}</p>`;
        }
    }

    async function renderMyGroup() {
        const el = $('myGroupTable');
        if (!el) return;
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const filter = $('groupFilter')?.value || '';
            const nameQuery = ($('groupNameSearch')?.value || '').trim().toLowerCase();
            const all = await API.get('/admin/students');
            const students = all
                .filter(s => !filter || String(s.groupId || '') === filter)
                .filter(s => !nameQuery || s.username.toLowerCase().includes(nameQuery));
            if (!students.length) {
                el.innerHTML = '<div class="empty-row">В выбранной подгруппе нет учеников.</div>';
                return;
            }
            const groupOptions = groupsCache.map(g => `<option value="${g.id}">${esc(g.name)}</option>`).join('');
            el.innerHTML =
                '<table class="results-table"><tr><th>Герой</th><th>XP</th><th>LVL</th><th>Решено</th><th>Прогресс</th><th>Подгруппа</th><th></th></tr>' +
                students.map(s => `<tr>
                    <td>${esc(s.username)}</td>
                    <td>${s.xp}</td>
                    <td>${s.level}</td>
                    <td>${s.totalSolved}/${s.totalTasks}</td>
                    <td><div class="mini-bar"><div class="mini-fill" style="width:${s.solvedPercent}%"></div></div></td>
                    <td>
                        <select class="group-move-select" data-move="${s.userId}">
                            <option value="">— нет —</option>
                            ${groupOptions}
                        </select>
                    </td>
                    <td><button type="button" class="btn-group-action btn-remove" data-remove="${s.userId}">Убрать</button></td>
                </tr>`).join('') + '</table>';
            el.querySelectorAll('select[data-move]').forEach(sel => {
                const sid = sel.dataset.move;
                const current = students.find(x => x.userId == sid);
                if (current?.groupId) sel.value = String(current.groupId);
                sel.addEventListener('change', async () => {
                    const groupId = sel.value ? Number(sel.value) : null;
                    try {
                        await API.put('/admin/students/' + sid + '/group', { groupId });
                        await loadGroup();
                    } catch (e) { alert(e.message); }
                });
            });
            el.querySelectorAll('[data-remove]').forEach(b =>
                b.addEventListener('click', async () => {
                    if (!confirm('Убрать ученика из вашей группы?')) return;
                    try {
                        await API.delete('/admin/students/' + b.dataset.remove + '/assign');
                        showToast('👥', 'Ученик убран из группы', '');
                        loadGroup();
                    } catch (e) { alert(e.message); }
                }));
        } catch (e) {
            el.innerHTML = `<p style="color:var(--text-dim)">${esc(e.message)}</p>`;
        }
    }

    async function renderStudentSearch(query) {
        const el = $('studentSearchResults');
        if (!el) return;
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const q = (query || '').trim();
            const path = '/admin/students/available' + (q ? '?search=' + encodeURIComponent(q) : '');
            const students = await API.get(path);
            if (!students.length) {
                el.innerHTML = '<div class="empty-row">Подходящих учеников не найдено.</div>';
                return;
            }
            el.innerHTML = '<div class="group-list">' + students.map(s => `
                <div class="group-list-row">
                    <div>
                        <div>${esc(s.username)}</div>
                        ${s.teacherUsername ? `<div class="gl-meta">сейчас в группе: ${esc(s.teacherUsername)}</div>` : '<div class="gl-meta">без группы</div>'}
                    </div>
                    <div class="gl-meta">#${s.userId}</div>
                    <button type="button" class="btn-group-action btn-add" data-invite="${s.userId}" ${s.inviteSent ? 'disabled' : ''}>${s.inviteSent ? 'Запрос отправлен' : 'Пригласить'}</button>
                </div>`).join('') + '</div>';
            el.querySelectorAll('[data-invite]').forEach(b =>
                b.addEventListener('click', async () => {
                    const groupSel = $('inviteGroupSelect');
                    const groupId = groupSel && groupSel.value ? Number(groupSel.value) : null;
                    try {
                        await API.post('/admin/students/' + b.dataset.invite + '/invite', { groupId });
                        showToast('📩', 'Приглашение отправлено', '');
                        renderStudentSearch(query);
                    } catch (e) { alert(e.message); }
                }));
        } catch (e) {
            el.innerHTML = `<p style="color:var(--text-dim)">${esc(e.message)}</p>`;
        }
    }

    let studentSearchTimer;
    $('studentSearch')?.addEventListener('input', () => {
        clearTimeout(studentSearchTimer);
        studentSearchTimer = setTimeout(() => renderStudentSearch($('studentSearch').value || ''), 300);
    });
    $('studentSearchBtn')?.addEventListener('click', () =>
        renderStudentSearch($('studentSearch').value || ''));

    $('createGroupBtn')?.addEventListener('click', async () => {
        const input = $('newGroupName');
        const name = (input.value || '').trim();
        if (!name) { alert('Введите название подгруппы'); return; }
        try {
            await API.post('/admin/groups', { name });
            input.value = '';
            await loadGroup();
        } catch (e) { alert(e.message); }
    });

    $('groupFilter')?.addEventListener('change', renderMyGroup);
    let groupNameSearchTimer;
    $('groupNameSearch')?.addEventListener('input', () => {
        clearTimeout(groupNameSearchTimer);
        groupNameSearchTimer = setTimeout(renderMyGroup, 200);
    });

    async function loadMyLessonsIndex() {
        const el = $('myLessonsIndex');
        if (!el) return;
        try {
            const data = await API.get('/lessons?page=0&size=100');
            const lessons = data.content ?? data ?? [];
            if (!lessons.length) { el.innerHTML = ''; return; }
            el.innerHTML = '<h4>Ваши подземелья</h4>' + lessons.map(l => `
                <div class="lesson-index-row">
                    <span class="li-title">${esc(l.title)}</span>
                    <span class="li-order">#${l.orderIndex}</span>
                    <button type="button" class="btn-admin btn-admin-ghost" data-edit-order="${l.orderIndex}">Редактировать</button>
                </div>`).join('');
            el.querySelectorAll('[data-edit-order]').forEach(b => b.addEventListener('click', () => {
                $('loadLessonOrder').value = String(b.dataset.editOrder);
                $('loadLessonBtn').click();
            }));
        } catch (e) { el.innerHTML = ''; }
    }

    const taskExampleRowHtml = () => `
        <div class="admin-example-row">
            <div><label>Пример ввода</label><textarea data-tc="in" rows="2" placeholder="Что попадает в stdin (можно пусто)"></textarea></div>
            <div><label>Ожидаемый вывод для этого примера</label><textarea data-tc="out" rows="3" placeholder="Что должно быть в stdout; без этого пример не сохранится"></textarea></div>
            <label class="admin-tc-chk"><input type="checkbox" data-tc="sample" checked> Показывать в условии задачи</label>
            <button type="button" class="rm-tc">Удалить пример</button>
        </div>`;

    function bindExampleRow(row) {
        row.querySelector('.rm-tc').addEventListener('click', () => {
            const box = $('taskExamples');
            if (box && box.children.length > 1) row.remove();
        });
    }

    function resetTaskExamples() {
        const box = $('taskExamples');
        if (!box) return;
        box.innerHTML = taskExampleRowHtml();
        bindExampleRow(box.firstElementChild);
    }

    function collectTaskExamplesForApi() {
        const box = $('taskExamples');
        if (!box) return [];
        return [...box.querySelectorAll('.admin-example-row')].map(row => ({
            input: row.querySelector('[data-tc="in"]').value,
            expectedOutput: row.querySelector('[data-tc="out"]').value,
            sample: row.querySelector('[data-tc="sample"]').checked
        })).filter(tc => tc.expectedOutput.trim().length > 0);
    }

    function renderTaskExamplesFromApi(examples) {
        const box = $('taskExamples');
        if (!box) return;
        box.innerHTML = '';
        const rows = examples && examples.length ? examples : [{ input: '', expectedOutput: '', sample: true }];
        rows.forEach(ex => {
            box.insertAdjacentHTML('beforeend', taskExampleRowHtml());
            const row = box.lastElementChild;
            row.querySelector('[data-tc="in"]').value = ex.input ?? '';
            row.querySelector('[data-tc="out"]').value = ex.expectedOutput ?? '';
            row.querySelector('[data-tc="sample"]').checked = !!ex.sample;
            bindExampleRow(row);
        });
    }

    function articlePreviewEmptyHtml() {
        return '<p class="article-preview-placeholder">Сюда нельзя вводить текст: это окно только показывает, как будет выглядеть Markdown <strong>слева</strong>. Серая подсказка в поле не считается содержимым — начните печатать, и превью обновится.</p>';
    }

    function syncArticlePreview() {
        const ta = $('articleContent');
        const pv = $('articleMdPreview');
        if (!ta || !pv) return;
        const raw = mdFromApi(ta.value);
        if (!raw.trim()) {
            pv.innerHTML = articlePreviewEmptyHtml();
            return;
        }
        const apply = (html) => { pv.innerHTML = html; };
        try {
            if (typeof marked !== 'undefined') {
                const out = marked.parse(raw, { breaks: true });
                if (out != null && typeof out.then === 'function') {
                    apply('<p class="article-preview-placeholder">…</p>');
                    out.then((h) => apply(String(h))).catch(() => apply(`<p>${esc(raw)}</p>`));
                    return;
                }
                apply(String(out));
            } else {
                apply(`<p>${esc(raw)}</p>`);
            }
        } catch {
            apply(`<p>${esc(raw)}</p>`);
        }
    }

    $('loadLessonBtn')?.addEventListener('click', async () => {
        const order = parseDungeonOrder($('loadLessonOrder').value);
        if (!Number.isFinite(order)) { alert('Введите номер подземелья (целое число от 0).'); return; }
        try {
            const d = await API.get('/admin/lessons/by-order/' + order);
            const f = $('createLessonForm');
            setFormField(f, 'title', d.title);
            setFormField(f, 'description', d.description);
            setFormField(f, 'content', d.content);
            setFormField(f, 'orderIndex', d.orderIndex ?? 0);
            setFormField(f, 'taskIds', '');
            $('editLessonOrder').value = String(order);
            $('lessonFormSubmitBtn').textContent = 'Сохранить урок';
        } catch (err) { alert(err.message); }
    });
    $('clearLessonEditBtn')?.addEventListener('click', () => {
        $('createLessonForm').reset();
        $('editLessonOrder').value = '';
        $('loadLessonOrder').value = '';
        $('lessonFormSubmitBtn').textContent = 'Создать урок';
    });

    $('loadTaskBtn')?.addEventListener('click', async () => {
        const id = +$('loadTaskId').value;
        if (!id) { alert('Введите id задачи (число).'); return; }
        try {
            const d = await API.get('/admin/tasks/' + id);
            const f = $('createTaskForm');
            setFormField(f, 'title', d.title);
            setFormField(f, 'description', d.description);
            setFormField(f, 'difficulty', d.difficulty);
            setFormField(f, 'xpReward', d.xpReward);
            setFormField(f, 'templateCode', d.templateCode);
            setFormField(f, 'hints', d.hints);
            renderTaskExamplesFromApi(d.examples);
            $('editTaskId').value = String(d.id);
            $('taskFormSubmitBtn').textContent = 'Сохранить задачу';
            const tcr = $('taskChallengeRow');
            if (tcr) tcr.style.display = 'none';
        } catch (err) { alert(err.message); }
    });
    $('clearTaskEditBtn')?.addEventListener('click', () => {
        $('createTaskForm').reset();
        $('editTaskId').value = '';
        $('loadTaskId').value = '';
        const tcs = $('taskChallengeSelect');
        if (tcs) tcs.value = '';
        const tcr = $('taskChallengeRow');
        if (tcr) tcr.style.display = '';
        refreshTaskChallengeSelect();
        resetTaskExamples();
        $('taskFormSubmitBtn').textContent = 'Создать задачу';
    });

    $('loadArticleBtn')?.addEventListener('click', async () => {
        const id = +$('loadArticleId').value;
        if (!id) { alert('Введите id статьи (число).'); return; }
        try {
            const d = await API.get('/articles/' + id);
            const f = $('createArticleForm');
            setFormField(f, 'title', d.title);
            setFormField(f, 'category', d.category);
            $('articleContent').value = d.content || '';
            $('editArticleId').value = String(d.id);
            $('articleFormSubmitBtn').textContent = 'Сохранить статью';
            syncArticlePreview();
        } catch (err) { alert(err.message); }
    });
    $('clearArticleEditBtn')?.addEventListener('click', () => {
        $('createArticleForm').reset();
        $('editArticleId').value = '';
        $('loadArticleId').value = '';
        $('articleFormSubmitBtn').textContent = 'Опубликовать';
        syncArticlePreview();
    });
    $('articleContent')?.addEventListener('input', () => syncArticlePreview());

    function parseIdList(raw) {
        if (!raw) return [];
        return String(raw).split(/[\s,;]+/).map(s => +s).filter(n => Number.isFinite(n) && n > 0);
    }

    function parseDungeonOrder(raw) {
        if (raw == null) return NaN;
        const s = String(raw).trim();
        if (s === '') return NaN;
        const n = Number(s);
        if (!Number.isInteger(n) || n < 0) return NaN;
        return n;
    }

    $('createLessonForm').addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const taskIds = parseIdList(fd.get('taskIds'));
        const body = {
            title: fd.get('title'),
            description: fd.get('description'),
            content: fd.get('content'),
            orderIndex: +fd.get('orderIndex'),
            taskIds
        };
        const rawEdit = ($('editLessonOrder') && $('editLessonOrder').value != null)
            ? String($('editLessonOrder').value).trim() : '';
        try {
            const saved = rawEdit !== ''
                ? await API.put('/admin/lessons/by-order/' + encodeURIComponent(rawEdit), body)
                : await API.post('/admin/lessons', body);
            if (rawEdit !== '') {
                $('lessonFormSubmitBtn').textContent = 'Сохранить урок';
                if (saved?.orderIndex != null) {
                    $('editLessonOrder').value = String(saved.orderIndex);
                    $('loadLessonOrder').value = String(saved.orderIndex);
                }
            } else {
                e.target.reset();
                $('editLessonOrder').value = '';
                $('lessonFormSubmitBtn').textContent = 'Создать урок';
                if (saved?.orderIndex != null) $('loadLessonOrder').value = String(saved.orderIndex);
            }
            const detail = taskIds.length ? `Прикреплено задач: ${taskIds.length}` : '';
            showToast('📖', rawEdit !== '' ? 'Урок сохранён' : 'Урок создан', detail);
            loadMyLessonsIndex();
        } catch (err) { alert(err.message); }
    });

    async function resolveLessonByOrder(order) {
        const l = await API.get('/admin/lessons/by-order/' + order);
        return l;
    }

    $('attachTasksForm')?.addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const order = parseDungeonOrder(fd.get('lessonOrder'));
        const taskIds = parseIdList(fd.get('taskIds'));
        if (!Number.isFinite(order) || !taskIds.length) {
            alert('Укажите номер подземелья и хотя бы один id задачи.');
            return;
        }
        try {
            const lesson = await resolveLessonByOrder(order);
            const r = await API.post('/admin/lessons/' + lesson.id + '/tasks', { taskIds });
            const n = (r && r.attachedTaskIds && r.attachedTaskIds.length) || taskIds.length;
            showToast('🔗', 'Задачи прикреплены', `Подземелье #${order}: ${n} задач(и)`);
            e.target.reset();
        } catch (err) { alert(err.message); }
    });

    $('detachTasksBtn')?.addEventListener('click', async () => {
        const f = $('attachTasksForm');
        const fd = new FormData(f);
        const order = parseDungeonOrder(fd.get('lessonOrder'));
        const taskIds = parseIdList(fd.get('taskIds'));
        if (!Number.isFinite(order) || !taskIds.length) {
            alert('Укажите номер подземелья и хотя бы один id задачи.');
            return;
        }
        try {
            const lesson = await resolveLessonByOrder(order);
            for (const tid of taskIds) {
                await API.delete('/admin/lessons/' + lesson.id + '/tasks/' + tid);
            }
            showToast('🪶', 'Задачи откреплены', `Подземелье #${order}: ${taskIds.length}`);
            f.reset();
        } catch (err) { alert(err.message); }
    });
    $('createTaskForm').addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const testCases = collectTaskExamplesForApi();
        const tid = $('editTaskId').value;
        const chEl = formField($('createTaskForm'), 'challengeId');
        const rawCh = chEl && chEl.value ? +chEl.value : NaN;
        const challengeId = !tid && Number.isFinite(rawCh) && rawCh > 0 ? rawCh : null;
        const body = {
            title: fd.get('title'),
            description: fd.get('description'),
            difficulty: fd.get('difficulty'),
            xpReward: +fd.get('xpReward'),
            templateCode: fd.get('templateCode'),
            hints: fd.get('hints'),
            testCases: testCases.length ? testCases : null
        };
        if (challengeId != null) body.challengeId = challengeId;
        try {
            const saved = tid
                ? await API.put('/admin/tasks/' + tid, body)
                : await API.post('/admin/tasks', body);
            if (tid) {
                if (saved && saved.id != null) $('editTaskId').value = String(saved.id);
                $('taskFormSubmitBtn').textContent = 'Сохранить задачу';
                if (saved?.id != null) $('loadTaskId').value = String(saved.id);
            } else {
                e.target.reset();
                $('editTaskId').value = '';
                resetTaskExamples();
                $('taskFormSubmitBtn').textContent = 'Создать задачу';
                if (saved?.id != null) $('loadTaskId').value = String(saved.id);
            }
            showToast('⚔', tid ? 'Задача сохранена' : 'Задача создана', '');
        } catch (err) { alert(err.message); }
    });
    $('createChallengeForm').addEventListener('submit', async e => {
        e.preventDefault(); const fd = new FormData(e.target);
        try {
            const startTime = normalizeDatetimeLocalForApi(fd.get('startTime'));
            const endTime = normalizeDatetimeLocalForApi(fd.get('endTime'));
            await API.post('/admin/challenges', { title: fd.get('title'), description: fd.get('description'), startTime, endTime, bonusXp: +fd.get('bonusXp') });
            e.target.reset(); showToast('🏟','Челлендж создан','');
            refreshTaskChallengeSelect();
        } catch(err) { alert(err.message); }
    });
    $('createArticleForm').addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const ta = $('articleContent');
        const body = {
            title: (fd.get('title') || '').trim(),
            content: (ta ? ta.value : fd.get('content')) || '',
            category: (fd.get('category') || '').trim() || 'Справочник'
        };
        const aid = $('editArticleId').value;
        try {
            const saved = aid
                ? await API.put('/admin/articles/' + aid, body)
                : await API.post('/admin/articles', body);
            const sid = saved && saved.id != null ? saved.id : null;
            if (aid) {
                if (sid != null) $('editArticleId').value = String(sid);
                $('articleFormSubmitBtn').textContent = 'Сохранить статью';
                if (sid != null) $('loadArticleId').value = String(sid);
            } else {
                e.target.reset();
                $('editArticleId').value = '';
                $('articleFormSubmitBtn').textContent = 'Опубликовать';
                if (sid != null) $('loadArticleId').value = String(sid);
            }
            syncArticlePreview();
            showToast('📚', aid ? 'Статья сохранена' : 'Статья опубликована',
                sid != null ? `Номер в библиотеке: ${sid}` : '');
        } catch (err) { alert(err.message); }
    });

    function diffLabel(d) { return { EASY:'🟢 Лёгкая', MEDIUM:'🟡 Средняя', HARD:'🔴 Сложная' }[d] || d; }
    function esc(s) { if (!s) return ''; const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
    function escAttr(s) {
        if (!s) return '';
        return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
    }
    function formatMsk(iso) {
        if (!iso) return '';
        const m = String(iso).match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
        if (!m) return iso;
        return `${m[3]}.${m[2]}.${m[1]} ${m[4]}:${m[5]}`;
    }
    function mdFromApi(s) {
        if (!s) return '';
        return s.replace(/\r\n/g, '\n').replace(/\\n/g, '\n').replace(/\\r/g, '\r');
    }
    function showToast(icon, title, desc) {
        const t = document.createElement('div'); t.className = 'toast';
        t.innerHTML = `<span class="toast-icon">${icon}</span><div class="toast-text"><h4>${esc(title)}</h4><p>${esc(desc)}</p></div>`;
        $('toasts').appendChild(t); setTimeout(() => t.remove(), 4000);
    }
    $('codeEditor').addEventListener('keydown', e => {
        if (e.key === 'Tab') { e.preventDefault(); const s = $('codeEditor').selectionStart; $('codeEditor').value = $('codeEditor').value.substring(0,s)+'    '+$('codeEditor').value.substring($('codeEditor').selectionEnd); $('codeEditor').selectionStart = $('codeEditor').selectionEnd = s+4; }
    });

    if (typeof CodeMirror !== 'undefined') {
        state.cm = CodeMirror.fromTextArea($('codeEditor'), {
            mode: 'text/x-csrc',
            theme: document.body.classList.contains('light') ? 'eclipse' : 'material-darker',
            lineNumbers: false, tabSize: 4, indentWithTabs: false,
            matchBrackets: true, lineWrapping: true
        });
        state.cm.setSize('100%', 280);
    }

    let prevLevel = null;
    const origLoadProfile = loadProfile;
    loadProfile = async function () {
        await origLoadProfile();
        if (state.profile) {
            if (prevLevel !== null && state.profile.level > prevLevel) {
                CQSound.levelUp();
                $('levelUpLevel').textContent = state.profile.level;
                $('levelUpOverlay').style.display = 'flex';
                setTimeout(() => $('levelUpOverlay').style.display = 'none', 2500);
            }
            prevLevel = state.profile.level;
        }
    };

    $('addExampleBtn')?.addEventListener('click', () => {
        const box = $('taskExamples');
        if (!box) return;
        box.insertAdjacentHTML('beforeend', taskExampleRowHtml());
        bindExampleRow(box.lastElementChild);
    });
    if ($('taskExamples')) resetTaskExamples();
    syncArticlePreview();

    (async () => {
        await loadProfile();
        await loadLessons();
    })();
});
