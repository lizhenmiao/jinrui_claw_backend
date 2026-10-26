const $ = (id) => document.getElementById(id);

const state = {
  tab: "dashboard",
  token: localStorage.getItem("xlxJwtToken") || "",
  username: localStorage.getItem("xlxAdminUsername") || "",
  licenses: [],
  versions: [],
  models: [],
  eventsPage: 1,
  eventsPageSize: 20,
  licensePage: null,
  devicePage: null,
  versionPage: null,
  licenseKeySearch: "",
  customerNameSearch: "",
  statusSearch: "",
  usbIdSearch: "",
  deviceLicenseKeySearch: "",
  deviceHostnameSearch: "",
  deviceUsbIdSearch: "",
  deviceIpSearch: "",
  versionKeywordSearch: "",
  versionPlatformSearch: "",
  versionArchSearch: "",
  versionPublishedSearch: "",
  reports: [],
  reportPage: null,
  reportPageSize: 20,
  reportKeywordSearch: "",
  reportSceneSearch: "",
  reportStatusSearch: "",
  reportLicenseKeySearch: ""
};

const tabTitles = {
  dashboard: "首页",
  licenses: "授权",
  devices: "设备",
  versions: "版本",
  models: "模型",
  events: "日志",
  reports: "反馈"
};

init();

function init() {
  $("username").value = state.username || "admin";
  initLicenseDialog();
  initVersionDialog();
  initModelDialog();
  initReportDialog();

  document.querySelectorAll("aside button").forEach((btn) => {
    btn.addEventListener("click", () => setTab(btn.dataset.tab));
  });

  $("loginForm").addEventListener("submit", login);
  $("logout").addEventListener("click", logout);
  $("refresh").addEventListener("click", load);
  $("createLicense").addEventListener("click", () => openLicenseDialog());
  $("createVersion").addEventListener("click", () => openVersionDialog());
  $("createModel").addEventListener("click", () => openModelDialog());

  $("licenseSearchBtn").addEventListener("click", () => {
    state.licenseKeySearch = $("licenseKeySearch").value.trim();
    state.customerNameSearch = $("customerNameSearch").value.trim();
    state.statusSearch = $("statusSearch").value;
    state.usbIdSearch = $("usbIdSearch").value.trim();
    loadLicenses(1);
  });
  ["licenseKeySearch", "customerNameSearch", "usbIdSearch"].forEach((id) => {
    $(id).addEventListener("keydown", (e) => {
      if (e.key === "Enter") $("licenseSearchBtn").click();
    });
  });
  $("statusSearch").addEventListener("change", () => $("licenseSearchBtn").click());

  $("deviceSearchBtn").addEventListener("click", () => {
    state.deviceLicenseKeySearch = $("deviceLicenseKeySearch").value.trim();
    state.deviceHostnameSearch = $("deviceHostnameSearch").value.trim();
    state.deviceUsbIdSearch = $("deviceUsbIdSearch").value.trim();
    state.deviceIpSearch = $("deviceIpSearch").value.trim();
    loadDevices(1);
  });
  ["deviceLicenseKeySearch", "deviceHostnameSearch", "deviceUsbIdSearch", "deviceIpSearch"].forEach((id) => {
    $(id).addEventListener("keydown", (e) => {
      if (e.key === "Enter") $("deviceSearchBtn").click();
    });
  });

  $("versionSearchBtn").addEventListener("click", () => {
    state.versionKeywordSearch = $("versionKeywordSearch").value.trim();
    state.versionPlatformSearch = $("versionPlatformSearch").value;
    state.versionArchSearch = $("versionArchSearch").value;
    state.versionPublishedSearch = $("versionPublishedSearch").value;
    loadVersions(1);
  });
  $("versionKeywordSearch").addEventListener("keydown", (e) => {
    if (e.key === "Enter") $("versionSearchBtn").click();
  });
  ["versionPlatformSearch", "versionArchSearch", "versionPublishedSearch"].forEach((id) => {
    $(id).addEventListener("change", () => $("versionSearchBtn").click());
  });

  $("reportSearchBtn").addEventListener("click", () => {
    state.reportKeywordSearch = $("reportKeywordSearch").value.trim();
    state.reportSceneSearch = $("reportSceneSearch").value;
    state.reportStatusSearch = $("reportStatusSearch").value;
    state.reportLicenseKeySearch = $("reportLicenseKeySearch").value.trim();
    loadReports(1);
  });
  ["reportKeywordSearch", "reportLicenseKeySearch"].forEach((id) => {
    $(id).addEventListener("keydown", (e) => {
      if (e.key === "Enter") $("reportSearchBtn").click();
    });
  });
  ["reportSceneSearch", "reportStatusSearch"].forEach((id) => {
    $(id).addEventListener("change", () => $("reportSearchBtn").click());
  });

  refreshLoginState();
  load();
}

function setTab(tab) {
  state.tab = tab || "dashboard";
  document.querySelectorAll("aside button").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.tab === state.tab);
  });
  document.querySelectorAll(".panel").forEach((panel) => {
    panel.classList.toggle("active", panel.id === state.tab);
  });
  $("pageTitle").textContent = tabTitles[state.tab] || state.tab;
  load();
}

async function login(event) {
  event.preventDefault();
  clearLoginError();

  const username = $("username").value.trim();
  const password = $("password").value;
  if (!username || !password) {
    showLoginError("请输入用户名和密码");
    return;
  }

  const loginButton = $("login");
  loginButton.disabled = true;
  loginButton.textContent = "登录中...";

  try {
    const data = await publicApi("/api/admin/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password })
    });
    state.token = data.accessToken;
    state.username = data.username || username;
    localStorage.setItem("xlxJwtToken", state.token);
    localStorage.setItem("xlxAdminUsername", state.username);
    $("password").value = "";
    refreshLoginState();
    await load();
  } catch (error) {
    state.token = "";
    localStorage.removeItem("xlxJwtToken");
    showLoginError(normalizeLoginError(error));
    refreshLoginState();
  } finally {
    loginButton.disabled = false;
    loginButton.textContent = "登录";
  }
}

function logout() {
  state.token = "";
  localStorage.removeItem("xlxJwtToken");
  clearLoginError();
  refreshLoginState();
}

function expireLogin(message = "登录已过期，请重新登录") {
  state.token = "";
  localStorage.removeItem("xlxJwtToken");
  refreshLoginState();
  showLoginError(message);
}

function refreshLoginState() {
  const loggedIn = Boolean(state.token);
  $("loginPage").classList.toggle("hidden", loggedIn);
  $("adminApp").classList.toggle("hidden", !loggedIn);
  $("loginStatus").textContent = loggedIn ? `已登录：${state.username || "admin"}` : "";
  if (!loggedIn) {
    $("username").value = state.username || "admin";
    $("password").value = "";
  }
}

