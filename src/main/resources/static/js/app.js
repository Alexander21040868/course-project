document.addEventListener('DOMContentLoaded', () => {
    if (!localStorage.getItem('cq_token')) { window.location.href = '/index.html'; return; }

    const state = { currentView: 'quests', currentLessonId: null, currentTaskId: null, profile: null };
    const $ = id => document.getElementById(id);

    // ── Navigation ──
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
        if (btn.dataset.atab === 'students') loadStudents();
    }));

    let catalogTimer;
    $('catalogSearch').addEventListener('input', () => {
        clearTimeout(catalogTimer);
        catalogTimer = setTimeout(() => loadCatalog($('catalogSearch').value), 300);
    });

    const titles = {
        quests:'Карта подземелий', catalog:'Каталог задач', lesson:'Подземелье', task:'Битва',
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

        if (view === 'quests') loadLessons();
        if (view === 'catalog') loadCatalog('');
        if (view === 'achievements') loadAchievements();
        if (view === 'challenges') loadChallenges();
        if (view === 'leaderboard') loadLeaderboard();
        if (view === 'profile') loadFullProfile();
    }

    function showView(view) {
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        $('view-' + view).classList.add('active');
    }

    // ── Profile (sidebar) ──
    async function loadProfile() {
        try {
            const p = await API.get('/profile');
            state.profile = p;
            $('charName').textContent = p.username;
            $('charRole').textContent = p.role === 'TEACHER' ? 'Мастер' : getTitle(p.level);
            $('xpText').textContent = p.xp + ' XP';
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
        } catch (e) { console.error(e); }
    }

    function getTitle(level) {
        if (level >= 16) return 'Архимаг';
        if (level >= 11) return 'Мастер';
        if (level >= 8)  return 'Кодер';
        if (level >= 5)  return 'Подмастерье';
        if (level >= 3)  return 'Ученик';
        return 'Новобранец';
    }

    // ── Full Profile (без достижений — есть отдельная вкладка) ──
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
                    <div class="stat-card"><div class="stat-val">${p.level}</div><div class="stat-label">Уровень</div></div>
                    <div class="stat-card"><div class="stat-val">${p.xp}</div><div class="stat-label">Опыт (XP)</div></div>
                    <div class="stat-card"><div class="stat-val">${p.solvedCount}/${p.totalTasks}</div><div class="stat-label">Решено</div></div>
                    <div class="stat-card"><div class="stat-val streak-val">🔥 ${p.streak}</div><div class="stat-label">Streak (макс: ${p.maxStreak})</div></div>
                </div>
                <h3 class="section-title">📊 Прогресс по урокам</h3>
                <div class="progress-list">${(p.lessonsProgress || []).map(l => `
                    <div class="progress-item">
                        <div class="progress-item-head">
                            <span>${esc(l.title)}</span>
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

    // ── Catalog (пул задач) ──
    async function loadCatalog(search) {
        const el = $('catalogList');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const url = search ? '/tasks?search=' + encodeURIComponent(search) : '/tasks';
            const tasks = await API.get(url);
            if (!tasks.length) { el.innerHTML = '<div class="empty-state"><p>Задачи не найдены</p></div>'; return; }
            el.innerHTML = tasks.map(t => `
                <div class="task-item" data-id="${t.id}">
                    <div class="task-id-badge">#${t.id}</div>
                    <div class="task-status-icon ${t.solved?'solved':'unsolved'}">${t.solved?'✅':'⬜'}</div>
                    <div class="task-info">
                        <h4>${esc(t.title)}</h4>
                        <span class="task-diff diff-${t.difficulty.toLowerCase()}">${diffLabel(t.difficulty)}</span>
                        <span class="task-lesson-tag">${esc(t.lessonTitle)}</span>
                    </div>
                    <span class="task-xp">+${t.xpReward} XP</span>
                </div>`).join('');
            el.querySelectorAll('.task-item').forEach(i =>
                i.addEventListener('click', () => openTask(+i.dataset.id)));
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    // ── Leaderboard ──
    async function loadLeaderboard() {
        const el = $('leaderboardContent');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const board = await API.get('/leaderboard');
            if (!board.length) { el.innerHTML = '<div class="empty-state"><p>Пока нет данных</p></div>'; return; }
            el.innerHTML = '<table class="results-table leaderboard-table"><tr><th>#</th><th>Герой</th><th>XP</th><th>Уровень</th><th>Решено</th></tr>' +
                board.map(r => {
                    const medal = r.rank === 1 ? '🥇' : r.rank === 2 ? '🥈' : r.rank === 3 ? '🥉' : r.rank;
                    const cls = r.rank <= 3 ? ' class="top-row"' : '';
                    return `<tr${cls}><td>${medal}</td><td>${esc(r.username)}</td><td>${r.xp}</td><td>${r.level}</td><td>${r.solvedCount}</td></tr>`;
                }).join('') + '</table>';
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    // ── Lessons ──
    async function loadLessons() {
        const el = $('questMap');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const lessons = await API.get('/lessons');
            if (!lessons.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">🗺</div><p>Подземелья не созданы.</p></div>'; return; }
            el.innerHTML = lessons.map(l => `
                <div class="quest-card" data-id="${l.id}">
                    <div class="quest-order">ПОДЗЕМЕЛЬЕ #${l.orderIndex + 1}</div>
                    <h3>${esc(l.title)}</h3>
                    <p>${esc(l.description || '')}</p>
                    <div class="quest-meta"><span>⚔ ${l.taskCount} задач</span></div>
                </div>`).join('');
            el.querySelectorAll('.quest-card').forEach(c => c.addEventListener('click', () => openLesson(+c.dataset.id)));
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    async function openLesson(id) {
        state.currentLessonId = id;
        showView('lesson'); $('viewTitle').textContent = 'Подземелье';
        try {
            const lesson = await API.get('/lessons/' + id);
            $('lessonTitle').textContent = lesson.title;
            $('lessonContent').innerHTML = lesson.content || '<p>Теория скоро будет добавлена...</p>';
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
        showView('task'); $('viewTitle').textContent = 'Битва';
        $('resultPanel').classList.remove('visible','correct','wrong');
        try {
            const t = await API.get('/tasks/' + id);
            $('taskTitle').textContent = `#${t.id} ${t.title}`;
            $('taskDiff').textContent = diffLabel(t.difficulty);
            $('taskDiff').className = 'task-diff diff-' + t.difficulty.toLowerCase();
            $('taskXp').textContent = '+' + t.xpReward + ' XP';
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

            $('codeEditor').value = t.templateCode || '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}';
            loadTaskHistory(id);
        } catch (e) { $('taskDesc').textContent = 'Ошибка: ' + e.message; }
    }

    // ── Task History ──
    async function loadTaskHistory(taskId) {
        try {
            const history = await API.get('/submissions/task/' + taskId);
            if (history.length) {
                $('taskHistoryBox').style.display = '';
                $('taskHistoryList').innerHTML = history.map(s => `
                    <div class="history-item">
                        <span class="history-status">${s.status === 'CORRECT' ? '✅' : '❌'}</span>
                        <span class="history-task">${esc(s.output)}</span>
                        <span class="history-time">${new Date(s.submittedAt).toLocaleString('ru')}</span>
                    </div>`).join('');
            } else { $('taskHistoryBox').style.display = 'none'; }
        } catch (e) { $('taskHistoryBox').style.display = 'none'; }
    }

    // ── Submit ──
    $('submitBtn').addEventListener('click', async () => {
        const code = $('codeEditor').value.trim();
        if (!code) return;
        $('submitBtn').disabled = true; $('submitSpinner').style.display = '';
        try {
            const res = await API.post('/submissions', { taskId: state.currentTaskId, code });
            const rp = $('resultPanel');
            rp.classList.remove('correct','wrong');
            rp.classList.add('visible', res.status === 'CORRECT' ? 'correct' : 'wrong');
            $('resultHeader').textContent = res.status === 'CORRECT' ? '✅ ПОБЕДА!' : '❌ Неверно';
            $('resultBody').textContent = res.output;
            $('resultTests').innerHTML = (res.testResults || []).length
                ? '<div class="test-results">' + res.testResults.map(tr =>
                    `<span class="test-badge ${tr.passed?'pass':'fail'}">#${tr.testNumber} ${tr.passed?'✓':'✗'}</span>`
                ).join('') + '</div>' : '';
            if (res.xpEarned > 0) { $('resultXp').textContent = '+ ' + res.xpEarned + ' XP!'; $('resultXp').style.display = ''; }
            else { $('resultXp').style.display = 'none'; }
            if (res.newAchievements) res.newAchievements.forEach(a => showToast(a.icon, a.name, a.description));
            loadProfile();
            loadTaskHistory(state.currentTaskId);
        } catch (e) {
            $('resultPanel').classList.add('visible','wrong');
            $('resultHeader').textContent = '⚠ Ошибка'; $('resultBody').textContent = e.message;
        } finally { $('submitBtn').disabled = false; $('submitSpinner').style.display = 'none'; }
    });

    // ── Challenges ──
    async function loadChallenges() {
        const el = $('challengesList');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const chs = await API.get('/challenges');
            if (!chs.length) { el.innerHTML = '<div class="empty-state"><div class="empty-icon">⚔</div><p>Нет активных челленджей.</p></div>'; return; }
            el.innerHTML = chs.map(c => {
                const remaining = Math.max(0, Math.floor((new Date(c.endTime) - Date.now()) / 3600000));
                return `<div class="challenge-card ${c.active?'':'ended'}">
                    <div class="ch-header"><h3>${esc(c.title)}</h3><span class="ch-bonus">+${c.bonusXp} XP</span></div>
                    <p class="ch-desc">${esc(c.description || '')}</p>
                    <div class="ch-meta">
                        <span>⚔ ${c.taskCount} задач</span>
                        <span>⏱ ${c.active ? remaining + 'ч' : 'Завершён'}</span>
                    </div>
                    <div class="ch-actions">
                        ${c.active && !c.joined ? `<button class="btn-join" data-id="${c.id}">Вступить</button>` : ''}
                        ${c.joined ? '<span class="ch-joined">✓ Участвуете</span>' : ''}
                        <button class="btn-results" data-id="${c.id}">Таблица</button>
                    </div>
                    <div class="ch-results" id="ch-results-${c.id}" style="display:none"></div>
                </div>`;
            }).join('');
            el.querySelectorAll('.btn-join').forEach(b => b.addEventListener('click', async () => {
                try { await API.post('/challenges/' + b.dataset.id + '/join', {}); loadChallenges(); } catch(e) { alert(e.message); }
            }));
            el.querySelectorAll('.btn-results').forEach(b => b.addEventListener('click', async () => {
                const r = $('ch-results-' + b.dataset.id);
                if (r.style.display !== 'none') { r.style.display = 'none'; return; }
                try {
                    const results = await API.get('/challenges/' + b.dataset.id + '/results');
                    r.innerHTML = results.length
                        ? '<table class="results-table"><tr><th>#</th><th>Герой</th><th>Решено</th><th>LVL</th></tr>' +
                          results.map(x => `<tr><td>${x.rank}</td><td>${esc(x.username)}</td><td>${x.tasksSolved}</td><td>${x.level}</td></tr>`).join('') + '</table>'
                        : '<p style="font-size:12px;color:var(--text-dim)">Нет участников</p>';
                    r.style.display = '';
                } catch(e) { r.innerHTML = '<p>Ошибка</p>'; r.style.display = ''; }
            }));
        } catch (e) { el.innerHTML = `<div class="empty-state"><p>${esc(e.message)}</p></div>`; }
    }

    // ── Achievements ──
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

    // ── Admin ──
    async function loadStudents() {
        const el = $('studentsTable');
        el.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const students = await API.get('/admin/students');
            if (!students.length) { el.innerHTML = '<p style="color:var(--text-dim)">Нет студентов</p>'; return; }
            el.innerHTML = '<table class="results-table"><tr><th>Герой</th><th>XP</th><th>LVL</th><th>Решено</th><th>Прогресс</th></tr>' +
                students.map(s => `<tr><td>${esc(s.username)}</td><td>${s.xp}</td><td>${s.level}</td><td>${s.totalSolved}/${s.totalTasks}</td><td><div class="mini-bar"><div class="mini-fill" style="width:${s.solvedPercent}%"></div></div></td></tr>`).join('') + '</table>';
        } catch (e) { el.innerHTML = '<p>Ошибка</p>'; }
    }

    $('createLessonForm').addEventListener('submit', async e => {
        e.preventDefault(); const fd = new FormData(e.target);
        try { await API.post('/admin/lessons', { title: fd.get('title'), description: fd.get('description'), content: fd.get('content'), orderIndex: +fd.get('orderIndex') }); e.target.reset(); showToast('📖','Урок создан',''); } catch(err) { alert(err.message); }
    });
    $('createTaskForm').addEventListener('submit', async e => {
        e.preventDefault(); const fd = new FormData(e.target);
        try { await API.post('/admin/tasks', { lessonId: +fd.get('lessonId'), title: fd.get('title'), description: fd.get('description'), difficulty: fd.get('difficulty'), xpReward: +fd.get('xpReward'), templateCode: fd.get('templateCode'), expectedOutput: fd.get('expectedOutput'), hints: fd.get('hints'), orderIndex: +fd.get('orderIndex') }); e.target.reset(); showToast('⚔','Задача создана',''); } catch(err) { alert(err.message); }
    });
    $('createChallengeForm').addEventListener('submit', async e => {
        e.preventDefault(); const fd = new FormData(e.target);
        try {
            const ids = fd.get('taskIds').split(',').map(s => +s.trim()).filter(n => n > 0);
            await API.post('/admin/challenges', { title: fd.get('title'), description: fd.get('description'), startTime: fd.get('startTime'), endTime: fd.get('endTime'), bonusXp: +fd.get('bonusXp'), taskIds: ids });
            e.target.reset(); showToast('🏟','Челлендж создан','');
        } catch(err) { alert(err.message); }
    });

    // ── Helpers ──
    function diffLabel(d) { return { EASY:'🟢 Лёгкая', MEDIUM:'🟡 Средняя', HARD:'🔴 Сложная' }[d] || d; }
    function esc(s) { if (!s) return ''; const d = document.createElement('div'); d.textContent = s; return d.innerHTML; }
    function showToast(icon, title, desc) {
        const t = document.createElement('div'); t.className = 'toast';
        t.innerHTML = `<span class="toast-icon">${icon}</span><div class="toast-text"><h4>${esc(title)}</h4><p>${esc(desc)}</p></div>`;
        $('toasts').appendChild(t); setTimeout(() => t.remove(), 4000);
    }
    $('codeEditor').addEventListener('keydown', e => {
        if (e.key === 'Tab') { e.preventDefault(); const s = $('codeEditor').selectionStart; $('codeEditor').value = $('codeEditor').value.substring(0,s)+'    '+$('codeEditor').value.substring($('codeEditor').selectionEnd); $('codeEditor').selectionStart = $('codeEditor').selectionEnd = s+4; }
    });

    loadProfile(); loadLessons();
});
