## GitHub Copilot Chat

- Extension: 0.37.8 (prod)
- VS Code: 1.109.5 (072586267e68ece9a47aa43f8c108e0dcbf44622)
- OS: win32 10.0.26200 x64
- GitHub Account: daviddrc16

## Network

User Settings:
```json
  "http.systemCertificatesNode": true,
  "github.copilot.advanced.debug.useElectronFetcher": true,
  "github.copilot.advanced.debug.useNodeFetcher": false,
  "github.copilot.advanced.debug.useNodeFetchFetcher": true
```

Connecting to https://api.github.com:
- DNS ipv4 Lookup: 140.82.113.6 (28 ms)
- DNS ipv6 Lookup: Error (25 ms): getaddrinfo ENOTFOUND api.github.com
- Proxy URL: None (1 ms)
- Electron fetch (configured): HTTP 200 (124 ms)
- Node.js https: HTTP 200 (408 ms)
- Node.js fetch: HTTP 200 (121 ms)

Connecting to https://api.githubcopilot.com/_ping:
- DNS ipv4 Lookup: 140.82.112.22 (25 ms)
- DNS ipv6 Lookup: Error (26 ms): getaddrinfo ENOTFOUND api.githubcopilot.com
- Proxy URL: None (36 ms)
- Electron fetch (configured): HTTP 200 (412 ms)
- Node.js https: HTTP 200 (420 ms)
- Node.js fetch: HTTP 200 (423 ms)

Connecting to https://copilot-proxy.githubusercontent.com/_ping:
- DNS ipv4 Lookup: 4.249.131.160 (25 ms)
- DNS ipv6 Lookup: Error (25 ms): getaddrinfo ENOTFOUND copilot-proxy.githubusercontent.com
- Proxy URL: None (19 ms)
- Electron fetch (configured): HTTP 200 (595 ms)
- Node.js https: HTTP 200 (576 ms)
- Node.js fetch: HTTP 200 (563 ms)

Connecting to https://mobile.events.data.microsoft.com: HTTP 404 (189 ms)
Connecting to https://dc.services.visualstudio.com: HTTP 404 (807 ms)
Connecting to https://copilot-telemetry.githubusercontent.com/_ping: HTTP 200 (411 ms)
Connecting to https://copilot-telemetry.githubusercontent.com/_ping: HTTP 200 (372 ms)
Connecting to https://default.exp-tas.com: HTTP 400 (496 ms)

Number of system certificates: 73

## Documentation

In corporate networks: [Troubleshooting firewall settings for GitHub Copilot](https://docs.github.com/en/copilot/troubleshooting-github-copilot/troubleshooting-firewall-settings-for-github-copilot).