function showLoginError(message) {
  $("loginError").textContent = message || "登录失败，请稍后再试";
  $("loginError").classList.remove("hidden");
}

function clearLoginError() {
  $("loginError").textContent = "";
  $("loginError").classList.add("hidden");
}

function normalizeLoginError(error) {
  const code = error && error.code ? error.code : "";
  const message = error && error.message ? error.message : String(error || "");
  if (code === "LOGIN_FAILED" || message.includes("用户名") || message.includes("密码")) {
    return "用户名或密码不正确";
  }
  if (message.includes("Failed to fetch") || message.includes("NetworkError")) {
    return "无法连接后台服务，请确认服务已启动";
  }
  return message || "登录失败，请稍后再试";
}

async function publicApi(path, options = {}) {
  const res = await fetch(path, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  let json;
  try {
    json = await res.json();
  } catch {
    throw new Error("服务器响应异常");
  }
  if (!res.ok || !json.ok) {
    const error = new Error(json.message || json.code || "请求失败");
    error.code = json.code;
    throw error;
  }
  return json.data;
}

async function api(path, options = {}) {
  if (!state.token) throw new Error("请先登录后台");
  const headers = {
    Authorization: `Bearer ${state.token}`,
    ...(options.headers || {})
  };
  if (!(options.body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
  }
  const res = await fetch(path, {
    ...options,
    headers
  });
  let json;
  try {
    json = await res.json();
  } catch {
    throw new Error("服务器响应异常");
  }
  if (!res.ok || !json.ok) {
    const message = json.message || json.code || "请求失败";
    if (isAuthExpiredMessage(message)) {
      expireLogin();
      const error = new Error("登录已过期，请重新登录");
      error.code = json.code || "AUTH_EXPIRED";
      error.authExpired = true;
      throw error;
    }
    const error = new Error(json.message || json.code || "请求失败");
    error.code = json.code;
    throw error;
  }
  return json.data;
}

function isAuthExpiredMessage(message) {
  const text = String(message || "").toLowerCase();
  return text.includes("token")
    || text.includes("login expired")
    || text.includes("invalid")
    || text.includes("unauthorized")
    || text.includes("登录已过期")
    || text.includes("请先登录");
}

async function load() {
  try {
    if (!state.token) return;
    if (state.tab === "dashboard") await loadSummary();
    if (state.tab === "licenses") await loadLicenses();
    if (state.tab === "devices") await loadDevices();
    if (state.tab === "versions") await loadVersions();
    if (state.tab === "models") await loadModels();
    if (state.tab === "events") await loadEvents();
    if (state.tab === "reports") await loadReports();
  } catch (error) {
    if (error.authExpired) return;
    alert(error.message);
  }
}

async function loadSummary() {
  const data = await api("/api/admin/summary");
  $("summary").innerHTML = Object.entries(data).map(([key, value]) => (
    `<div class="metric"><span>${label(key)}</span><strong>${value}</strong></div>`
  )).join("");
}

async function loadLicenses(page) {
  $("licenseKeySearch").value = state.licenseKeySearch;
  $("customerNameSearch").value = state.customerNameSearch;
  $("statusSearch").value = state.statusSearch;
  $("usbIdSearch").value = state.usbIdSearch;
  page = page || (state.licensePage ? state.licensePage.current : 1);
  const params = new URLSearchParams({ page, pageSize: 20 });
  if (state.licenseKeySearch) params.set("licenseKey", state.licenseKeySearch);
  if (state.customerNameSearch) params.set("customerName", state.customerNameSearch);
  if (state.statusSearch) params.set("status", state.statusSearch);
  if (state.usbIdSearch) params.set("usbId", state.usbIdSearch);
  const data = await api(`/api/admin/licenses?${params}`);
  state.licensePage = data;
  state.licenses = data.records || [];
  renderTable("licenseTable", ["授权码", "客户", "状态", "U盘ID", "渠道", "模型组", "更新", "到期", "操作"], data.records.map((item) => [
    code(item.licenseKey),
    escapeHtml(item.customerName || ""),
    status(item.status),
    item.usbId ? code(item.usbId) : "<span class='muted'>未绑定</span>",
    escapeHtml(item.channel || ""),
    escapeHtml(item.modelGroup || ""),
    item.allowUpdate ? "允许" : "禁止",
    fmt(item.expiresAt),
    `<div class="row-actions">
      <button class="small secondary" type="button" onclick="openLicenseDialog(${item.id})">编辑</button>
      ${item.usbId ? `<button class="small" type="button" onclick="clearLicenseUsb(${item.id})">清空U盘</button>` : ""}
      <button class="small danger" type="button" onclick="deleteLicense(${item.id})">删除</button>
    </div>`
  ]));
  renderPagination("licensePager", data, (p) => loadLicenses(p));
}

async function loadDevices(page) {
  $("deviceLicenseKeySearch").value = state.deviceLicenseKeySearch;
  $("deviceHostnameSearch").value = state.deviceHostnameSearch;
  $("deviceUsbIdSearch").value = state.deviceUsbIdSearch;
  $("deviceIpSearch").value = state.deviceIpSearch;
  page = page || (state.devicePage ? state.devicePage.current : 1);
  const params = new URLSearchParams({ page, pageSize: 20 });
  if (state.deviceLicenseKeySearch) params.set("licenseKey", state.deviceLicenseKeySearch);
  if (state.deviceHostnameSearch) params.set("hostname", state.deviceHostnameSearch);
  if (state.deviceUsbIdSearch) params.set("usbId", state.deviceUsbIdSearch);
  if (state.deviceIpSearch) params.set("lastIp", state.deviceIpSearch);
  const data = await api(`/api/admin/devices?${params}`);
  state.devicePage = data;
  const rows = data.records || [];
  renderTable("deviceTable", ["授权ID", "授权码", "机器", "系统", "U盘", "版本", "启动次数", "网关", "最近网关", "最近启动", "IP", "记录数"], rows.map((item) => [
    item.licenseId,
    code(item.licenseKey || ""),
    escapeHtml(item.hostname || item.machineId || ""),
    escapeHtml(item.osName || ""),
    `${item.usbId ? code(item.usbId) : ""}<br><span class="muted">${escapeHtml(item.driveFs || "")}</span>`,
    escapeHtml(item.clientVersion || ""),
    item.launchCount || 0,
    gatewaySummary(item),
    gatewayRecent(item),
    fmt(item.lastSeenAt),
    escapeHtml(item.lastIp || ""),
    item.rawCount || 1
  ]));
  renderPagination("devicePager", data, (p) => loadDevices(p));
}

async function loadVersions(page) {
  $("versionKeywordSearch").value = state.versionKeywordSearch;
  $("versionPlatformSearch").value = state.versionPlatformSearch;
  $("versionArchSearch").value = state.versionArchSearch;
  $("versionPublishedSearch").value = state.versionPublishedSearch;
  page = page || (state.versionPage ? state.versionPage.current : 1);
  const params = new URLSearchParams({ page, pageSize: 20 });
  if (state.versionKeywordSearch) params.set("keyword", state.versionKeywordSearch);
  if (state.versionPlatformSearch) params.set("platform", state.versionPlatformSearch);
  if (state.versionArchSearch) params.set("arch", state.versionArchSearch);
  if (state.versionPublishedSearch) params.set("published", state.versionPublishedSearch);
  const data = await api(`/api/admin/versions?${params}`);
  state.versionPage = data;
  const rows = data.records || [];
  state.versions = rows;
  renderTable("versionTable", ["平台", "架构", "版本号", "渠道", "标题", "发布状态", "强制更新", "下载地址", "更新内容", "发布时间", "操作"], rows.map((item) => [
    escapeHtml(item.platform || "windows"),
    escapeHtml(item.arch || "x64"),
    code(item.version),
    escapeHtml(item.channel || ""),
    escapeHtml(item.title || ""),
    item.published ? "<span class='ok'>已发布</span>" : "<span class='muted'>草稿</span>",
    item.forceUpdate ? "<span class='bad'>是</span>" : "否",
    `<span class="url-cell">${escapeHtml(item.downloadUrl || "")}</span>`,
    item.releaseNotes ? `<div class="notes-cell">${escapeHtml(item.releaseNotes).replace(/\r?\n/g, "<br>")}</div>` : "",
    fmt(item.createdAt),
    `<div class="row-actions">
      <button class="small secondary" type="button" onclick="openVersionDialog(${item.id})">编辑</button>
      ${item.published ? `<button class="small" type="button" onclick="toggleVersionPublished(${item.id}, false)">下架</button>` : `<button class="small" type="button" onclick="toggleVersionPublished(${item.id}, true)">发布</button>`}
      <button class="small danger" type="button" onclick="deleteVersion(${item.id})">删除</button>
    </div>`
  ]));
  renderPagination("versionPager", data, (p) => loadVersions(p));
}

async function loadModels() {
  const rows = await api("/api/admin/models");
  state.models = rows;
  renderTable("modelTable", ["名称", "Provider", "地址", "Key来源", "默认模型", "状态", "渠道", "模型组", "Revision", "操作"], rows.map((item) => [
    escapeHtml(item.displayName || ""),
    code(item.providerId),
    `<span class="url-cell">${escapeHtml(item.baseUrl || "")}</span>`,
    keyModeLabel(item.keyMode),
    code(item.defaultModel || ""),
    item.enabled ? "<span class='ok'>启用</span>" : "<span class='bad'>禁用</span>",
    escapeHtml(item.channel || ""),
    escapeHtml(item.modelGroup || ""),
    item.revision,
    `<div class="row-actions">
      <button class="small secondary" type="button" onclick="openModelDialog(${item.id})">编辑</button>
      <button class="small danger" type="button" onclick="deleteModel(${item.id})">删除</button>
    </div>`
  ]));
}

async function loadEvents(page) {
  page = page || state.eventsPage;
  const data = await api(`/api/admin/events?page=${page}&pageSize=${state.eventsPageSize}`);
  const pageData = normalizePageData(data);
  const records = pageData.records;
  state.eventsPage = pageData.current;
  renderTable("eventTable", ["时间", "授权", "类型", "级别", "消息", "详情"], records.map((item) => [
    fmt(item.createdAt),
    code(item.licenseKey || ""),
    escapeHtml(item.eventType || ""),
    escapeHtml(item.level || ""),
    escapeHtml(item.message || ""),
    `<pre>${escapeHtml(item.detailsJson || "")}</pre>`
  ]));
  renderPagination("eventPager", pageData, (p) => loadEvents(p));
}

async function loadReports(page) {
  $("reportKeywordSearch").value = state.reportKeywordSearch;
  $("reportSceneSearch").value = state.reportSceneSearch;
  $("reportStatusSearch").value = state.reportStatusSearch;
  $("reportLicenseKeySearch").value = state.reportLicenseKeySearch;
  page = page || (state.reportPage ? state.reportPage.current : 1);
  const params = new URLSearchParams({ page, pageSize: state.reportPageSize });
  if (state.reportKeywordSearch) params.set("keyword", state.reportKeywordSearch);
  if (state.reportSceneSearch) params.set("scene", state.reportSceneSearch);
  if (state.reportStatusSearch) params.set("status", state.reportStatusSearch);
  if (state.reportLicenseKeySearch) params.set("licenseKey", state.reportLicenseKeySearch);
  const data = await api(`/api/admin/reports?${params}`);
  const pageData = normalizePageData(data);
  state.reportPage = pageData;
  state.reports = pageData.records;
  renderTable("reportTable", ["提交时间", "场景", "状态", "回执码", "版本", "授权码", "机器", "错误摘要", "操作"], pageData.records.map((item) => [
    fmt(item.createdAt),
    reportSceneLabel(item.scene),
    reportStatusLabel(item.status),
    code(item.reportNo || ""),
    escapeHtml(item.clientVersion || ""),
    code(item.licenseKey || "") || "<span class='muted'>未上报</span>",
    `${escapeHtml(item.hostname || item.machineId || "")}<br><span class="muted">${escapeHtml(item.osName || "")}</span>`,
    `<div class="notes-cell">${escapeHtml(item.errorSummary || item.remark || "")}</div>`,
    `<div class="row-actions">
      <button class="small secondary" type="button" onclick="openReportDialog(${item.id})">详情</button>
      <button class="small" type="button" onclick="downloadReportPackage(${item.id})">下载日志</button>
    </div>`
  ]));
  renderPagination("reportPager", pageData, (p) => loadReports(p));
}

function initReportDialog() {
  const dialog = document.createElement("div");
  dialog.id = "reportDialog";
  dialog.className = "modal hidden";
  dialog.innerHTML = `
    <div class="modal-mask" data-close-report-dialog></div>
    <div class="modal-card">
      <div class="modal-head">
        <div>
          <h3>反馈详情</h3>
          <p>客户端上报的诊断信息，改完状态和备注记得保存。日志包只能通过后台鉴权下载。</p>
        </div>
        <button class="icon-btn" type="button" data-close-report-dialog>×</button>
      </div>
      <form id="reportForm" class="model-form">
        <input id="reportId" type="hidden">
        <label>回执码<input id="reportNoInput" readonly></label>
        <label>提交时间<input id="reportCreatedInput" readonly></label>
        <label>场景<input id="reportSceneInput" readonly></label>
        <label>错误码<input id="reportErrorCodeInput" readonly></label>
        <label>客户端版本<input id="reportVersionInput" readonly></label>
        <label>授权码<input id="reportLicenseInput" readonly></label>
        <label>机器名<input id="reportHostInput" readonly></label>
        <label>系统 / 架构<input id="reportOsInput" readonly></label>
        <label>联系方式<input id="reportContactInput" readonly></label>
        <label>来源 IP<input id="reportIpInput" readonly></label>
        <label class="span-2">错误摘要<input id="reportSummaryInput" readonly></label>
        <label class="span-2">用户描述<input id="reportRemarkInput" readonly></label>
        <label>日志包<input id="reportFileInput" readonly></label>
        <label>SHA256<input id="reportShaInput" readonly></label>
        <label>处理状态
          <select id="reportStatusEdit">
            <option value="submitted">待处理</option>
            <option value="triaged">已受理</option>
            <option value="resolved">已解决</option>
            <option value="ignored">已忽略</option>
          </select>
        </label>
        <label>最近更新<input id="reportUpdatedInput" readonly></label>
        <label class="span-2">客服备注
          <textarea id="reportAdminNote" rows="4" placeholder="例如：已复现，用户环境缺 VC++ 运行库，已远程处理"></textarea>
        </label>
        <div class="modal-actions span-2">
          <button class="secondary" type="button" data-close-report-dialog>取消</button>
          <button class="secondary" type="button" id="reportDownloadButton">下载日志包</button>
          <button type="submit">保存处理结果</button>
        </div>
      </form>
    </div>
  `;
  document.body.appendChild(dialog);
  dialog.querySelectorAll("[data-close-report-dialog]").forEach((item) => {
    item.addEventListener("click", closeReportDialog);
  });
  $("reportForm").addEventListener("submit", saveReportFromDialog);
  $("reportDownloadButton").addEventListener("click", () => downloadReportPackage($("reportId").value));
}

// 详情接口返回完整字段（含 adminNote），列表数据可能被截断，所以打开时重新拉一次
async function openReportDialog(id) {
  let item;
  try {
    item = await api(`/api/admin/reports/${id}`);
  } catch (error) {
    if (error.authExpired) return;
    alert(error.message);
    return;
  }
  if (!item) return;
  $("reportId").value = item.id;
  $("reportNoInput").value = item.reportNo || "";
  $("reportCreatedInput").value = fmt(item.createdAt);
  $("reportSceneInput").value = reportSceneText(item.scene);
  $("reportErrorCodeInput").value = item.errorCode || "";
  $("reportVersionInput").value = item.clientVersion || "";
  $("reportLicenseInput").value = item.licenseKey || "";
  $("reportHostInput").value = item.hostname || item.machineId || "";
  $("reportOsInput").value = [item.osName, item.arch].filter(Boolean).join(" / ");
  $("reportContactInput").value = item.contact || "";
  $("reportIpInput").value = item.sourceIp || "";
  $("reportSummaryInput").value = item.errorSummary || "";
  $("reportRemarkInput").value = item.remark || "";
  $("reportFileInput").value = item.filePath ? `${item.filePath}（${formatBytes(item.fileSize || 0)}）` : "未保存";
  $("reportShaInput").value = item.fileSha256 || "";
  $("reportUpdatedInput").value = fmt(item.updatedAt);
  $("reportStatusEdit").value = item.status || "submitted";
  $("reportAdminNote").value = item.adminNote || "";
  $("reportDialog").classList.remove("hidden");
  $("reportStatusEdit").focus();
}

function closeReportDialog() {
  $("reportDialog").classList.add("hidden");
}

async function saveReportFromDialog(event) {
  event.preventDefault();
  const id = $("reportId").value;
  if (!id) return;
  await api(`/api/admin/reports/${id}`, {
    method: "PATCH",
    body: JSON.stringify({
      status: $("reportStatusEdit").value,
      adminNote: $("reportAdminNote").value.trim()
    })
  });
  closeReportDialog();
  await loadReports();
}

// 下载接口要带 Authorization 头，不能直接 window.open，用 fetch 拿 blob 再触发保存
async function downloadReportPackage(id) {
  if (!state.token) return;
  const item = (state.reports || []).find((row) => String(row.id) === String(id));
  const fileName = `${item && item.reportNo ? item.reportNo : "report-" + id}.zip`;
  try {
    const res = await fetch(`/api/admin/reports/${id}/download`, {
      headers: { Authorization: `Bearer ${state.token}` }
    });
    const contentType = res.headers.get("content-type") || "";
    if (!res.ok || contentType.includes("application/json")) {
      let message = "下载失败，日志包可能已被清理";
      try {
        const json = await res.json();
        message = json.message || json.code || message;
      } catch {
        // 非 JSON 错误体（如文件缺失返回的空 404），保留默认提示
      }
      if (isAuthExpiredMessage(message)) expireLogin();
      alert(message);
      return;
    }
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (error) {
    alert(error.message || "下载失败");
  }
}

function reportSceneText(value) {
  return {
    reset_failed: "重置失败",
    repair_failed: "启动修复失败",
    startup_failed: "启动失败",
    update_failed: "更新失败",
    manual: "用户主动提交",
    other: "其他"
  }[value] || value || "";
}

function reportSceneLabel(value) {
  const text = escapeHtml(reportSceneText(value));
  return value === "manual" || !value ? `<span class="muted">${text}</span>` : text;
}

function reportStatusLabel(value) {
  return {
    submitted: '<span class="tag server">待处理</span>',
    triaged: '<span class="tag client">已受理</span>',
    resolved: '<span class="ok">已解决</span>',
    ignored: '<span class="muted">已忽略</span>'
  }[value] || escapeHtml(value || "");
}

function initVersionDialog() {
  const dialog = document.createElement("div");
  dialog.id = "versionDialog";
  dialog.className = "modal hidden";
  dialog.innerHTML = `
    <div class="modal-mask" data-close-version-dialog></div>
    <div class="modal-card">
      <div class="modal-head">
        <div>
          <h3 id="versionDialogTitle">新增版本</h3>
          <p>配置客户端检查更新时返回的版本、下载地址、校验值和更新说明。</p>
        </div>
        <button class="icon-btn" type="button" data-close-version-dialog>×</button>
      </div>
      <form id="versionForm" class="model-form">
        <input id="versionId" type="hidden">
        <label>版本号<input id="versionNumber" required placeholder="2026.7.25"></label>
        <label>Platform
          <select id="versionPlatform">
            <option value="windows">windows</option>
            <option value="macos">macos</option>
          </select>
        </label>
        <label>Arch
          <select id="versionArch">
            <option value="x64">x64</option>
            <option value="arm64">arm64</option>
            <option value="universal">universal</option>
          </select>
        </label>
        <label>渠道<input id="versionChannel" required placeholder="stable"></label>
        <label class="span-2">更新标题<input id="versionTitle" placeholder="小龙虾U盘版 2026.7.25"></label>
        <div class="upload-box span-2">
          <div>
            <strong>更新包</strong>
            <span id="versionUploadStatus">可以上传 .zip，上传后自动回填下载地址和 SHA256。</span>
          </div>
          <input id="versionUploadFile" class="hidden" type="file" accept=".zip,application/zip">
          <button id="versionUploadButton" class="secondary" type="button">上传压缩包</button>
        </div>
        <label class="span-2">下载地址<input id="versionDownloadUrl"></label>
        <label class="span-2">SHA256<input id="versionSha256" placeholder="更新包的 sha256 校验值"></label>
        <label class="span-2">更新说明
          <textarea id="versionReleaseNotes" rows="6" placeholder="每行一条更新说明"></textarea>
        </label>
        <div class="check-row span-2">
          <label><input id="versionPublished" type="checkbox"> 发布</label>
          <label><input id="versionForceUpdate" type="checkbox"> 强制更新</label>
        </div>
        <div class="modal-actions span-2">
          <button class="secondary" type="button" data-close-version-dialog>取消</button>
          <button type="submit">保存版本</button>
        </div>
      </form>
    </div>
  `;
  document.body.appendChild(dialog);
  dialog.querySelectorAll("[data-close-version-dialog]").forEach((item) => {
    item.addEventListener("click", closeVersionDialog);
  });
  $("versionForm").addEventListener("submit", saveVersionFromDialog);
  $("versionUploadButton").addEventListener("click", () => $("versionUploadFile").click());
  $("versionUploadFile").addEventListener("change", uploadVersionPackage);
  $("versionPlatform").addEventListener("change", () => {
    if ($("versionPlatform").value === "macos") $("versionArch").value = "arm64";
    if ($("versionPlatform").value === "windows" && $("versionArch").value === "arm64") $("versionArch").value = "x64";
  });
}

function openVersionDialog(id) {
  const item = id ? state.versions.find((row) => row.id === id) : null;
  const defaultVersion = currentDateVersion();
  $("versionDialogTitle").textContent = item ? "编辑版本" : "新增版本";
  $("versionId").value = item ? item.id : "";
  $("versionNumber").value = item ? item.version || "" : defaultVersion;
  const platform = item ? item.platform || "windows" : "windows";
  $("versionPlatform").value = platform === "linux" ? "windows" : platform;
  $("versionArch").value = item ? item.arch || (platform === "macos" ? "arm64" : "x64") : "x64";
  $("versionChannel").value = item ? item.channel || "stable" : "stable";
  $("versionTitle").value = item ? item.title || "" : `小龙虾U盘版 ${defaultVersion}`;
  $("versionDownloadUrl").value = item ? item.downloadUrl || "" : "";
  $("versionSha256").value = item ? item.sha256 || "" : "";
  $("versionUploadFile").value = "";
  $("versionUploadStatus").textContent = "可以上传 .zip，上传后自动回填下载地址和 SHA256。";
  $("versionReleaseNotes").value = item ? item.releaseNotes || "" : "";
  $("versionPublished").checked = item ? Boolean(item.published) : true;
  $("versionForceUpdate").checked = item ? Boolean(item.forceUpdate) : false;
  $("versionDialog").classList.remove("hidden");
  $("versionNumber").focus();
}

function closeVersionDialog() {
  $("versionDialog").classList.add("hidden");
}

async function uploadVersionPackage() {
  const file = $("versionUploadFile").files && $("versionUploadFile").files[0];
  if (!file) return;
  if (!file.name.toLowerCase().endsWith(".zip")) {
    alert("目前只支持上传 .zip 更新包");
    $("versionUploadFile").value = "";
    return;
  }

  const button = $("versionUploadButton");
  const status = $("versionUploadStatus");
  const oldText = button.textContent;
  button.disabled = true;
  button.textContent = "上传中...";
  status.textContent = `${file.name} 正在上传，请稍等`;

  try {
    const formData = new FormData();
    formData.append("file", file);
    const data = await uploadWithProgress("/api/admin/versions/upload", formData, (loaded, total) => {
      const percent = total ? Math.floor(loaded * 100 / total) : 0;
      status.textContent = `${file.name} 上传中：${formatBytes(loaded)} / ${formatBytes(total)}（${percent}%）`;
    });
    $("versionDownloadUrl").value = data.downloadUrl || "";
    $("versionSha256").value = data.sha256 || "";
    status.textContent = `已上传：${data.fileName || file.name}（${formatBytes(data.size || file.size)}，SHA256 已自动回填）`;
  } catch (error) {
    if (error.authExpired) return;
    status.textContent = "上传失败";
    alert(error.message || "上传失败");
  } finally {
    button.disabled = false;
    button.textContent = oldText;
    $("versionUploadFile").value = "";
  }
}

// 带上传进度的 POST（fetch 不支持上传进度回调，这里用 XMLHttpRequest 实现）
function uploadWithProgress(path, formData, onProgress) {
  return new Promise((resolve, reject) => {
    if (!state.token) {
      reject(new Error("请先登录后台"));
      return;
    }
    const xhr = new XMLHttpRequest();
    xhr.open("POST", path);
    xhr.setRequestHeader("Authorization", `Bearer ${state.token}`);
    xhr.upload.addEventListener("progress", (event) => {
      if (event.lengthComputable) onProgress(event.loaded, event.total);
    });
    xhr.addEventListener("load", () => {
      let json;
      try {
        json = JSON.parse(xhr.responseText);
      } catch {
        reject(new Error("服务器响应异常"));
        return;
      }
      if (!json.ok) {
        const message = json.message || json.code || "请求失败";
        if (isAuthExpiredMessage(message)) {
          expireLogin();
          const error = new Error("登录已过期，请重新登录");
          error.authExpired = true;
          reject(error);
          return;
        }
        reject(new Error(message));
        return;
      }
      resolve(json.data);
    });
    xhr.addEventListener("error", () => reject(new Error("网络异常，上传失败")));
    xhr.addEventListener("abort", () => reject(new Error("上传已取消")));
    xhr.send(formData);
  });
}

function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < units.length - 1) {
    value /= 1024;
    unit += 1;
  }
  return `${value.toFixed(1)} ${units[unit]}`;
}

async function saveVersionFromDialog(event) {
  event.preventDefault();
  const id = $("versionId").value;
  const body = readVersionForm();
  if (!body.version) {
    alert("请填写版本号");
    return;
  }
  if (body.published && !body.downloadUrl) {
    alert("发布版本前请填写下载地址");
    return;
  }
  await api(id ? `/api/admin/versions/${id}` : "/api/admin/versions", {
    method: id ? "PATCH" : "POST",
    body: JSON.stringify(body)
  });
  closeVersionDialog();
  await loadVersions();
}

async function toggleVersionPublished(id, published) {
  const item = state.versions.find((row) => row.id === id);
  if (!item) return;
  if (published && !item.downloadUrl) {
    alert("发布版本前请先编辑并填写下载地址");
    return;
  }
  await saveVersion(id, { ...versionToRequest(item), published });
  await loadVersions();
}

async function deleteVersion(id) {
  const item = state.versions.find((row) => row.id === id);
  const name = item ? item.version : `ID ${id}`;
  if (!confirm(`确定删除版本“${name}”吗？删除后客户端不会再获取这个版本。`)) return;
  await api(`/api/admin/versions/${id}`, { method: "DELETE" });
  await loadVersions();
}

async function saveVersion(id, body) {
  await api(`/api/admin/versions/${id}`, {
    method: "PATCH",
    body: JSON.stringify(body)
  });
}

function readVersionForm() {
  return {
    version: $("versionNumber").value.trim(),
    platform: $("versionPlatform").value || "windows",
    arch: $("versionArch").value || "x64",
    channel: $("versionChannel").value.trim() || "stable",
    title: $("versionTitle").value.trim(),
    releaseNotes: $("versionReleaseNotes").value.trim(),
    downloadUrl: $("versionDownloadUrl").value.trim(),
    sha256: $("versionSha256").value.trim(),
    forceUpdate: $("versionForceUpdate").checked,
    published: $("versionPublished").checked
  };
}

function versionToRequest(item) {
  return {
    version: item.version,
    platform: item.platform || "windows",
    arch: item.arch || "x64",
    channel: item.channel || "stable",
    title: item.title || "",
    releaseNotes: item.releaseNotes || "",
    downloadUrl: item.downloadUrl || "",
    sha256: item.sha256 || "",
    forceUpdate: Boolean(item.forceUpdate),
    published: Boolean(item.published)
  };
}

function currentDateVersion() {
  const now = new Date();
  return `${now.getFullYear()}.${now.getMonth() + 1}.${now.getDate()}`;
}

function initLicenseDialog() {
  const dialog = document.createElement("div");
  dialog.id = "licenseDialog";
  dialog.className = "modal hidden";
  dialog.innerHTML = `
    <div class="modal-mask" data-close-license-dialog></div>
    <div class="modal-card">
      <div class="modal-head">
        <div>
          <h3 id="licenseDialogTitle">新增授权</h3>
          <p>编辑授权信息，U盘ID留空表示解除绑定。</p>
        </div>
        <button class="icon-btn" type="button" data-close-license-dialog>×</button>
      </div>
      <form id="licenseForm" class="model-form">
        <input id="licenseId" type="hidden">
        <label>授权码<input id="licenseKey" placeholder="留空自动生成，如 XLX-DEMO-0001"></label>
        <label>客户名称<input id="licenseCustomerName" placeholder="客户名称"></label>
        <label>U盘ID<input id="licenseUsbId" placeholder="留空表示未绑定，可重新绑定新U盘"></label>
        <label>状态
          <select id="licenseStatus">
            <option value="active">active</option>
            <option value="disabled">disabled</option>
            <option value="expired">expired</option>
            <option value="revoked">revoked</option>
          </select>
        </label>
        <label>渠道<input id="licenseChannel" placeholder="stable"></label>
        <label>模型组<input id="licenseModelGroup" placeholder="default"></label>
        <label>到期时间<input id="licenseExpiresAt" type="datetime-local"></label>
        <div class="check-row">
          <label><input id="licenseAllowUpdate" type="checkbox"> 允许更新</label>
        </div>
        <div class="modal-actions span-2">
          <button class="secondary" type="button" data-close-license-dialog>取消</button>
          <button type="submit">保存授权</button>
        </div>
      </form>
    </div>
  `;
  document.body.appendChild(dialog);
  dialog.querySelectorAll("[data-close-license-dialog]").forEach((item) => {
    item.addEventListener("click", closeLicenseDialog);
  });
  $("licenseForm").addEventListener("submit", saveLicenseFromDialog);
  $("licenseExpiresAt").addEventListener("change", syncLicenseStatusByExpiry);
}

function syncLicenseStatusByExpiry() {
  const value = $("licenseExpiresAt").value;
  const currentStatus = $("licenseStatus").value;
  if (!value || currentStatus === "disabled" || currentStatus === "revoked") return;
  $("licenseStatus").value = new Date(value).getTime() < Date.now() ? "expired" : "active";
}

function openLicenseDialog(id) {
  const rows = (state.licensePage && state.licensePage.records) || state.licenses || [];
  const item = id ? rows.find((row) => String(row.id) === String(id)) : null;
  if (id && !item) {
    alert("未找到这条授权记录，页面已刷新，请重新点编辑。");
    loadLicenses();
    return;
  }
  $("licenseDialogTitle").textContent = item ? "编辑授权" : "新增授权";
  $("licenseId").value = item ? item.id : "";
  $("licenseKey").value = item ? item.licenseKey || "" : "";
  $("licenseCustomerName").value = item ? item.customerName || "" : "";
  $("licenseUsbId").value = item ? item.usbId || "" : "";
  $("licenseStatus").value = item ? item.status || "active" : "active";
  $("licenseChannel").value = item ? item.channel || "stable" : "stable";
  $("licenseModelGroup").value = item ? item.modelGroup || "default" : "default";
  $("licenseExpiresAt").value = toDatetimeLocal(item ? item.expiresAt : "");
  $("licenseAllowUpdate").checked = item ? item.allowUpdate !== false : true;
  $("licenseDialog").classList.remove("hidden");
  $("licenseCustomerName").focus();
}

function closeLicenseDialog() {
  $("licenseDialog").classList.add("hidden");
}

async function saveLicenseFromDialog(event) {
  event.preventDefault();
  const id = $("licenseId").value;
  await api(id ? `/api/admin/licenses/${id}` : "/api/admin/licenses", {
    method: id ? "PATCH" : "POST",
    body: JSON.stringify({
      licenseKey: $("licenseKey").value.trim(),
      customerName: $("licenseCustomerName").value.trim(),
      usbId: $("licenseUsbId").value.trim(),
      status: $("licenseStatus").value || "active",
      expiresAt: fromDatetimeLocal($("licenseExpiresAt").value),
      allowUpdate: $("licenseAllowUpdate").checked,
      channel: $("licenseChannel").value.trim() || "stable",
      modelGroup: $("licenseModelGroup").value.trim() || "default"
    })
  });
  closeLicenseDialog();
  await loadLicenses();
}

async function clearLicenseUsb(id) {
  const item = (state.licensePage && state.licensePage.records || []).find((row) => row.id === id);
  if (!item) return;
  if (!confirm(`确定清空授权“${item.licenseKey || id}”的U盘ID吗？清空后客户端可以重新绑定当前U盘。`)) return;
  await api(`/api/admin/licenses/${id}`, {
    method: "PATCH",
    body: JSON.stringify({
      licenseKey: item.licenseKey,
      customerName: item.customerName,
      usbId: "",
      status: item.status || "active",
      expiresAt: item.expiresAt || null,
      allowUpdate: item.allowUpdate !== false,
      channel: item.channel || "stable",
      modelGroup: item.modelGroup || "default"
    })
  });
  await loadLicenses();
}

async function deleteLicense(id) {
  const item = (state.licensePage && state.licensePage.records || []).find((row) => row.id === id);
  const name = item ? item.licenseKey || `ID ${id}` : `ID ${id}`;
  if (!confirm(`确定删除授权“${name}”吗？删除后这个授权码将不能再通过客户端校验。`)) return;
  await api(`/api/admin/licenses/${id}`, { method: "DELETE" });
  await loadLicenses();
  await loadSummary();
}

function initModelDialog() {
  const dialog = document.createElement("div");
  dialog.id = "modelDialog";
  dialog.className = "modal hidden";
  dialog.innerHTML = `
    <div class="modal-mask" data-close-model-dialog></div>
    <div class="modal-card">
      <div class="modal-head">
        <div>
          <h3 id="modelDialogTitle">新增模型</h3>
          <p>配置后台下发给小龙虾客户端的模型供应商。</p>
        </div>
        <button class="icon-btn" type="button" data-close-model-dialog>×</button>
      </div>
      <form id="modelForm" class="model-form">
        <input id="modelId" type="hidden">
        <label>显示名称<input id="modelDisplayName" required placeholder="锦锐内置算力"></label>
        <label>Provider ID<input id="modelProviderId" required placeholder="jinrui-deepseek"></label>
        <label>API类型
          <select id="modelApiType">
            <option value="openai-completions">openai-completions</option>
            <option value="openai-responses">openai-responses</option>
          </select>
        </label>
        <label>Base URL<input id="modelBaseUrl" required placeholder="http://192.168.0.212:13000/v1"></label>
        <label>Key来源
          <select id="modelKeyMode">
            <option value="client">客户端填写Key</option>
            <option value="server">后台内置Key</option>
          </select>
        </label>
        <label id="modelApiKeyWrap">API Key<input id="modelApiKey" placeholder="后台内置时填写；编辑时留空表示不修改"></label>
        <label>默认模型<input id="modelDefaultModel" required placeholder="deepseek-v4-flash"></label>
        <label>渠道<input id="modelChannel" placeholder="stable"></label>
        <label>模型组<input id="modelGroup" placeholder="default"></label>
        <label class="span-2">模型列表JSON
          <textarea id="modelModelsJson" rows="7" required spellcheck="false"></textarea>
        </label>
        <div class="check-row span-2">
          <label><input id="modelEnabled" type="checkbox"> 启用</label>
          <label><input id="modelForceDefault" type="checkbox"> 强制设为默认模型</label>
        </div>
        <div class="modal-actions span-2">
          <button class="secondary" type="button" data-close-model-dialog>取消</button>
          <button type="submit">保存模型</button>
        </div>
      </form>
    </div>
  `;
  document.body.appendChild(dialog);
  dialog.querySelectorAll("[data-close-model-dialog]").forEach((item) => {
    item.addEventListener("click", closeModelDialog);
  });
  $("modelForm").addEventListener("submit", saveModelFromDialog);
  $("modelKeyMode").addEventListener("change", updateModelKeyModeUi);
}

function openModelDialog(id) {
  const item = id ? state.models.find((row) => row.id === id) : null;
  $("modelDialogTitle").textContent = item ? "编辑模型" : "新增模型";
  $("modelId").value = item ? item.id : "";
  $("modelDisplayName").value = item ? item.displayName || "" : "锦锐内置算力";
  $("modelProviderId").value = item ? item.providerId || "" : "jinrui-deepseek";
  $("modelApiType").value = item ? item.apiType || "openai-completions" : "openai-completions";
  $("modelBaseUrl").value = item ? item.baseUrl || "" : "http://192.168.0.212:13000/v1";
  $("modelKeyMode").value = item ? item.keyMode || "client" : "client";
  $("modelApiKey").value = "";
  $("modelDefaultModel").value = item ? item.defaultModel || "" : "deepseek-v4-flash";
  $("modelChannel").value = item ? item.channel || "stable" : "stable";
  $("modelGroup").value = item ? item.modelGroup || "default" : "default";
  $("modelEnabled").checked = item ? Boolean(item.enabled) : true;
  $("modelForceDefault").checked = item ? Boolean(item.forceDefault) : false;
  $("modelModelsJson").value = item ? formatModelsJson(item.modelsJson) : '[{"id":"deepseek-v4-flash","name":"deepseek-v4-flash","reasoning":false,"enabled":true}]';
  updateModelKeyModeUi();
  $("modelDialog").classList.remove("hidden");
  $("modelDisplayName").focus();
}

function closeModelDialog() {
  $("modelDialog").classList.add("hidden");
}

async function saveModelFromDialog(event) {
  event.preventDefault();
  let modelsJson = $("modelModelsJson").value.trim();
  try {
    modelsJson = JSON.stringify(JSON.parse(modelsJson));
  } catch {
    alert("模型列表JSON格式不正确，请检查逗号、引号和括号。");
    return;
  }

  const id = $("modelId").value;
  const providerId = $("modelProviderId").value.trim();
  const duplicate = state.models.find((item) => item.providerId === providerId && String(item.id) !== String(id || ""));
  if (duplicate) {
    alert(`Provider ID“${providerId}”已经存在，请编辑原模型或换一个Provider ID。`);
    return;
  }

  const body = {
    providerId,
    displayName: $("modelDisplayName").value.trim(),
    apiType: $("modelApiType").value,
    baseUrl: $("modelBaseUrl").value.trim(),
    keyMode: $("modelKeyMode").value,
    apiKey: $("modelApiKey").value.trim(),
    modelsJson,
    defaultModel: $("modelDefaultModel").value.trim(),
    enabled: $("modelEnabled").checked,
    channel: $("modelChannel").value.trim() || "stable",
    modelGroup: $("modelGroup").value.trim() || "default",
    forceDefault: $("modelForceDefault").checked
  };
  if (body.keyMode === "client") delete body.apiKey;
  if (id && !body.apiKey) delete body.apiKey;

  await api(id ? `/api/admin/models/${id}` : "/api/admin/models", {
    method: id ? "PATCH" : "POST",
    body: JSON.stringify(body)
  });
  closeModelDialog();
  await loadModels();
}

async function deleteModel(id) {
  const item = state.models.find((row) => row.id === id);
  const name = item ? item.displayName || item.providerId : `ID ${id}`;
  if (!confirm(`确定删除模型“${name}”吗？删除后客户端将不会再获取这个模型配置。`)) return;
  await api(`/api/admin/models/${id}`, { method: "DELETE" });
  await loadModels();
}

function updateModelKeyModeUi() {
  const serverKey = $("modelKeyMode").value === "server";
  $("modelApiKeyWrap").classList.toggle("hidden", !serverKey);
  $("modelApiKey").required = serverKey && !$("modelId").value;
}

function renderTable(id, headers, rows) {
  const safeRows = Array.isArray(rows) ? rows : [];
  const body = safeRows.length
    ? safeRows.map((row) => `<tr>${(Array.isArray(row) ? row : []).map((cell) => `<td>${cell ?? ""}</td>`).join("")}</tr>`).join("")
    : `<tr><td class="muted" colspan="${headers.length}">暂无数据</td></tr>`;
  $(id).innerHTML = `<thead><tr>${headers.map((header) => `<th>${header}</th>`).join("")}</tr></thead><tbody>${body}</tbody>`;
}

function label(key) {
  return {
    licenses: "授权总数",
    activeLicenses: "有效授权",
    devices: "设备数",
    versions: "版本数",
    models: "模型数",
    events: "事件数",
    reports: "反馈数",
    reports24h: "24小时反馈",
    reportsPending: "待处理反馈",
    errors24h: "24小时错误",
    gatewayStartsToday: "今日网关启动",
    gatewaySuccessToday: "今日网关成功",
    gatewayFailedToday: "今日网关失败",
    gatewaySuccessRateToday: "今日成功率"
  }[key] || key;
}

function keyModeLabel(value) {
  return value === "server"
    ? "<span class='tag server'>后台内置</span>"
    : "<span class='tag client'>客户端填写</span>";
}

function formatModelsJson(value) {
  try {
    return JSON.stringify(JSON.parse(value || "[]"), null, 2);
  } catch {
    return value || "[]";
  }
}

function code(value) {
  return value ? `<code>${escapeHtml(value)}</code>` : "";
}

function gatewaySummary(item) {
  const total = Number(item.gatewayStartCount || 0);
  const success = Number(item.gatewaySuccessCount || 0);
  const failed = Number(item.gatewayFailedCount || 0);
  if (!total) return "<span class='gateway-muted'>未上报</span>";
  const statusClass = failed > 0 ? "failed" : "success";
  const statusText = failed > 0 ? "有失败" : "正常";
  return `<div class="gateway-cell">
    <span class="gateway-status ${statusClass}">${statusText}</span>
    <span class="gateway-counts">总 ${total} / 成 ${success} / 败 ${failed}</span>
  </div>`;
}

function gatewayRecent(item) {
  if (!item.lastGatewayAt) return "<span class='muted'>-</span>";
  const failed = item.lastGatewayEventType === "gateway_start_failed";
  const label = failed ? "失败" : "成功";
  const message = item.lastGatewayMessage ? `<br><span class="muted">${escapeHtml(item.lastGatewayMessage)}</span>` : "";
  return `<span class="${failed ? "bad" : "ok"}">${label}</span><br>${fmt(item.lastGatewayAt)}${message}`;
}

function normalizePageData(data) {
  if (Array.isArray(data)) {
    return {
      records: data,
      current: 1,
      pages: 1,
      total: data.length
    };
  }
  const records = Array.isArray(data && data.records)
    ? data.records
    : Array.isArray(data && data.rows)
      ? data.rows
      : Array.isArray(data && data.list)
        ? data.list
        : [];
  return {
    records,
    current: Number(data && data.current) || Number(data && data.page) || 1,
    pages: Number(data && data.pages) || Number(data && data.totalPages) || 1,
    total: Number(data && data.total) || records.length
  };
}

function renderPagination(containerId, page, onPage) {
  const el = document.getElementById(containerId);
  if (!el) return;
  if (!page || page.pages <= 1) {
    el.innerHTML = "";
    return;
  }
  const current = page.current || 1;
  const total = page.pages || 1;
  const totalRecords = page.total || 0;
  let html = `<div class="pager-summary">共 <strong>${totalRecords}</strong> 条 <span>第 ${current}/${total} 页</span></div>`;
  html += `<div class="pager-actions">`;
  html += `<button class="pager-btn" type="button" data-page="1"${current <= 1 ? " disabled" : ""}>首页</button>`;
  html += `<button class="pager-btn" type="button" data-page="${current - 1}"${current <= 1 ? " disabled" : ""}>上一页</button>`;
  html += `<label class="pager-goto">跳至 <input type="number" id="${containerId}Input" value="${current}" min="1" max="${total}"> 页 <button class="pager-btn primary" type="button" data-go>GO</button></label>`;
  html += `<button class="pager-btn" type="button" data-page="${current + 1}"${current >= total ? " disabled" : ""}>下一页</button>`;
  html += `<button class="pager-btn" type="button" data-page="${total}"${current >= total ? " disabled" : ""}>末页</button>`;
  html += `</div>`;
  el.innerHTML = html;

  el.querySelectorAll("[data-page]").forEach((button) => {
    button.onclick = () => onPage(Number(button.dataset.page));
  });
  el.querySelector("[data-go]").onclick = () => {
    const v = parseInt(document.getElementById(containerId + "Input").value, 10);
    if (v >= 1 && v <= total) onPage(v);
  };
}

function status(value) {
  return `<span class="${value === "active" ? "ok" : "bad"}">${escapeHtml(value || "")}</span>`;
}

function fmt(value) {
  return value ? String(value).replace("T", " ").slice(0, 19) : "";
}

function toDatetimeLocal(value) {
  return value ? String(value).slice(0, 16) : "";
}

function fromDatetimeLocal(value) {
  return value ? `${value}:00` : null;
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  }[char]));
}

window.openLicenseDialog = openLicenseDialog;
window.clearLicenseUsb = clearLicenseUsb;
window.deleteLicense = deleteLicense;
window.openVersionDialog = openVersionDialog;
window.toggleVersionPublished = toggleVersionPublished;
window.deleteVersion = deleteVersion;
window.openModelDialog = openModelDialog;
window.deleteModel = deleteModel;
window.openReportDialog = openReportDialog;
window.downloadReportPackage = downloadReportPackage;
