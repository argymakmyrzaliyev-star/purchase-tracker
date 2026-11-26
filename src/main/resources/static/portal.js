"use strict";
const element = id => document.getElementById(id);
const purchases = "/api/v1/purchase-requests";
let token = null;
let expiryTimer;

function clearSession() {
  clearTimeout(expiryTimer);
  token = null;
  element("identity").hidden = true;
  element("login").hidden = false;
  element("session").textContent = "Signed out";
}

async function call(path, method = "GET", data) {
  const headers = { "X-Correlation-Id": crypto.randomUUID() };
  if (token) headers.Authorization = "Bearer " + token;
  if (data) headers["Content-Type"] = "application/json";
  const response = await fetch(path, { method, headers, body: data ? JSON.stringify(data) : undefined, cache: "no-store" });
  const body = await response.json();
  if (response.status === 401) clearSession();
  return { status: response.status, ok: response.ok, body };
}

function display(reply, message = "") {
  const {status, body} = reply;
  const lines = ["HTTP " + status];
  if (body.code) lines.push(body.code + " · " + body.message, "Correlation ID: " + body.correlationId);
  if (body.businessKey) lines.push(body.businessKey + " · " + body.title, "Status: " + body.status);
  if (message) lines.push(message);
  element("result").textContent = lines.join("\n");
}

// Serialize actions so that a late response cannot restore a signed-out session.
let busy = false;
function bind(id, eventName, action) {
  element(id).addEventListener(eventName, async event => {
    event.preventDefault();
    if (busy) return;
    busy = true;
    try { await action(); }
    catch { element("result").textContent = "Could not read the API response. Check that the server is running."; }
    finally { busy = false; }
  });
}

bind("login", "submit", async () => {
  clearSession();
  const form = element("login");
  const credentials = Object.fromEntries(new FormData(form));
  form.elements.password.value = "";
  const login = await call("/api/v1/auth/login", "POST", credentials);
  if (!login.ok) return display(login);
  token = login.body.accessToken;
  let me;
  try { me = await call("/api/me"); }
  catch (error) { clearSession(); throw error; }
  if (!me.ok) { clearSession(); return display(me); }
  element("login").hidden = true;
  element("identity").hidden = false;
  element("who").textContent = me.body.username;
  element("role").textContent = me.body.authorities.join(", ");
  element("session").textContent = "Signed in · " + me.body.username;
  element("expiry").textContent = "Session length: " + login.body.expiresIn / 60 + " minutes";
  expiryTimer = setTimeout(clearSession, login.body.expiresIn * 1000);
  display(me, "Signed in as " + me.body.username);
});

bind("create", "submit", async () => {
  const reply = await call(purchases, "POST", Object.fromEntries(new FormData(element("create"))));
  if (reply.ok) element("request-id").value = reply.body.id;
  display(reply);
});

function requestPath() {
  const id = element("request-id").value.trim();
  if (!id) { element("result").textContent = "Create a draft or enter a request UUID."; return null; }
  return purchases + "/" + encodeURIComponent(id);
}

bind("read", "click", async () => {
  const path = requestPath();
  if (path) display(await call(path));
});

bind("approve", "click", async () => {
  const path = requestPath();
  if (!path) return;
  const before = await call(path);
  if (!before.ok) return display(before);
  const reply = await call(path + "/approve", "POST");
  if (reply.status === 403) {
    const after = await call(path);
    const comparison = after.ok ? "Status before / after: " + before.body.status + " / " + after.body.status
                                : "Could not re-read the status.";
    return display(reply, comparison);
  }
  display(reply);
});

bind("logout", "click", async () => {
  clearSession();
  display(await call("/api/me"), "Signed out.");
});
