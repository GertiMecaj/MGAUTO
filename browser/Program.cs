using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Net;

record LaunchRequest(string Mode, string Profile, string Url, string Cookie, string Script, string OAuth);
static class Program {
    [STAThread]
    static void Main(string[] args) {
        if (args.Contains("--self-test")) {
            var sample = JsonSerializer.Deserialize<LaunchRequest>("{\"Mode\":\"login\",\"Profile\":\"test\",\"Url\":\"https://magicgarden.gg\",\"Cookie\":\"\",\"Script\":\"\",\"OAuth\":\"\"}");
            if (sample?.Mode != "login") Environment.Exit(2);
            return;
        }
        ApplicationConfiguration.Initialize();
        try {
            var request = JsonSerializer.Deserialize<LaunchRequest>(Console.In.ReadLine() ?? "") ?? throw new Exception("Missing browser request");
            Application.Run(new BrowserWindow(request));
        } catch (Exception e) { Console.Out.WriteLine(JsonSerializer.Serialize(new { error = e.Message })); Environment.ExitCode = 1; }
    }
}
class BrowserWindow : Form {
    readonly LaunchRequest request;
    readonly WebView2 web = new() { Dock = DockStyle.Fill };
    readonly System.Windows.Forms.Timer timer = new() { Interval = 750 };
    bool checking, done;
    readonly HttpClient http = new(new HttpClientHandler { AllowAutoRedirect = true, UseCookies = false }) { Timeout = TimeSpan.FromSeconds(40) };
    public BrowserWindow(LaunchRequest request) {
        this.request = request;
        Text = request.Mode == "login" ? "MGAUTO — Discord login" : "MGAUTO — Play Magic Garden";
        Width = 1280; Height = 900; StartPosition = FormStartPosition.CenterScreen;
        Controls.Add(web); Shown += async (_,_) => await InitializeBrowser();
        FormClosed += (_,_) => { timer.Stop(); timer.Dispose(); http.Dispose(); web.Dispose(); };
    }
    async Task InitializeBrowser() {
        try {
            string profile = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "MGAUTO", "Browser", request.Profile);
            Directory.CreateDirectory(profile);
            try { CoreWebView2Environment.GetAvailableBrowserVersionString(); }
            catch (WebView2RuntimeNotFoundException) {
                if (request.Mode == "smoke") throw;
                var install = MessageBox.Show(this, "MGAUTO needs Microsoft Edge WebView2 to log in and play. Install it now?", "Install browser component", MessageBoxButtons.YesNo, MessageBoxIcon.Question);
                if (install != DialogResult.Yes) throw new Exception("WebView2 installation was cancelled.");
                var setup = Path.Combine(AppContext.BaseDirectory, "MicrosoftEdgeWebview2Setup.exe");
                if (!File.Exists(setup)) throw new Exception("WebView2 installer is missing. Reinstall MGAUTO.");
                Text = "MGAUTO — Installing browser component…";
                using var installer = System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(setup, "/silent /install") { UseShellExecute = true })!;
                await installer.WaitForExitAsync();
                bool ready = false;
                for (int attempt = 0; attempt < 60; attempt++) {
                    try { CoreWebView2Environment.GetAvailableBrowserVersionString(); ready = true; break; }
                    catch (WebView2RuntimeNotFoundException) { await Task.Delay(1000); }
                }
                if (!ready) throw new Exception("WebView2 setup did not complete. Restart MGAUTO after Windows finishes installing it.");
            }
            var env = await CoreWebView2Environment.CreateAsync(null, profile);
            await web.EnsureCoreWebView2Async(env);
            var core = web.CoreWebView2;
            core.Settings.AreDevToolsEnabled = false;
            core.NewWindowRequested += (_,e) => { e.Handled = true; core.Navigate(e.Uri); };
            if (request.Mode == "smoke") {
                SetCookie("MGAUTO_test", "cookie-roundtrip", "magicgarden.gg");
                var cookies = await core.CookieManager.GetCookiesAsync("https://magicgarden.gg");
                if (!cookies.Any(c => c.Name == "MGAUTO_test" && c.Value == "cookie-roundtrip")) throw new Exception("Cookie roundtrip failed");
                core.CookieManager.DeleteCookies("MGAUTO_test", "https://magicgarden.gg");
                core.NavigationCompleted += async (_,_) => {
                    var result = await core.ExecuteScriptAsync("JSON.stringify({title:document.title,webgl:!!document.createElement('canvas').getContext('webgl')})");
                    using var screenshot = File.Create("smoke-browser.png");
                    await core.CapturePreviewAsync(CoreWebView2CapturePreviewImageFormat.Png, screenshot);
                    File.WriteAllText("smoke-browser-result.txt", result);
                    Console.Out.WriteLine("{\"smoke\":true}"); Console.Out.Flush(); Close();
                };
                core.NavigateToString("<html><head><title>MGAUTO browser ready</title></head><body style='background:#0b0f14;color:#e8ecf0;font:24px Segoe UI;padding:60px'><h1>MGAUTO browser ready</h1><p>WebView2 initialized. Cookie storage and JavaScript enabled.</p></body></html>");
            } else if (request.Mode == "login") {
                await core.Profile.ClearBrowsingDataAsync(CoreWebView2BrowsingDataKinds.AllProfile);
                SetCookie("mc_oauth_room_id", "MgAFK", ".magicgarden.gg");
                SetCookie("mc_oauth_redirect_uri", "https://magicgarden.gg/oauth2/redirect", ".magicgarden.gg");
                timer.Tick += async (_,_) => await CheckLogin(); timer.Start();
                core.Navigate(request.OAuth);
            } else {
                var target = new Uri(request.Url);
                if (target.Scheme != "https") throw new Exception("The game URL must use HTTPS.");
                foreach (var pair in request.Cookie.Split(';')) {
                    int pos = pair.IndexOf('=');
                    if (pos > 0) SetCookie(pair[..pos].Trim(), pair[(pos+1)..].Trim(), target.Host);
                }
                if (!string.IsNullOrWhiteSpace(request.Script)) {
                    string poly = File.ReadAllText(Path.Combine(AppContext.BaseDirectory, "gm-polyfill.js"));
                    string version = Regex.Match(request.Script, @"(?m)^\s*//\s*@version\s+(\S+)").Groups[1].Value;
                    var info = JsonSerializer.Serialize(new { scriptHandler = "MGAUTO", version, script = new { name = "Gemini", version, @namespace = "mgafk" } });
                    // Origin check prevents exposing the native request bridge to Discord or external pages.
                    string injection = "if (location.origin === " + JsonSerializer.Serialize(target.GetLeftPart(UriPartial.Authority)) + ") { window.GM_info = " + info + ";" + poly + "\nwindow.addEventListener('load', function() {\n" + request.Script + "\n}, {once:true});\n}";
                    await core.AddScriptToExecuteOnDocumentCreatedAsync(injection);
                    core.WebMessageReceived += async (_,e) => await HandleRequest(e, target.Host);
                }
                core.Navigate(request.Url);
            }
        } catch (Exception e) {
            Console.Out.WriteLine(JsonSerializer.Serialize(new { error = e.Message })); Console.Out.Flush();
            MessageBox.Show(this, "Could not open the browser. Install Microsoft Edge WebView2 Runtime if it is missing.\n\n" + e.Message, "MGAUTO", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Environment.ExitCode = 1; Close();
        }
    }
    void SetCookie(string name, string value, string domain) {
        var cookie = web.CoreWebView2.CookieManager.CreateCookie(name, value, domain, "/");
        cookie.IsSecure = true; web.CoreWebView2.CookieManager.AddOrUpdateCookie(cookie);
    }
    async Task CheckLogin() {
        if (checking || done) return;
        checking = true;
        try {
            var cookies = await web.CoreWebView2.CookieManager.GetCookiesAsync("https://magicgarden.gg");
            var token = cookies.FirstOrDefault(c => c.Name == "mc_jwt" && !string.IsNullOrWhiteSpace(c.Value));
            if (token != null) {
                done = true; timer.Stop();
                Console.Out.WriteLine(JsonSerializer.Serialize(new { token = token.Value })); Console.Out.Flush(); Close();
            }
        } catch (Exception e) { if (!IsDisposed) Text = "MGAUTO — Waiting for login: " + e.Message; }
        finally { checking = false; }
    }
    async Task HandleRequest(CoreWebView2WebMessageReceivedEventArgs e, string gameHost) {
        if (!Uri.TryCreate(e.Source, UriKind.Absolute, out var source) || source.Scheme != "https" || source.Host != gameHost) return;
        string id = "";
        try {
            using var document = JsonDocument.Parse(e.WebMessageAsJson);
            var data = document.RootElement;
            id = data.GetProperty("id").GetString() ?? "";
            var url = new Uri(data.GetProperty("url").GetString()!);
            if (url.Scheme != "https" || url.IsLoopback || IPAddress.TryParse(url.Host, out _)) throw new Exception("Only public HTTPS requests are supported.");
            using var message = new HttpRequestMessage(new HttpMethod(data.GetProperty("method").GetString() ?? "GET"), url);
            if (data.TryGetProperty("body", out var body) && body.ValueKind == JsonValueKind.String) message.Content = new StringContent(body.GetString()!);
            if (data.TryGetProperty("headers", out var headers)) foreach (var h in headers.EnumerateObject()) {
                if (!message.Headers.TryAddWithoutValidation(h.Name, h.Value.GetString())) message.Content?.Headers.TryAddWithoutValidation(h.Name, h.Value.GetString());
            }
            using var response = await http.SendAsync(message);
            var bytes = await response.Content.ReadAsByteArrayAsync();
            var result = new { status = (int)response.StatusCode, statusText = response.ReasonPhrase, body = System.Text.Encoding.UTF8.GetString(bytes), bodyB64 = Convert.ToBase64String(bytes), headers = response.Headers.ToString() + response.Content.Headers.ToString(), finalUrl = response.RequestMessage?.RequestUri?.ToString() };
            await Callback(id, result);
        } catch (Exception ex) { if (!web.IsDisposed) await Callback(id, new { error = ex.Message }); }
    }
    async Task Callback(string id, object result) {
        await web.CoreWebView2.ExecuteScriptAsync("window.__MgAfkBridge && window.__MgAfkBridge.callback(" + JsonSerializer.Serialize(id) + "," + JsonSerializer.Serialize(result) + ");");
    }
}
