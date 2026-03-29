document.addEventListener('DOMContentLoaded', () => {
    if (!localStorage.getItem('cq_token')) {
        window.location.href = '/index.html';
        return;
    }

    const state = {
        currentView: 'quests',
        currentLessonId: null,
        currentTaskId: null,
        profile: null
    };

    // ── DOM refs ──
    const $ = id => document.getElementById(id);
    const charName    = $('charName');
    const charRole    = $('charRole');
    const xpText      = $('xpText');
    const xpFill      = $('xpFill');
    const levelBadge  = $('levelBadge');
    const solvedStat  = $('solvedStat');
    const levelStat   = $('levelStat');
    const viewTitle   = $('viewTitle');
    const questMap    = $('questMap');
    const taskList    = $('taskList');
    const achGrid     = $('achGrid');
    const codeEditor  = $('codeEditor');
    const resultPanel = $('resultPanel');
    const adminNav    = $('adminNav');
    const toasts      = $('toasts');

    // ── Navigation ──
    document.querySelectorAll('.nav-item[data-view]').forEach(btn => {
        btn.addEventListener('click', () => navigateTo(btn.dataset.view));
    });

    $('backToQuests').addEventListener('click', () => navigateTo('quests'));
    $('backToLesson').addEventListener('click', () => openLesson(state.currentLessonId));
    $('logoutBtn').addEventListener('click', () => {
        localStorage.clear();
        window.location.href = '/index.html';
    });

    function navigateTo(view) {
        state.currentView = view;
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        $('view-' + view).classList.add('active');
        document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
        const navBtn = document.querySelector(`.nav-item[data-view="${view}"]`);
        if (navBtn) navBtn.classList.add('active');

        const titles = {
            quests: 'Карта подземелий',
            lesson: 'Подземелье',
            task: 'Битва',
            achievements: 'Зал славы',
            admin: 'Штаб мастера'
        };
        viewTitle.textContent = titles[view] || '';

        if (view === 'quests') loadLessons();
        if (view === 'achievements') loadAchievements();
    }

    // ── Profile ──
    async function loadProfile() {
        try {
            const p = await API.get('/profile');
            state.profile = p;
            charName.textContent = p.username;
            charRole.textContent = p.role === 'TEACHER' ? 'Мастер' : 'Искатель';
            xpText.textContent = p.xp + ' XP';
            levelBadge.textContent = 'LVL ' + p.level;
            solvedStat.textContent = p.solvedCount;
            levelStat.textContent = p.level;

            const pct = p.xpToNextLevel > 0
                ? Math.min(100, ((p.xp % (p.level * p.level * 100 - (p.level - 1) * (p.level - 1) * 100)) /
                  (p.level * p.level * 100 - (p.level - 1) * (p.level - 1) * 100)) * 100)
                : 100;
            xpFill.style.width = Math.max(5, pct) + '%';

            if (p.role === 'TEACHER') {
                adminNav.style.display = '';
            }

            $('avatar').textContent = p.role === 'TEACHER' ? '🧙' : '⚔';
        } catch (e) {
            console.error('Profile load failed', e);
        }
    }

    // ── Lessons (Quests) ──
    async function loadLessons() {
        questMap.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            const lessons = await API.get('/lessons');
            if (!lessons.length) {
                questMap.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🗺</div>
                        <p>Подземелья ещё не созданы.<br>Ожидайте — мастер готовит квесты!</p>
                    </div>`;
                return;
            }
            questMap.innerHTML = lessons.map(l => `
                <div class="quest-card" data-id="${l.id}">
                    <div class="quest-order">ПОДЗЕМЕЛЬЕ #${l.orderIndex + 1}</div>
                    <h3>${esc(l.title)}</h3>
                    <p>${esc(l.description || '')}</p>
                    <div class="quest-meta">
                        <span>⚔ ${l.taskCount} задач</span>
                        <span>👤 ${esc(l.authorName)}</span>
                    </div>
                </div>
            `).join('');

            questMap.querySelectorAll('.quest-card').forEach(card => {
                card.addEventListener('click', () => openLesson(+card.dataset.id));
            });
        } catch (e) {
            questMap.innerHTML = `<div class="empty-state"><p>Ошибка: ${esc(e.message)}</p></div>`;
        }
    }

    // ── Single Lesson ──
    async function openLesson(id) {
        state.currentLessonId = id;
        navigateToView('lesson');
        viewTitle.textContent = 'Подземелье';

        try {
            const lesson = await API.get('/lessons/' + id);
            $('lessonTitle').textContent = lesson.title;
            $('lessonContent').innerHTML = lesson.content || '<p>Теория скоро будет добавлена...</p>';

            const tasks = await API.get('/tasks/lesson/' + id);
            if (!tasks.length) {
                taskList.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">⚔</div>
                        <p>Задачи для этого подземелья ещё не созданы.</p>
                    </div>`;
                return;
            }
            taskList.innerHTML = tasks.map(t => `
                <div class="task-item" data-id="${t.id}">
                    <div class="task-status-icon ${t.solved ? 'solved' : 'unsolved'}">
                        ${t.solved ? '✅' : '⬜'}
                    </div>
                    <div class="task-info">
                        <h4>${esc(t.title)}</h4>
                        <span class="task-diff diff-${t.difficulty.toLowerCase()}">${diffLabel(t.difficulty)}</span>
                    </div>
                    <span class="task-xp">+${t.xpReward} XP</span>
                </div>
            `).join('');

            taskList.querySelectorAll('.task-item').forEach(item => {
                item.addEventListener('click', () => openTask(+item.dataset.id));
            });
        } catch (e) {
            $('lessonContent').innerHTML = `<p>Ошибка: ${esc(e.message)}</p>`;
        }
    }

    // ── Single Task ──
    async function openTask(id) {
        state.currentTaskId = id;
        navigateToView('task');
        viewTitle.textContent = 'Битва';
        resultPanel.classList.remove('visible', 'correct', 'wrong');

        try {
            const t = await API.get('/tasks/' + id);
            $('taskTitle').textContent = t.title;
            $('taskDiff').textContent = diffLabel(t.difficulty);
            $('taskDiff').className = 'task-diff diff-' + t.difficulty.toLowerCase();
            $('taskXp').textContent = '+' + t.xpReward + ' XP';
            $('taskDesc').innerHTML = t.description || 'Описание задачи.';

            if (t.hints) {
                $('hintBox').style.display = '';
                $('hintText').textContent = t.hints;
            } else {
                $('hintBox').style.display = 'none';
            }

            codeEditor.value = t.templateCode || '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}';
        } catch (e) {
            $('taskDesc').textContent = 'Ошибка: ' + e.message;
        }
    }

    // ── Submit ──
    $('submitBtn').addEventListener('click', async () => {
        const code = codeEditor.value.trim();
        if (!code) return;

        const btn = $('submitBtn');
        const spinner = $('submitSpinner');
        btn.disabled = true;
        spinner.style.display = '';

        try {
            const res = await API.post('/submissions', {
                taskId: state.currentTaskId,
                code: code
            });

            resultPanel.classList.remove('correct', 'wrong');
            resultPanel.classList.add('visible', res.status === 'CORRECT' ? 'correct' : 'wrong');

            $('resultHeader').textContent = res.status === 'CORRECT'
                ? '✅ ПОБЕДА!' : '❌ Неверно';
            $('resultBody').textContent = res.output;

            if (res.xpEarned > 0) {
                $('resultXp').textContent = '+ ' + res.xpEarned + ' XP получено!';
                $('resultXp').style.display = '';
            } else {
                $('resultXp').style.display = 'none';
            }

            if (res.newAchievements && res.newAchievements.length > 0) {
                res.newAchievements.forEach(a => showToast(a.icon, a.name, a.description));
            }

            loadProfile();
        } catch (e) {
            resultPanel.classList.add('visible', 'wrong');
            $('resultHeader').textContent = '⚠ Ошибка';
            $('resultBody').textContent = e.message;
            $('resultXp').style.display = 'none';
        } finally {
            btn.disabled = false;
            spinner.style.display = 'none';
        }
    });

    // ── Achievements ──
    async function loadAchievements() {
        achGrid.innerHTML = '<div class="empty-state"><span class="spinner"></span></div>';
        try {
            await loadProfile();
            const achs = state.profile.achievements;
            if (!achs.length) {
                achGrid.innerHTML = `
                    <div class="empty-state">
                        <div class="empty-icon">🏆</div>
                        <p>Пока нет достижений.<br>Решайте задачи, чтобы получить награды!</p>
                    </div>`;
                return;
            }
            achGrid.innerHTML = achs.map(a => `
                <div class="ach-card">
                    <div class="ach-icon">${a.icon}</div>
                    <div class="ach-info">
                        <h4>${esc(a.name)}</h4>
                        <p>${esc(a.description)}</p>
                        <div class="ach-xp">+${a.xpReward} XP</div>
                    </div>
                </div>
            `).join('');
        } catch (e) {
            achGrid.innerHTML = `<div class="empty-state"><p>Ошибка: ${esc(e.message)}</p></div>`;
        }
    }

    // ── Admin: Create Lesson ──
    $('createLessonForm').addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            await API.post('/admin/lessons', {
                title: fd.get('title'),
                description: fd.get('description'),
                content: fd.get('content'),
                orderIndex: +fd.get('orderIndex')
            });
            e.target.reset();
            showToast('📖', 'Урок создан', 'Новое подземелье доступно на карте');
        } catch (err) {
            alert('Ошибка: ' + err.message);
        }
    });

    // ── Admin: Create Task ──
    $('createTaskForm').addEventListener('submit', async e => {
        e.preventDefault();
        const fd = new FormData(e.target);
        try {
            await API.post('/admin/tasks', {
                lessonId: +fd.get('lessonId'),
                title: fd.get('title'),
                description: fd.get('description'),
                difficulty: fd.get('difficulty'),
                xpReward: +fd.get('xpReward'),
                templateCode: fd.get('templateCode'),
                expectedOutput: fd.get('expectedOutput'),
                hints: fd.get('hints'),
                orderIndex: +fd.get('orderIndex')
            });
            e.target.reset();
            showToast('⚔', 'Задача создана', 'Новый враг ждёт героев');
        } catch (err) {
            alert('Ошибка: ' + err.message);
        }
    });

    // ── Helpers ──
    function navigateToView(view) {
        document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
        $('view-' + view).classList.add('active');
    }

    function diffLabel(d) {
        return { EASY: '🟢 Лёгкая', MEDIUM: '🟡 Средняя', HARD: '🔴 Сложная' }[d] || d;
    }

    function esc(str) {
        const d = document.createElement('div');
        d.textContent = str;
        return d.innerHTML;
    }

    function showToast(icon, title, desc) {
        const t = document.createElement('div');
        t.className = 'toast';
        t.innerHTML = `
            <span class="toast-icon">${icon}</span>
            <div class="toast-text">
                <h4>🎉 ${esc(title)}</h4>
                <p>${esc(desc)}</p>
            </div>`;
        toasts.appendChild(t);
        setTimeout(() => t.remove(), 4000);
    }

    // Tab support in code editor
    codeEditor.addEventListener('keydown', e => {
        if (e.key === 'Tab') {
            e.preventDefault();
            const start = codeEditor.selectionStart;
            const end = codeEditor.selectionEnd;
            codeEditor.value = codeEditor.value.substring(0, start) + '    ' + codeEditor.value.substring(end);
            codeEditor.selectionStart = codeEditor.selectionEnd = start + 4;
        }
    });

    // ── Init ──
    loadProfile();
    loadLessons();
});
