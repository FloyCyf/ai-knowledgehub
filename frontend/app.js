(function () {
  "use strict";

  const storageKeys = {
    baseUrl: "akh.frontend.baseUrl",
    token: "akh.frontend.token",
    user: "akh.frontend.user"
  };

  const state = {
    baseUrl: localStorage.getItem(storageKeys.baseUrl) || "http://localhost:8080",
    token: localStorage.getItem(storageKeys.token) || "",
    user: readJson(storageKeys.user),
    currentArticleId: "",
    streamAbort: null,
    toastTimer: null
  };

  const $ = (id) => document.getElementById(id);

  const els = {
    baseUrl: $("baseUrl"),
    saveBaseUrl: $("saveBaseUrl"),
    authStatus: $("authStatus"),
    username: $("username"),
    password: $("password"),
    registerBtn: $("registerBtn"),
    loginBtn: $("loginBtn"),
    profileBtn: $("profileBtn"),
    logoutBtn: $("logoutBtn"),
    userSummary: $("userSummary"),
    articleTitle: $("articleTitle"),
    articleSummary: $("articleSummary"),
    articleContent: $("articleContent"),
    createDraftBtn: $("createDraftBtn"),
    publishBtn: $("publishBtn"),
    refreshArticlesBtn: $("refreshArticlesBtn"),
    currentArticleBadge: $("currentArticleBadge"),
    currentArticleId: $("currentArticleId"),
    loadDetailBtn: $("loadDetailBtn"),
    articleDetail: $("articleDetail"),
    articleList: $("articleList"),
    likeBtn: $("likeBtn"),
    commentContent: $("commentContent"),
    commentBtn: $("commentBtn"),
    loadCommentsBtn: $("loadCommentsBtn"),
    commentList: $("commentList"),
    rankingBtn: $("rankingBtn"),
    rankingList: $("rankingList"),
    aiPrompt: $("aiPrompt"),
    aiContinueBtn: $("aiContinueBtn"),
    aiStreamBtn: $("aiStreamBtn"),
    aiStopBtn: $("aiStopBtn"),
    aiResult: $("aiResult"),
    clearLogBtn: $("clearLogBtn"),
    responseLog: $("responseLog"),
    toast: $("toast")
  };

  function readJson(key) {
    try {
      const raw = localStorage.getItem(key);
      return raw ? JSON.parse(raw) : null;
    } catch (error) {
      return null;
    }
  }

  function writeJson(key, value) {
    localStorage.setItem(key, JSON.stringify(value));
  }

  function normalizeBaseUrl(url) {
    return (url || "").trim().replace(/\/+$/, "");
  }

  function escapeHtml(value) {
    return String(value ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function formatJson(value) {
    return JSON.stringify(value, null, 2);
  }

  function setLog(title, payload) {
    els.responseLog.textContent = `${title}\n\n${formatJson(payload)}`;
  }

  function showToast(message, type) {
    window.clearTimeout(state.toastTimer);
    els.toast.textContent = message;
    els.toast.className = `toast show ${type || ""}`.trim();
    state.toastTimer = window.setTimeout(() => {
      els.toast.className = "toast";
    }, 2600);
  }

  function setLoading(button, loadingText) {
    const oldText = button.textContent;
    button.disabled = true;
    button.textContent = loadingText;
    return () => {
      button.disabled = false;
      button.textContent = oldText;
    };
  }

  function headers(hasBody) {
    const result = {};
    if (hasBody) {
      result["Content-Type"] = "application/json";
    }
    if (state.token) {
      result.Authorization = `Bearer ${state.token}`;
    }
    return result;
  }

  async function api(path, options) {
    const opts = options || {};
    const hasBody = Object.prototype.hasOwnProperty.call(opts, "body");
    const response = await fetch(`${state.baseUrl}${path}`, {
      method: opts.method || "GET",
      headers: {
        ...headers(hasBody),
        ...(opts.headers || {})
      },
      body: hasBody ? JSON.stringify(opts.body) : undefined
    });

    const text = await response.text();
    let payload = null;
    try {
      payload = text ? JSON.parse(text) : null;
    } catch (error) {
      payload = { code: response.status, message: text || response.statusText, data: null };
    }

    if (!response.ok || (payload && payload.code && payload.code !== 200)) {
      const message = payload && payload.message ? payload.message : `HTTP ${response.status}`;
      const apiError = new Error(message);
      apiError.payload = payload;
      apiError.status = response.status;
      throw apiError;
    }

    return payload;
  }

  async function run(button, loadingText, title, task) {
    const done = setLoading(button, loadingText);
    try {
      const payload = await task();
      setLog(title, payload);
      showToast("操作成功", "success");
      return payload;
    } catch (error) {
      const payload = error.payload || {
        message: error.message || "请求失败",
        hint: "请确认后端服务和网关地址是否可用。"
      };
      setLog(`${title} - 失败`, payload);
      showToast(payload.message || error.message || "请求失败", "error");
      return null;
    } finally {
      done();
    }
  }

  function updateAuthView() {
    if (state.token && state.user) {
      els.authStatus.textContent = `${state.user.username || "已登录"} / ${state.user.role || "USER"}`;
      els.authStatus.classList.remove("muted");
      els.userSummary.textContent = `用户 ID: ${state.user.id || "-"}，用户名: ${state.user.username || "-"}，角色: ${state.user.role || "-"}`;
    } else if (state.token) {
      els.authStatus.textContent = "已保存 Token";
      els.authStatus.classList.remove("muted");
      els.userSummary.textContent = "已保存 Token，可点击“查看用户”同步用户信息。";
    } else {
      els.authStatus.textContent = "未登录";
      els.authStatus.classList.add("muted");
      els.userSummary.textContent = "当前没有登录用户。";
    }
  }

  function setCurrentArticleId(id) {
    state.currentArticleId = id ? String(id) : "";
    els.currentArticleId.value = state.currentArticleId;
    els.currentArticleBadge.textContent = state.currentArticleId ? `文章 #${state.currentArticleId}` : "未选择文章";
  }

  function getCurrentArticleId() {
    const id = els.currentArticleId.value.trim();
    if (!id) {
      showToast("请先创建、选择或输入文章 ID", "error");
      return "";
    }
    setCurrentArticleId(id);
    return id;
  }

  function renderArticleDetail(article) {
    if (!article) {
      els.articleDetail.className = "detail-box empty";
      els.articleDetail.textContent = "暂无文章详情。";
      return;
    }

    els.articleDetail.className = "detail-box";
    els.articleDetail.innerHTML = `
      <h3>${escapeHtml(article.title || `文章 #${article.id}`)}</h3>
      <div class="detail-meta">
        <span>ID ${escapeHtml(article.id)}</span>
        <span>状态 ${escapeHtml(article.status || "-")}</span>
        <span>阅读 ${escapeHtml(article.viewCount ?? 0)}</span>
        <span>点赞 ${escapeHtml(article.likeCount ?? 0)}</span>
        <span>评论 ${escapeHtml(article.commentCount ?? 0)}</span>
      </div>
      <p><strong>摘要：</strong>${escapeHtml(article.summary || "-")}</p>
      <p>${escapeHtml(article.content || "列表接口可能不返回正文，点击详情后查看完整内容。")}</p>
    `;
  }

  function renderArticleList(data) {
    const list = data && Array.isArray(data.list) ? data.list : [];
    if (!list.length) {
      els.articleList.className = "list empty";
      els.articleList.textContent = "暂无文章。";
      return;
    }

    els.articleList.className = "list";
    els.articleList.innerHTML = list.map((article) => `
      <div class="article-row">
        <div>
          <h3>${escapeHtml(article.title || `文章 #${article.id}`)}</h3>
          <p>ID ${escapeHtml(article.id)} · 阅读 ${escapeHtml(article.viewCount ?? 0)} · 点赞 ${escapeHtml(article.likeCount ?? 0)} · 评论 ${escapeHtml(article.commentCount ?? 0)}</p>
          <p>${escapeHtml(article.summary || "")}</p>
        </div>
        <button type="button" data-action="detail" data-id="${escapeHtml(article.id)}">查看</button>
      </div>
    `).join("");
  }

  function renderComments(data) {
    const list = data && Array.isArray(data.list) ? data.list : [];
    if (!list.length) {
      els.commentList.className = "list compact-list empty";
      els.commentList.textContent = "暂无评论。";
      return;
    }

    els.commentList.className = "list compact-list";
    els.commentList.innerHTML = list.map((comment) => `
      <div class="comment-row">
        <p>${escapeHtml(comment.content)}</p>
        <small>ID ${escapeHtml(comment.id)} · 用户 ${escapeHtml(comment.userId)} · ${escapeHtml(comment.createdAt || "")}</small>
      </div>
    `).join("");
  }

  function renderRanking(data) {
    const articles = data && Array.isArray(data.articles) ? data.articles : [];
    if (!articles.length) {
      els.rankingList.className = "ranking-list empty";
      els.rankingList.textContent = "暂无排行榜数据。";
      return;
    }

    els.rankingList.className = "ranking-list";
    els.rankingList.innerHTML = articles.map((article, index) => `
      <div class="rank-row">
        <span class="rank-number">${index + 1}</span>
        <strong>文章 #${escapeHtml(article.articleId)}</strong>
        <span>热度 ${escapeHtml(article.hotScore ?? 0)}</span>
      </div>
    `).join("");
  }

  function setAiResult(text, empty) {
    els.aiResult.className = empty ? "detail-box empty" : "detail-box";
    els.aiResult.textContent = text;
  }

  async function register() {
    await run(els.registerBtn, "注册中", "POST /api/user/register", async () => {
      return api("/api/user/register", {
        method: "POST",
        body: {
          username: els.username.value.trim(),
          password: els.password.value
        }
      });
    });
  }

  async function login() {
    const payload = await run(els.loginBtn, "登录中", "POST /api/user/login", async () => {
      return api("/api/user/login", {
        method: "POST",
        body: {
          username: els.username.value.trim(),
          password: els.password.value
        }
      });
    });

    if (payload && payload.data) {
      state.token = payload.data.token || "";
      state.user = payload.data.user || null;
      localStorage.setItem(storageKeys.token, state.token);
      if (state.user) {
        writeJson(storageKeys.user, state.user);
      }
      updateAuthView();
    }
  }

  async function logout() {
    await run(els.logoutBtn, "退出中", "POST /api/user/logout", async () => {
      if (!state.token) {
        return { code: 200, message: "本地已无 Token", data: null };
      }
      return api("/api/user/logout", { method: "POST" });
    });
    state.token = "";
    state.user = null;
    localStorage.removeItem(storageKeys.token);
    localStorage.removeItem(storageKeys.user);
    updateAuthView();
  }

  async function profile() {
    const payload = await run(els.profileBtn, "读取中", "GET /api/user/profile", async () => {
      return api("/api/user/profile");
    });
    if (payload && payload.data) {
      state.user = payload.data;
      writeJson(storageKeys.user, state.user);
      updateAuthView();
    }
  }

  async function createDraft() {
    const payload = await run(els.createDraftBtn, "创建中", "POST /api/articles/draft", async () => {
      return api("/api/articles/draft", {
        method: "POST",
        body: {
          title: els.articleTitle.value.trim(),
          summary: els.articleSummary.value.trim(),
          content: els.articleContent.value.trim()
        }
      });
    });

    if (payload && payload.data && payload.data.articleId) {
      setCurrentArticleId(payload.data.articleId);
    }
  }

  async function publishArticle() {
    const id = getCurrentArticleId();
    if (!id) return;
    await run(els.publishBtn, "发布中", `POST /api/articles/${id}/publish`, async () => {
      return api(`/api/articles/${encodeURIComponent(id)}/publish`, { method: "POST" });
    });
  }

  async function refreshArticles() {
    const payload = await run(els.refreshArticlesBtn, "刷新中", "GET /api/articles/latest", async () => {
      return api("/api/articles/latest?page=1&size=10");
    });
    if (payload) {
      renderArticleList(payload.data);
    }
  }

  async function loadDetail() {
    const id = getCurrentArticleId();
    if (!id) return;
    const payload = await run(els.loadDetailBtn, "读取中", `GET /api/articles/${id}`, async () => {
      return api(`/api/articles/${encodeURIComponent(id)}`);
    });
    if (payload) {
      renderArticleDetail(payload.data);
    }
  }

  async function likeArticle() {
    const id = getCurrentArticleId();
    if (!id) return;
    await run(els.likeBtn, "点赞中", `POST /api/articles/${id}/like`, async () => {
      return api(`/api/articles/${encodeURIComponent(id)}/like`, { method: "POST" });
    });
  }

  async function postComment() {
    const id = getCurrentArticleId();
    if (!id) return;
    await run(els.commentBtn, "提交中", `POST /api/articles/${id}/comments`, async () => {
      return api(`/api/articles/${encodeURIComponent(id)}/comments`, {
        method: "POST",
        body: {
          content: els.commentContent.value.trim()
        }
      });
    });
  }

  async function loadComments() {
    const id = getCurrentArticleId();
    if (!id) return;
    const payload = await run(els.loadCommentsBtn, "读取中", `GET /api/articles/${id}/comments`, async () => {
      return api(`/api/articles/${encodeURIComponent(id)}/comments?page=1&size=10`);
    });
    if (payload) {
      renderComments(payload.data);
    }
  }

  async function loadRanking() {
    const payload = await run(els.rankingBtn, "刷新中", "GET /api/ranking/top10", async () => {
      return api("/api/ranking/top10");
    });
    if (payload) {
      renderRanking(payload.data);
    }
  }

  async function aiContinue() {
    const prompt = els.aiPrompt.value.trim();
    const payload = await run(els.aiContinueBtn, "续写中", "POST /api/ai/continue-writing", async () => {
      return api("/api/ai/continue-writing", {
        method: "POST",
        body: { prompt }
      });
    });
    if (payload && payload.data) {
      const model = payload.data.modelName ? `模型：${payload.data.modelName}\n\n` : "";
      setAiResult(`${model}${payload.data.content || ""}`, false);
    }
  }

  async function aiStream() {
    const prompt = els.aiPrompt.value.trim();
    if (!prompt) {
      showToast("请输入 Prompt", "error");
      return;
    }

    if (state.streamAbort) {
      state.streamAbort.abort();
    }
    state.streamAbort = new AbortController();
    const done = setLoading(els.aiStreamBtn, "流式输出中");
    setAiResult("", false);

    try {
      const response = await fetch(`${state.baseUrl}/api/ai/continue-writing/stream?prompt=${encodeURIComponent(prompt)}`, {
        method: "GET",
        headers: headers(false),
        signal: state.streamAbort.signal
      });

      if (!response.ok || !response.body) {
        const text = await response.text();
        throw new Error(text || `HTTP ${response.status}`);
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder("utf-8");
      let buffer = "";
      let output = "";

      while (true) {
        const { value, done: streamDone } = await reader.read();
        if (streamDone) break;

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split(/\r?\n\r?\n/);
        buffer = events.pop() || "";

        events.forEach((eventText) => {
          eventText.split(/\r?\n/).forEach((line) => {
            if (line.startsWith("data:")) {
              output += line.slice(5).trimStart();
              setAiResult(output, false);
            }
          });
        });
      }

      if (buffer.trim()) {
        buffer.split(/\r?\n/).forEach((line) => {
          if (line.startsWith("data:")) {
            output += line.slice(5).trimStart();
          }
        });
        setAiResult(output, false);
      }

      setLog("GET /api/ai/continue-writing/stream", {
        code: 200,
        message: "stream completed",
        data: { content: output }
      });
      showToast("流式输出完成", "success");
    } catch (error) {
      if (error.name === "AbortError") {
        setLog("GET /api/ai/continue-writing/stream", {
          code: 0,
          message: "stream aborted",
          data: null
        });
        showToast("已停止流式输出");
      } else {
        setLog("GET /api/ai/continue-writing/stream - 失败", {
          message: error.message || "流式请求失败"
        });
        showToast(error.message || "流式请求失败", "error");
      }
    } finally {
      state.streamAbort = null;
      done();
    }
  }

  function stopStream() {
    if (state.streamAbort) {
      state.streamAbort.abort();
    }
  }

  function bindEvents() {
    els.saveBaseUrl.addEventListener("click", () => {
      state.baseUrl = normalizeBaseUrl(els.baseUrl.value);
      els.baseUrl.value = state.baseUrl;
      localStorage.setItem(storageKeys.baseUrl, state.baseUrl);
      showToast("网关地址已保存", "success");
    });

    els.currentArticleId.addEventListener("change", () => setCurrentArticleId(els.currentArticleId.value.trim()));
    els.registerBtn.addEventListener("click", register);
    els.loginBtn.addEventListener("click", login);
    els.logoutBtn.addEventListener("click", logout);
    els.profileBtn.addEventListener("click", profile);
    els.createDraftBtn.addEventListener("click", createDraft);
    els.publishBtn.addEventListener("click", publishArticle);
    els.refreshArticlesBtn.addEventListener("click", refreshArticles);
    els.loadDetailBtn.addEventListener("click", loadDetail);
    els.likeBtn.addEventListener("click", likeArticle);
    els.commentBtn.addEventListener("click", postComment);
    els.loadCommentsBtn.addEventListener("click", loadComments);
    els.rankingBtn.addEventListener("click", loadRanking);
    els.aiContinueBtn.addEventListener("click", aiContinue);
    els.aiStreamBtn.addEventListener("click", aiStream);
    els.aiStopBtn.addEventListener("click", stopStream);
    els.clearLogBtn.addEventListener("click", () => {
      els.responseLog.textContent = "等待请求...";
    });

    els.articleList.addEventListener("click", (event) => {
      const target = event.target;
      if (target instanceof HTMLElement && target.dataset.action === "detail") {
        setCurrentArticleId(target.dataset.id || "");
        loadDetail();
      }
    });
  }

  function init() {
    els.baseUrl.value = state.baseUrl;
    updateAuthView();
    bindEvents();
  }

  init();
})();
