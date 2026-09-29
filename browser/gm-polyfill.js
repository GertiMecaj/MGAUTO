
        (function() {
          if (typeof window.GM_setValue === 'function') return;
          var prefix = 'GM_';

          // ── Persistent storage → localStorage ───────────────────────────
          window.GM_setValue = function(k, v) {
            try { localStorage.setItem(prefix + k, JSON.stringify(v)); }
            catch (e) { console.warn('GM_setValue failed', e); }
          };
          window.GM_getValue = function(k, def) {
            try {
              var raw = localStorage.getItem(prefix + k);
              if (raw === null || raw === undefined) return def;
              return JSON.parse(raw);
            } catch (e) { return def; }
          };
          window.GM_deleteValue = function(k) {
            try { localStorage.removeItem(prefix + k); } catch (e) {}
          };
          window.GM_listValues = function() {
            var out = [];
            for (var i = 0; i < localStorage.length; i++) {
              var k = localStorage.key(i);
              if (k && k.indexOf(prefix) === 0) out.push(k.slice(prefix.length));
            }
            return out;
          };
          window.GM_addStyle = function(css) {
            var s = document.createElement('style');
            s.textContent = css;
            (document.head || document.documentElement).appendChild(s);
            return s;
          };

          // GM_info is injected separately right before the userscript runs,
          // with the actual version parsed from the script's metadata header.

          // ── GM_xmlhttpRequest → native bridge (bypasses CORS) ──────────
          // Tampermonkey's GM_xmlhttpRequest skips the browser's same-origin
          // policy; we route through Java/OkHttp to get the same effect in a
          // raw WebView.
          window.__MgAfkBridge = {
            pending: {},
            nextId: 0,
            // Convert a base64 string to an ArrayBuffer / Blob.
            _b64ToBytes: function(b64) {
              var binary = atob(b64);
              var len = binary.length;
              var bytes = new Uint8Array(len);
              for (var i = 0; i < len; i++) bytes[i] = binary.charCodeAt(i);
              return bytes;
            },
            callback: function(id, res) {
              var cb = this.pending[id];
              delete this.pending[id];
              if (!cb) return;
              if (res && res.error) {
                if (cb.onerror) cb.onerror({ error: res.error, status: 0 });
                return;
              }
              // Decode body. Native sends `body` as text (utf-8) and may also
              // send `bodyB64` for binary responses (blob / arraybuffer).
              var responseText = res.body || '';
              var response = responseText;
              try {
                if (cb.responseType === 'json') {
                  response = responseText ? JSON.parse(responseText) : null;
                } else if (cb.responseType === 'blob') {
                  var bytes = res.bodyB64 ? this._b64ToBytes(res.bodyB64)
                    : new TextEncoder().encode(responseText);
                  response = new Blob([bytes]);
                } else if (cb.responseType === 'arraybuffer') {
                  var bytesAB = res.bodyB64 ? this._b64ToBytes(res.bodyB64)
                    : new TextEncoder().encode(responseText);
                  response = bytesAB.buffer;
                }
              } catch (e) {
                if (cb.onerror) cb.onerror({ error: 'parse error: ' + e.message, status: res.status });
                return;
              }
              var headersText = (res && res.headers) || '';
              if (cb.onload) cb.onload({
                status: res.status,
                statusText: res.statusText || '',
                responseText: responseText,
                response: response,
                readyState: 4,
                finalUrl: res.finalUrl || cb.url,
                responseHeaders: headersText,
              });
            },
          };
          window.GM_xmlhttpRequest = function(opts) {
            opts = opts || {};
            var id = String(++window.__MgAfkBridge.nextId);
            window.__MgAfkBridge.pending[id] = {
              onload: opts.onload,
              onerror: opts.onerror,
              url: opts.url,
              responseType: opts.responseType || 'text',
            };
            try {
              var headersJson = JSON.stringify(opts.headers || {});
              var body = (opts.data == null) ? null : String(opts.data);
              window.chrome.webview.postMessage({method: opts.method || 'GET', url: opts.url, headers: opts.headers || {}, body: body, id: id, responseType: opts.responseType || 'text'});
            } catch (e) {
              delete window.__MgAfkBridge.pending[id];
              if (opts.onerror) opts.onerror({ error: e.message, status: 0 });
            }
            return { abort: function() { delete window.__MgAfkBridge.pending[id]; } };
          };
        })();
    