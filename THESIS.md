# ANDROID LOCAL FILE SERVER
### A Thesis Submitted in Partial Fulfillment of the Requirements for the Degree of Bachelor of Computer Science

**By**  
[Your Name]

---

## TABLE OF CONTENTS

1. [Introduction](#chapter-1-introduction)  
   1.1 Background  
   1.2 Problem Statement  
   1.3 Scope and Objectives  
   1.4 Structure of Thesis  
2. [Literature Review / Related Work](#chapter-2-literature-review--related-work)  
   2.1 Overview  
   2.2 Existing File-Sharing Solutions  
   2.3 Technologies Used  
3. [Methodology](#chapter-3-methodology)  
   3.1 User Requirement Analysis  
   3.2 Use Case Diagram and Use Case Tables  
   3.3 System Design  
4. [Implementation and Results](#chapter-4-implementation-and-results)  
   4.1 Implementation  
   4.2 Results  
5. [Discussion and Evaluation](#chapter-5-discussion-and-evaluation)  
   5.1 Discussion  
   5.2 Comparison  
   5.3 Evaluation  
6. [Conclusion and Future Work](#chapter-6-conclusion-and-future-work)  
   6.1 Conclusion  
   6.2 Future Work  
7. [References](#references)

---

## CHAPTER 1
## INTRODUCTION

### 1.1 Background

The widespread adoption of smartphones has transformed the way people manage and share data. Android devices, now a primary computing platform for billions of users, carry cameras, documents, videos, and a wide range of personal files. Despite this, moving files from an Android device to a laptop, a tablet, or a friend's phone remains surprisingly cumbersome. Users are routinely forced to rely on cloud services that require internet connectivity, proprietary desktop applications that must be installed on every machine, or physical cables that may not always be available.

Local area networks (LANs), by contrast, are present almost everywhere: in homes, offices, schools, and cafés. A Wi-Fi router provides a shared, high-speed network that can move gigabytes of data in seconds, yet most stock Android experiences leave this bandwidth largely untapped for peer-to-peer file transfers.

Embedded HTTP servers represent a compelling solution to this gap. By running a lightweight web server directly on the Android device, the device can expose its file system to every other machine on the same network through a standard web browser—no additional software, no internet connection, and no proprietary drivers required. Any device with a browser, whether a Windows PC, a macOS laptop, an iPhone, or another Android phone, can immediately upload to or download from the host device.

This project, the **Android Local File Server**, implements exactly this concept. An embedded NanoHTTPD server runs inside an Android application and serves a fully functional file-management web interface backed by pure HTML, CSS, and JavaScript assets bundled within the app. The user controls the server through a native WebView that renders the same interface locally, while remote users connect through their own browsers.

### 1.2 Problem Statement

Despite the maturity of local networking technology, existing approaches to on-device file sharing on Android suffer from several recurring problems:

1. **Dependency on External Infrastructure**  
   Popular solutions such as Google Drive, Dropbox, and WeTransfer route all traffic through remote servers. This imposes a minimum internet bandwidth requirement, introduces latency, and creates a single point of failure. When the internet is unavailable or slow, these tools become entirely non-functional even though the devices may be physically within metres of each other.

2. **Proprietary Application Requirements**  
   Tools such as Snapdrop, AirDroid, and SHAREit require dedicated applications installed on both the sending and receiving device. This creates friction, particularly in scenarios where the receiving device belongs to a guest or an organization that restricts software installation.

3. **Lack of Access Control**  
   Many lightweight Wi-Fi sharing utilities broadcast files to the entire network without any form of authentication. In a shared environment such as an office or a university dormitory, this exposes potentially sensitive files to every device on the subnet.

4. **Complex Setup**  
   Some solutions rely on platform-specific protocols (AirDrop on Apple devices, Nearby Share on Android) that are fundamentally incompatible across operating systems, forcing users to remember multiple workflows depending on which devices are involved.

5. **No Browser-Native Interface**  
   Tools that do provide an HTTP-based interface frequently deliver a minimal, text-only listing of files. They lack features users expect from a modern web application—progress indicators, search, sorting, file-type filtering, and responsive design.

This project addresses all five problems by delivering an Android application that runs a full-featured, browser-accessible HTTP server with tiered access control, a polished responsive web interface, and QR-code-based discovery—all operating entirely within the local network.

### 1.3 Scope and Objectives

**Scope**

The Android Local File Server is designed for on-device deployment on any Android device running API level 21 (Android 5.0) or higher. It targets the following core use cases:

1. **Serving as a File Host**: Allowing the Android device owner to expose a managed upload directory to other devices on the same Wi-Fi network through any standards-compliant browser.
2. **Joining Another Server**: Allowing the user to navigate their own WebView to an existing server instance running on a different device by entering its IP address or scanning a QR code.
3. **Access Control**: Allowing the host to choose between a public server accessible by anyone on the network and a private server protected by a password.
4. **File Operations**: Allowing connected clients to upload, download, search, sort, and filter files; restricting destructive operations (delete) to the localhost (device owner) only.

The project deliberately excludes real-time collaborative editing, media streaming, or any functionality that requires internet connectivity.

**Objectives**

1. **Zero-Installation Access**: Any device with a web browser on the same network can upload to or download from the Android host without installing additional software.
2. **Tiered Security**: The host can configure the server as public (open) or private (password-protected). Private mode additionally supports QR-code-based token authentication to allow trusted guests to connect without typing a password.
3. **Localhost-Only Destructive Privileges**: File deletion is restricted to the device owner through origin-based access control, preventing remote clients from modifying the file listing.
4. **Polished Web Interface**: The bundled HTML/CSS/JS frontend provides search, sort-by-name, sort-by-size, file-type identification, upload progress feedback, and a responsive layout suitable for both desktop browsers and mobile browsers.
5. **Automatic Port Selection**: The server scans for the next available TCP port starting at 8080, ensuring resilience against port conflicts.

### 1.4 Structure of Thesis

- **Chapter 1 – Introduction**: Motivates the project by describing the gap between the file-sharing capabilities users need and those provided by existing solutions.
- **Chapter 2 – Literature Review**: Reviews existing file-sharing applications and surveys the core technologies—NanoHTTPD, Android WebView, NanoHTTPD's HTTP session model, QR code generation with ZXing, and token-based authentication—used to implement the project.
- **Chapter 3 – Methodology**: Presents the system architecture through use-case diagrams, the HTTP request-handling flow, the authentication state machine, and the database (file system) design.
- **Chapter 4 – Implementation and Results**: Details the server-side Java implementation (`EmbeddedServer.java`, `MainActivity.java`, `NetworkUtils.java`), the client-side web assets, and presents screenshots of every major screen.
- **Chapter 5 – Discussion and Evaluation**: Reflects on design choices, addresses development challenges, compares the solution to alternatives, and evaluates how well each objective was achieved.
- **Chapter 6 – Conclusion and Future Work**: Summarises the project and proposes a roadmap for further development.

---

## CHAPTER 2
## LITERATURE REVIEW / RELATED WORK

### 2.1 Overview

Local network file sharing is not a new problem, and a range of solutions—ranging from decades-old protocols such as FTP and SMB to modern progressive web applications—have attempted to solve it. What distinguishes the Android Local File Server from these approaches is the combination of zero-installation access, a self-contained Android application, and tiered access control implemented entirely through standard HTTP semantics.

This chapter first surveys the most relevant existing tools and evaluates their limitations. It then reviews each core technology component used in the project and explains why it was chosen over alternatives.

### 2.2 Existing File-Sharing Solutions

#### 2.2.1 AirDroid

AirDroid is a commercial Android application that exposes the device over both the local network and the internet through AirDroid's own relay servers. It provides a rich web-based interface and supports file transfer, SMS management, and screen mirroring.

- **Pros**: Feature-rich; works across platforms; offers both local and remote access.
- **Cons**: Requires an AirDroid account; routes local traffic through external servers by default; freemium model limits transfer sizes; privacy implications of a third-party relay.

#### 2.2.2 Snapdrop / Pairdrop

Snapdrop is an open-source web application inspired by Apple's AirDrop. It uses WebRTC and a signalling server to establish peer-to-peer connections within the browser. Pairdrop is a fork with room-based discovery.

- **Pros**: Entirely browser-based; no installation needed on either device; open source.
- **Cons**: Relies on a public signalling server (or self-hosted infrastructure); works well only when both devices have a modern browser capable of WebRTC; file size is limited by browser memory constraints; no persistent storage on the sender's device.

#### 2.2.3 Python `http.server` / SimpleHTTPServer

Developers frequently run `python3 -m http.server` on a laptop to expose a directory over HTTP. This is the conceptual ancestor of this project but it operates in the opposite direction (from desktop to Android, not from Android to desktop) and provides no upload capability, no access control, and no user interface beyond a raw directory listing.

#### 2.2.4 SHAREit / Xender

SHAREit and Xender are dedicated peer-to-peer transfer applications that create Wi-Fi hotspots or use Wi-Fi Direct to move files at high speed.

- **Pros**: Fast transfers; no internet required; supports large files.
- **Cons**: Both devices must install the application; well-documented privacy and adware concerns in SHAREit; no browser accessibility; no fine-grained access control.

### 2.3 Technologies Used

#### 2.3.1 NanoHTTPD

NanoHTTPD is a lightweight, open-source HTTP server library for Java and Android. A single class file implements a fully compliant HTTP/1.1 server capable of handling simultaneous connections through a thread-per-request model. It supports multipart form uploads, cookie management, query-parameter parsing, and custom MIME type mappings.

NanoHTTPD was chosen over alternatives such as Jetty Embedded and Ktor for three reasons: its extremely small footprint (critical in an Android APK), its mature multipart-upload implementation (required for the file-upload endpoint), and its long track record in Android projects.

#### 2.3.2 Android WebView

Android's `WebView` component embeds a full Chromium-based browser engine inside a native Activity. The application renders its own home screen (`home.html`) inside a WebView, allowing the native Java code and the JavaScript running in the page to communicate bidirectionally through the `addJavascriptInterface` API. This approach means the entire user interface—both the local control panel and the server interface visited by remote clients—is a single, unified web application, reducing design duplication and making the UI trivially portable to any browser.

#### 2.3.3 ZXing (QR Code Generation)

The ZXing ("Zebra Crossing") library is the de-facto standard for barcode and QR code encoding and decoding on Android. The project uses ZXing's `MultiFormatWriter` to encode the server URL (optionally including a one-time QR token) into a 500 × 500 pixel `Bitmap`, which is then compressed to a PNG, Base64-encoded, and injected into the WebView as a data URI for display. The `journeyapps/zxing-android-embedded` wrapper provides a ready-to-use Activity for scanning, invoked through the `ScanContract` `ActivityResultLauncher` API.

#### 2.3.4 Token-Based HTTP Authentication

Rather than implementing HTTP Basic Authentication (which browsers handle awkwardly on modern versions), the server implements a custom token scheme modelled on stateful session cookies:

1. In **private mode**, unauthenticated requests are redirected to a login page (`login.html`).
2. The login page submits a `POST /auth` request with the password as a URL-encoded body parameter.
3. The server compares the submitted password to the configured `serverPassword` and, on success, generates a `UUID` token, stores it in an in-memory `Set<String>`, and returns it as an `HttpOnly` cookie (`auth_token`).
4. Subsequent requests are authenticated by checking whether the `auth_token` cookie value exists in the token set.
5. **QR tokens** provide a parallel fast-path: a one-time `UUID` embedded in the QR code URL is exchanged for a full session token on first visit, enabling trusted guests to authenticate without typing a password.

#### 2.3.5 Path Traversal Prevention

All file-serving and file-deletion handlers resolve the requested filename to a canonical absolute path and verify that it begins with the canonical absolute path of the `uploads/` directory. Any request that attempts to escape this directory (e.g., `../../etc/passwd`) is rejected with `HTTP 403 Forbidden` before any I/O is performed.

#### 2.3.6 HTML / CSS / JavaScript Frontend

The web interface is implemented as static assets bundled in the `app/src/main/assets/` directory and served directly from the `AssetManager`. This allows the frontend to be loaded with zero network round-trips when accessed locally, and it avoids any dependency on a JavaScript framework or build toolchain. The UI is styled with custom CSS (dark theme, responsive grid layout) and uses the browser Fetch API for all asynchronous communication with the server.

---

## CHAPTER 3
## METHODOLOGY

### 3.1 User Requirement Analysis

The development of the Android Local File Server began with identifying what users actually need when sharing files across devices on a local network. The target users are Android device owners — students, office workers, and anyone in a home or small-team setting — who want to move files to or from nearby devices without depending on the internet, installing extra software on the receiving device, or connecting a cable. Requirements were derived by examining the five pain points described in Chapter 1 and by enumerating the concrete actions each type of user must be able to perform through the application.

#### 3.1.1 Functional Requirements

The system must allow the Android device owner (the **Host**) to start a local HTTP server that exposes an `uploads/` directory to other devices on the same Wi-Fi network. Any device with a standard web browser must be able to connect to that server without installing any additional application. The server must bind to an automatically selected available TCP port starting from 8080, so that port conflicts do not prevent the application from starting.

The system must support the following file operations accessible to connected clients: uploading one or more files to the server through a multipart HTTP form, downloading any stored file with a browser-native download prompt, and browsing the full list of uploaded files. To improve day-to-day usability the file listing must support real-time keyword search, sorting by filename and by file size, and display of each file's type and size — all performed client-side without additional server round-trips.

The system must implement two distinct access-control modes. In **public mode** the server is open to any device on the network with no credentials required. In **private mode** all requests from remote devices must be authenticated before any file operation is permitted; authentication is performed either by submitting a password through a login form or by scanning a one-time QR code that the host generates from the app. The QR code embeds a single-use token in the server URL so that trusted guests can connect instantly without typing a password.

File deletion must be restricted exclusively to the Host device. Because the Host's requests originate from `127.0.0.1`, the server can enforce this at the network layer inside `EmbeddedServer.handleDelete()` regardless of what the client-side interface shows. Remote clients must never be able to delete files even if they bypass the UI. Additionally, the Host must be able to use the app's own WebView to join and browse a server running on a different device by entering that device's IP address or scanning its QR code.

#### 3.1.2 Non-Functional Requirements

**Performance.** File transfers occur entirely over the local Wi-Fi network using standard HTTP/1.1 over TCP/IP, so throughput is bounded only by the LAN speed rather than internet bandwidth. The embedded NanoHTTPD server uses a thread-per-request model that is sufficient for the low concurrency of a home or small-office environment. The application must remain responsive on the host device while the server is handling concurrent client requests.

**Usability.** The web interface served by the application must be usable from any modern browser on any platform — Windows, macOS, Linux, iOS, and Android — without requiring configuration or browser extensions. The interface must be fully responsive so that it is comfortable on both a 5-inch phone screen and a 15-inch laptop display. Upload progress must be visible in real time so that users know when a transfer is complete.

**Security.** The system must prevent path traversal attacks: every filename that arrives in a download or delete request must be resolved to a canonical absolute path and verified to lie within the `uploads/` directory before any file I/O is performed. In private mode, all endpoints except `/login` and `POST /auth` must reject unauthenticated requests with a redirect rather than serving content. Session tokens must be stored as `HttpOnly` cookies to reduce exposure to client-side script injection. One-time QR tokens must be invalidated immediately after first use and must be cleared whenever the server configuration changes.

**Portability and self-containment.** The application must run on any Android device with API level 21 (Android 5.0) or higher. All HTML, CSS, and JavaScript assets that make up the web interface must be bundled inside the APK so that the server requires no internet connection, no CDN, and no external runtime dependency beyond the Android OS itself.

**Robustness.** The server must tolerate port conflicts by scanning up to twenty consecutive ports and selecting the first available one transparently. File uploads that span filesystem mount-point boundaries — where a `rename()` call would fail — must fall back to a stream copy so that the upload completes successfully on all devices.

### 3.2 Use Case Diagram and Use Case Tables

#### 3.2.1 Use Case Diagram

```
                        ┌──────────────────────────────────────────────────┐
                        │           Android Local File Server              │
                        │                                                  │
          ┌─────┐       │  ┌─────────────────┐   ┌──────────────────────┐ │
          │Host │──────►│  │  Start Server   │   │  View / Search Files │ │
          └──┬──┘       │  │  (Public/Private│   │  Sort / Filter       │ │
             │          │  └─────────────────┘   └──────────────────────┘ │
             │          │                                                  │
             │          │  ┌─────────────────┐   ┌──────────────────────┐ │
             │          │  │  Generate QR    │   │  Delete File         │ │
             │          │  │  Code           │   │  (Host only)         │ │
             │          │  └─────────────────┘   └──────────────────────┘ │
             │          │                                                  │
          ┌──┴────┐     │  ┌─────────────────┐   ┌──────────────────────┐ │
          │Client │────►│  │  Login (Private │   │  Upload File         │ │
          └───────┘     │  │  Server)        │   │                      │ │
                        │  └─────────────────┘   └──────────────────────┘ │
                        │                                                  │
                        │  ┌─────────────────┐                            │
                        │  │  Download File  │                            │
                        │  └─────────────────┘                            │
                        └──────────────────────────────────────────────────┘
```

**Figure 1: Use Case Diagram – Android Local File Server**

The use case diagram above captures the complete set of interactions between the two actors and the system boundary of the Android Local File Server.

**Actors**

The system defines two distinct actors:

- **Host** — the owner of the Android device on which the application is installed. The Host has elevated privileges: they can start and configure the server, generate QR codes for guest access, and perform destructive file operations (delete). Because the Host's requests originate from `127.0.0.1` (localhost), the server can distinguish them from all remote requests at the network layer.
- **Client** — any external device connected to the same Wi-Fi network that accesses the server through a standard web browser. The Client actor inherits all non-destructive capabilities (login, upload, download, search/sort/filter) but is blocked from server configuration and file deletion. In the diagram the Client is shown as a generalisation of the Host for the shared use cases, reflecting that the Host can also act as a client of their own server.

**Use Cases and Actor Associations**

| Use Case | Host | Client | Notes |
|---|:---:|:---:|---|
| Start Server (Public / Private) | ✅ | — | Exclusive to the Host; initiated from `home.html` via the `WebAppInterface` bridge. |
| Generate QR Code | ✅ | — | Host requests a one-time QR token from `requestQRCode()`; the encoded image is displayed in the WebView. |
| Delete File | ✅ | — | Server enforces localhost-only access at the `POST /delete` handler regardless of what the UI shows. |
| Login (Private Server) | — | ✅ | Triggered automatically when an unauthenticated Client reaches a private server; also reachable via QR token fast-path. |
| Upload File | ✅ | ✅ | Both actors can upload; the Host uses the native Android file picker, Clients use their browser's file picker. |
| Download File | ✅ | ✅ | Standard `GET /download/<filename>` with `Content-Disposition: attachment`; available to all authenticated users. |
| View / Search / Sort / Filter Files | ✅ | ✅ | Performed entirely client-side in JavaScript on the `/files` page; no extra server round-trip required. |

**Relationships**

All UML use case relationships in the diagram are catalogued below, grouped by relationship type.

---

**1. Actor–Use Case Associations**

Associations are the primary links between an actor and a use case they can initiate or participate in.

| Association | Actor | Use Case | Multiplicity / Condition |
|---|---|---|---|
| A1 | Host | Start Server (Public / Private) | Host only; requires app to be foregrounded |
| A2 | Host | Generate QR Code | Host only; requires server to be running |
| A3 | Host | Delete File | Host only; enforced at server layer (`origin == 127.0.0.1`) |
| A4 | Host | Upload File | Shared with Client; Host uses native Android file picker |
| A5 | Host | Download File | Shared with Client; Host's WebView uses `DownloadManager` |
| A6 | Host | View / Search / Sort / Filter Files | Shared with Client |
| A7 | Host | Join Server | Host only; navigates own WebView to a remote server URL |
| A8 | Client | Login (Private Server) | Client only; not needed in public mode |
| A9 | Client | Upload File | Shared with Host; Client uses browser native file picker |
| A10 | Client | Download File | Shared with Host; browser handles `Content-Disposition: attachment` |
| A11 | Client | View / Search / Sort / Filter Files | Shared with Host; entirely client-side JavaScript |

---

**2. Generalisation (Actor Inheritance)**

The **Host** is a specialisation of the **Client** actor. Every use case available to the Client is also available to the Host (Upload, Download, View/Search/Sort/Filter, Login in private mode), because the Host's WebView accesses the same HTTP endpoints as any remote browser. The Host additionally has exclusive use cases (Start Server, Generate QR Code, Delete File, Join Server) that are inaccessible to the Client. This generalisation is depicted in the diagram by the Host actor's association arrows encompassing all Client use cases plus the Host-exclusive ones.

---

**3. «include» Relationships**

An «include» relationship means the included use case is *always* executed as a mandatory part of the base use case.

| ID | Base Use Case | Included Use Case | Rationale |
|---|---|---|---|
| I1 | Start Server (Public / Private) | **«include»** Generate QR Code | Immediately after the server binds to a port, `index.html` calls `Android.requestQRCode()` on load. Every server start unconditionally produces a QR code. |
| I2 | Upload File | **«include»** View / Search / Sort / Filter Files | After a successful upload the client is implicitly expected to verify the file appears in the listing; the upload response directs the user to `/files`. |
| I3 | Delete File | **«include»** View / Search / Sort / Filter Files | The Delete action is only reachable from the `/files` page; the listing must be loaded before deletion can be triggered. |
| I4 | Download File | **«include»** View / Search / Sort / Filter Files | Similarly, the Download link is only rendered inside the file listing table on `/files`. |

---

**4. «extend» Relationships**

An «extend» relationship means the extending use case is executed *conditionally*, only when a specific extension point is reached.

| ID | Base Use Case | Extending Use Case | Extension Point / Condition |
|---|---|---|---|
| E1 | Upload File | **«extend»** Login (Private Server) | Extension point: *before request is processed*. Condition: server is in PRIVATE mode AND the incoming request carries no valid `auth_token` cookie. The server intercepts the request and redirects to `/login` before any upload logic runs. |
| E2 | Download File | **«extend»** Login (Private Server) | Same condition as E1. Any `GET /download/<filename>` from an unauthenticated client on a private server is intercepted and redirected to `/login`. |
| E3 | View / Search / Sort / Filter Files | **«extend»** Login (Private Server) | Same condition as E1. `GET /files` from an unauthenticated client on a private server is redirected to `/login`. |
| E4 | Login (Private Server) | **«extend»** QR Token Fast-Path Authentication | Extension point: *on request arrival at `/login` redirect*. Condition: the original URL contains a valid `qr_token` query parameter. Instead of presenting the password form, the server directly exchanges the QR token for a session cookie and serves the home page, bypassing the password entry step entirely. |
| E5 | Start Server (Public / Private) | **«extend»** Start Private Server (password sub-flow) | Extension point: *on mode selection in modal*. Condition: user selects **Private**. The password input field is revealed and `startPrivateServer()` is called instead of `startPublicServer()`. In public mode this extension point is never reached. |
| E6 | Join Server | **«extend»** QR Code Scan (join via scan) | Extension point: *on IP address input*. Condition: user taps **Scan QR** instead of typing an IP. The ZXing scanner Activity is launched; the scanned URL is used in place of a manually entered address. |

---

**5. Guard Conditions on Associations**

Guard conditions are boolean constraints that must be satisfied for the association to be valid. They differ from «extend» in that they do not represent a separate use case being invoked; they simply restrict when an association is active.

| ID | Association | Guard Condition | Enforcement Point |
|---|---|---|---|
| G1 | Host → Delete File | `isLocalhost(session) == true` | `EmbeddedServer.handleDelete()` checks the remote IP before any file I/O; remote requests return HTTP 403 regardless of UI state. |
| G2 | Client → Upload File | `isAuthenticated(session) == true OR serverMode == PUBLIC` | `EmbeddedServer.serve()` auth gate runs before routing; unauthenticated requests on private servers are redirected to login. |
| G3 | Client → Download File | Same as G2 | Same auth gate. |
| G4 | Client → View / Search / Sort / Filter Files | Same as G2 | Same auth gate. |
| G5 | Host → Generate QR Code | `server.isAlive() == true` | `requestQRCode()` calls `server.generateQrToken()`; if the server is null or stopped, no token is generated. |

---

**6. Summary Relationship Table**

| ID | Type | From | To | Note |
|---|---|---|---|---|
| A1–A11 | Association | Host / Client | Use cases | See Actor–Use Case Associations above |
| GEN1 | Generalisation | Host | Client | Host specialises Client; inherits all Client use cases |
| I1 | «include» | Start Server | Generate QR Code | Always executed on server start |
| I2 | «include» | Upload File | View / Search Files | Post-upload listing verification |
| I3 | «include» | Delete File | View / Search Files | Delete only reachable from file listing |
| I4 | «include» | Download File | View / Search Files | Download only reachable from file listing |
| E1 | «extend» | Login | Upload File | Private mode, unauthenticated |
| E2 | «extend» | Login | Download File | Private mode, unauthenticated |
| E3 | «extend» | Login | View / Search Files | Private mode, unauthenticated |
| E4 | «extend» | QR Token Fast-Path | Login | Valid `qr_token` in URL |
| E5 | «extend» | Start Private Server | Start Server | Private mode selected |
| E6 | «extend» | QR Code Scan | Join Server | Scan chosen over manual IP entry |
| G1 | Guard | Host → Delete File | `isLocalhost == true` | Server-layer enforcement |
| G2–G4 | Guard | Client → Upload/Download/View | `isAuthenticated OR public` | Server-layer auth gate |
| G5 | Guard | Host → Generate QR Code | `server.isAlive == true` | Token generation guard |

#### 3.2.2 Use Case Tables

**Use Case 1 – Start Server (Public)**

| Field | Detail |
|---|---|
| Name | Start Public Server |
| Actor | Host |
| Description | The Host starts the HTTP server in public mode, making it accessible to all devices on the same network without authentication. |
| Precondition | The application is open. No server is currently running. |
| Steps | 1. Host taps **Start Server**. 2. Host selects **Public** in the modal. 3. App finds an available TCP port (starting from 8080). 4. NanoHTTPD server starts on `0.0.0.0:<port>`. 5. WebView loads `http://localhost:<port>/`. 6. Host sees the Server Home page with a QR code. |
| Result | Server is reachable at `http://<device-ip>:<port>/` from any browser on the network. |
| Extensions | None. |
| Exceptions | Port range exhausted → Toast error "Failed to start server". |

**Use Case 2 – Start Server (Private)**

| Field | Detail |
|---|---|
| Name | Start Private Server |
| Actor | Host |
| Description | The Host starts the HTTP server in private mode with a password, restricting access to authenticated clients only. |
| Precondition | Application is open. No server is currently running. |
| Steps | 1. Host taps **Start Server**. 2. Host enters a password and taps **Private**. 3. Server starts; mode is set to `PRIVATE` with the supplied password. 4. All non-localhost requests that lack a valid `auth_token` cookie are redirected to `/login`. |
| Result | Only clients who supply the correct password (or scan the QR code) can access the server. |
| Extensions | None. |
| Exceptions | Empty password field → alert "Please enter a password". |

**Use Case 3 – Client Login (Password)**

| Field | Detail |
|---|---|
| Name | Login with Password |
| Actor | Client |
| Description | A remote client authenticates to a private server by submitting the password. |
| Precondition | Server is running in private mode. Client's browser has navigated to the server URL. |
| Steps | 1. Client is redirected to `/login`. 2. Client enters the password and submits. 3. `POST /auth` is issued with `password=<value>`. 4. Server validates the password. 5. On success, server sets `auth_token` cookie and returns HTTP 200. 6. Browser redirects to `/`. |
| Result | Client gains session-scoped access to the server. |
| Extensions | Incorrect password → error message displayed; input cleared. |
| Exceptions | Server is stopped while login is in progress → WebView shows a connection refused error. |

**Use Case 4 – QR Code Connection**

| Field | Detail |
|---|---|
| Name | Connect via QR Code |
| Actor | Client |
| Description | A remote client scans the QR code displayed on the host's screen to connect instantly without entering a password. |
| Precondition | Server is running (public or private). Host's Server Home page is visible. |
| Steps | 1. Host's Server Home page calls `Android.requestQRCode()`. 2. App generates a one-time QR token, embeds it in the server URL, encodes as QR PNG, and injects it into the WebView. 3. Client scans the QR code. 4. Browser navigates to `http://<ip>:<port>/?qr_token=<token>`. 5. Server exchanges the QR token for a full session token, sets the cookie, and serves the home page. |
| Result | Client is authenticated and lands on the Server Home page. |
| Extensions | None. |
| Exceptions | QR token not found (already used or server restarted) → client receives normal login page. |

**Use Case 5 – Upload File**

| Field | Detail |
|---|---|
| Name | Upload File |
| Actor | Client or Host |
| Description | Upload one or more files to the server's managed `uploads/` directory. |
| Precondition | Client is authenticated (or server is public). |
| Steps | 1. Client navigates to `/upload`. 2. Client selects one or more files via the file chooser. 3. Browser issues a `POST /upload` multipart request. 4. Server parses the body, saves the temp file to `uploads/<filename>`. 5. Server returns `{"message":"File uploaded successfully"}`. |
| Result | File is persistently stored and appears in the file listing. |
| Extensions | Duplicate filename → server overwrites the existing file with the same name. |
| Exceptions | No file in request body → `{"error":"No file uploaded"}`. |

**Use Case 6 – Download File**

| Field | Detail |
|---|---|
| Name | Download File |
| Actor | Client or Host |
| Description | Download a file from the server to the client device. |
| Precondition | Client is authenticated (or server is public). File exists in `uploads/`. |
| Steps | 1. Client navigates to `/files`. 2. Client clicks **Download** for the desired file. 3. Browser issues `GET /download/<filename>`. 4. Server resolves canonical path, verifies it is within `uploads/`, streams the file with `Content-Disposition: attachment`. |
| Result | File is saved to the client device's default Downloads folder. |
| Extensions | None. |
| Exceptions | File not found → HTTP 404. Path traversal attempt → HTTP 403. |

**Use Case 7 – Delete File**

| Field | Detail |
|---|---|
| Name | Delete File |
| Actor | Host only |
| Description | Permanently remove a file from the server's `uploads/` directory. |
| Precondition | Request originates from `127.0.0.1` (localhost). File exists. |
| Steps | 1. Host (via WebView) clicks **Delete** on a file row. 2. Browser issues `POST /delete?file=<filename>`. 3. Server checks `isLocalhost(session)`. 4. Server resolves canonical path and deletes the file. 5. Server returns `{"message":"File deleted successfully"}`. |
| Result | File is removed from `uploads/` and disappears from the listing on next refresh. |
| Extensions | Remote client attempts delete → HTTP 403 Forbidden. |
| Exceptions | File system error prevents deletion → server returns HTTP 500; file remains in `uploads/`. |

**Use Case 8 – Search / Sort / Filter Files**

| Field | Detail |
|---|---|
| Name | Search and Sort Files |
| Actor | Client or Host |
| Description | Filter the file listing by name keyword, sort by name or size, all client-side. |
| Precondition | Client has navigated to `/files` and the file listing has loaded. |
| Steps | 1. Client types in the search box → `filterFiles()` filters `filesData` array in memory. 2. Client selects a sort option → `sortFiles()` re-sorts and re-renders the table. |
| Result | The file table updates instantly without a server round-trip. |
| Extensions | None. |
| Exceptions | None (all filtering and sorting logic runs client-side; no server communication occurs). |

**Use Case 9 – Join Existing Server**

| Field | Detail |
|---|---|
| Name | Join Server |
| Actor | Host (using their own WebView) |
| Description | The Host uses the app's WebView to browse to a server running on another device. |
| Precondition | Another device on the same network is running the server. |
| Steps | 1. Host taps **Join Server** on the home screen. 2. Host enters the IP address or scans a QR code. 3. `Android.joinServer(ip)` is called. 4. WebView navigates to `http://<ip>:<port>/`. |
| Result | WebView displays the remote server's web interface. |
| Extensions | Host taps **Scan QR** instead of typing an IP → ZXing scanner Activity launches; the scanned URL fills the address field automatically on a successful scan. |
| Exceptions | Entered IP is unreachable or the remote server is not running → WebView displays a connection error page. |

### 3.3 System Design

This section presents the overall design of the Android Local File Server, covering its layered architecture, HTTP request-handling flow, authentication state machine, and file storage strategy. The system operates entirely within a Local Area Network and follows a client–server model in which the Android device acts as the server host while any other device on the same network interacts with it through a standard web browser. All components — the NanoHTTPD server, the WebView-based UI, and the bundled HTML/CSS/JS assets — are self-contained within a single APK, requiring no external infrastructure at runtime.

#### 3.3.1 System Architecture

The system follows a three-layer architecture:

```
┌───────────────────────────────────────────────────────────────┐
│                    Android Application                        │
│                                                               │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  Presentation Layer (WebView + JavaScript)              │  │
│  │  home.html / index.html / upload.html / files.html      │  │
│  │  login.html / server.html / style.css / script.js       │  │
│  └──────────────────────┬──────────────────────────────────┘  │
│                         │ addJavascriptInterface (bridge)      │
│  ┌──────────────────────▼──────────────────────────────────┐  │
│  │  Application Layer (Java)                               │  │
│  │  MainActivity.java  –  lifecycle, WebView, QR scanner   │  │
│  │  NetworkUtils.java  –  LAN IP discovery                 │  │
│  └──────────────────────┬──────────────────────────────────┘  │
│                         │ extends NanoHTTPD                    │
│  ┌──────────────────────▼──────────────────────────────────┐  │
│  │  Server Layer (NanoHTTPD)                               │  │
│  │  EmbeddedServer.java                                    │  │
│  │  – HTTP routing   – Auth (password / QR token)          │  │
│  │  – Upload handler – Download handler – Delete handler   │  │
│  │  – File listing   – Asset serving                       │  │
│  └──────────────────────┬──────────────────────────────────┘  │
│                         │                                      │
│  ┌──────────────────────▼──────────────────────────────────┐  │
│  │  Storage Layer                                          │  │
│  │  Context.getFilesDir()/uploads/   (private app storage) │  │
│  │  AssetManager  (bundled HTML/CSS/JS)                    │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────┘
         ▲ HTTP over Wi-Fi LAN
         │
┌────────┴───────────────────────────────────┐
│  Remote Clients (any browser on the LAN)   │
└────────────────────────────────────────────┘
```

#### 3.3.2 HTTP Request Routing

Every incoming HTTP request is handled by `EmbeddedServer.serve()`. The routing logic follows this decision tree:

```
Request arrives
       │
       ▼
  Is server in PRIVATE mode AND request is NOT from localhost?
       │Yes                            │No
       ▼                               ▼
  Is QR token valid?            Route normally (see below)
       │Yes         │No
       ▼             ▼
  Exchange for     Is it POST /auth?
  session token         │Yes          │No
  → serve index    Validate password  Is it GET /login?
                        │               │Yes    │No
                   Valid?            Serve      Is auth_token cookie valid?
                   │Yes  │No        login.html      │Yes         │No
                   Set   Return                  Route         Serve
                   cookie 401                   normally      login.html
                   → 200

Normal Routing:
  GET  /              → serve index.html
  GET  /upload        → serve upload.html
  POST /upload        → handleUpload()
  GET  /files         → serve files.html
  GET  /server-info   → getServerInfo() JSON
  GET  /list          → listFiles() JSON
  GET  /is-owner      → {"isOwner": true|false}
  GET  *.css | *.js   → serveAsset()
  GET  /download/<f>  → serveFile() with path-traversal check
  POST /delete        → handleDelete() — localhost only
```

#### 3.3.3 Authentication State Machine

```
[Unauthenticated] ──POST /auth (correct password)──► [Authenticated]
                                                           │
[Unauthenticated] ──QR token in URL────────────────► [Authenticated]
                                                           │
[Authenticated]   ──Session ends / server restarts──► [Unauthenticated]
```

Token sets (`validTokens`, `validQrTokens`) are cleared whenever `setServerConfig()` is called (i.e., whenever the server mode or password changes), effectively invalidating all existing sessions.

#### 3.3.4 File Storage Design

Files are stored in the application's private internal storage: `Context.getFilesDir()/uploads/`. This directory is:

- **Private to the app**: inaccessible to other apps without root.
- **Persistent across server restarts**: files survive as long as the app is installed and `clearUploads()` is not called.
- **Cleared on app destruction**: `onDestroy()` calls `server.clearUploads()` so that files do not accumulate indefinitely across app sessions.

The `uploads/` directory acts as a flat file store. No database or manifest file is maintained; the canonical listing is produced by calling `File.listFiles()` at request time.

---

## CHAPTER 4
## IMPLEMENTATION AND RESULTS

### 4.1 Implementation

#### 4.1.1 Server Implementation (`EmbeddedServer.java`)

`EmbeddedServer` extends `NanoHTTPD` and overrides the `serve(IHTTPSession session)` method as the single entry point for all HTTP traffic.

**Key implementation details:**

**Port binding.** The server binds to `"0.0.0.0"` on the supplied port, making it accessible on all network interfaces simultaneously (Wi-Fi, USB tethering, Ethernet adapter). This is unlike binding to a specific interface IP, which would break if the IP changes.

**Multipart upload.** NanoHTTPD's `parseBody()` writes uploaded file content to a temp file and places the temp-file path in a `Map<String, String>` keyed by the field name. The handler retrieves this path, then either renames or copies the temp file to the `uploads/` directory.

**Path traversal prevention.** Every filename that enters `serveFile()` or `handleDelete()` is resolved to a canonical absolute path with `File.getCanonicalPath()`. The handler then asserts that this canonical path starts with the canonical path of the `uploads/` directory. Any deviation returns HTTP 403 without performing any I/O.

**Asset serving.** HTML, CSS, and JS files are loaded from the APK's asset bundle via `Context.getAssets().open(assetName)` and streamed as chunked HTTP responses. MIME types are resolved by a custom `getCustomMimeType()` method.

**CORS / mixed-content.** Because the WebView loads the home screen from `file:///android_asset/home.html` and then navigates to `http://localhost:<port>/`, Android's `MIXED_CONTENT_ALWAYS_ALLOW` setting is necessary to permit the WebView's JavaScript to call the local HTTP server.

#### 4.1.2 Main Activity (`MainActivity.java`)

`MainActivity` manages the Android lifecycle and acts as the bridge between the WebView JavaScript and the native Java server.

**`WebAppInterface`** is an inner class annotated with `@JavascriptInterface`. It exposes four methods to JavaScript:

| Method | Purpose |
|---|---|
| `startServer(mode, password)` | Creates and starts an `EmbeddedServer`, then loads `http://localhost:<port>/` in the WebView. |
| `joinServer(ip)` | Navigates the WebView to `http://<ip>:<port>/`. |
| `scanQRCode()` | Launches the ZXing scanner Activity. |
| `requestQRCode()` | Generates a one-time QR token, builds the server URL, encodes a QR PNG, Base64-encodes it, and injects it into the page via `evaluateJavascript("displayQRCode(...)`)`. |

**File chooser integration.** `WebChromeClient.onShowFileChooser()` is overridden to intercept the browser's `<input type="file">` dialog and launch a native Android `ACTION_GET_CONTENT` Intent, supporting multiple file selection. The result URIs are delivered back to the WebView through the stored `ValueCallback<Uri[]>`.

**Download listener.** `WebView.setDownloadListener()` intercepts download links and enqueues them with `DownloadManager`, triggering the standard Android download notification and saving files to the public `Downloads/` directory.

**Port selection.** `findAvailablePort(8080)` iterates from port 8080 to 8099, attempting to open a `ServerSocket` on each. The first port that succeeds is used. This prevents crashes when port 8080 is already occupied by another application.

#### 4.1.3 Client-Side Web Interface

The frontend is a set of seven static files served from the APK asset bundle:

| File | Purpose |
|---|---|
| `home.html` | Landing page: "Start Server" / "Join Server" buttons. |
| `index.html` | Server Home: "Upload File" / "View Files" links and QR code display. |
| `upload.html` | File upload form with drag-and-drop and progress bar. |
| `files.html` | File listing table with search, sort, and per-row actions. |
| `login.html` | Password form for private-server authentication. |
| `server.html` | IP address entry form and QR scanner button for joining a server. |
| `style.css` | Dark-themed responsive stylesheet. |
| `script.js` | All client-side logic: server control, file listing, upload progress, search, sort, login. |

**Ownership-aware UI.** On page load, `script.js` calls `GET /is-owner`. The server returns `{"isOwner": true}` only for requests originating from `127.0.0.1`. The JavaScript uses this flag to show or hide the **Delete** button in the file listing, ensuring that the destructive action is invisible—not merely disabled—for remote clients.

**Upload progress.** File uploads use the `XMLHttpRequest` API rather than `fetch` because `XHR` exposes `upload.onprogress` events. The progress handler calculates the percentage and updates a `<progress>` element in real time.

### 4.2 Results

#### 4.2.1 Home Screen

The home screen (`home.html`) presents two large buttons: **Start Server** and **Join Server**. Tapping **Start Server** opens a modal that allows the host to choose between public and private mode. If private mode is selected, a password input field is revealed. This design ensures that access-control decisions are made explicitly before the server accepts any connections.

#### 4.2.2 Server Home Page

Once the server is started, the WebView loads `http://localhost:<port>/`. This page (rendered from `index.html`) offers two navigation buttons—**Upload File** and **View Files**—and displays the QR code for the server URL. In private mode, the QR code URL contains a one-time token, so scanning it authenticates the client automatically.

#### 4.2.3 Upload Page

The upload page provides a standard `<input type="file" multiple>` element. On a remote browser, this uses the browser's native file picker. On the host's own WebView, the `WebChromeClient` override launches the Android file picker, supporting multi-file selection from any app (Files, Gallery, etc.). A progress bar animates during the upload, and a success or error message is displayed upon completion.

#### 4.2.4 Files Page

The files page (`files.html`) displays all uploaded files in a table with columns for Name, Type, Size, and Actions. Features include:

- **Search**: A text input filters the displayed rows client-side as the user types, with no server round-trip.
- **Sort**: A dropdown allows sorting by Name (alphabetical) or Size (ascending).
- **Download**: A per-row download link triggers the browser's standard download flow.
- **Delete**: Visible only to the host (via the `/is-owner` ownership check), the delete button issues `POST /delete?file=<name>` and refreshes the listing on success.

#### 4.2.5 Login Page

When a remote client connects to a private server, they are presented with `login.html`—a minimal, centred form with a password field and a **Login** button. Submitting an incorrect password displays an inline error message and clears the field. Submitting the correct password sets the session cookie and redirects the client to the server home page, after which they have full access to upload, download, and view files for the duration of their browser session.

#### 4.2.6 Join Server Page

`server.html` provides an IP address text field and a **Scan QR** button. Entering an IP address and tapping **Join** invokes `Android.joinServer(ip)`, which navigates the host's WebView to the remote server. Tapping **Scan QR** launches the ZXing scanner; a successful scan extracts the URL from the QR code and navigates the WebView to it directly.

---

## CHAPTER 5
## DISCUSSION AND EVALUATION

### 5.1 Discussion

#### 5.1.1 Key Features and Functionalities

The Android Local File Server successfully delivers a unified solution to local network file sharing with the following standout features:

- **Zero-Installation Browser Access**: Any device with a browser—regardless of operating system or model—can connect to the server and transfer files without installing any software. This is the project's most important differentiator.
- **Tiered Access Control**: The two-mode system (public / private) covers the two most common real-world scenarios: a trusted home or office network where open access is acceptable, and a semi-public environment (a school lab, a conference room) where password protection is needed.
- **QR-Code-Based Instant Authentication**: The one-time-token QR code eliminates the usability friction of password entry for trusted guests while maintaining security—the token is invalidated the moment the server mode changes.
- **Owner-Only Destructive Privileges**: Restricting file deletion to the localhost origin at the server layer (not merely the UI layer) means that even if a malicious client bypasses the UI, the `POST /delete` endpoint will return HTTP 403. This is a defence-in-depth measure.
- **Path Traversal Protection**: Canonical path resolution in both `serveFile()` and `handleDelete()` prevents any file outside the `uploads/` sandbox from being read or deleted, regardless of how the filename is encoded in the request.
- **Self-Contained APK**: All web assets are bundled inside the APK. The server requires no internet connectivity, no CDN, and no external dependency at runtime beyond the Android OS itself.

#### 5.1.2 Reflection on Implementation Details

The `WebAppInterface` bridge required careful attention. Android mandates that any method called from JavaScript must be annotated with `@JavascriptInterface`, and all UI operations (WebView navigation, Toast messages) must be dispatched to the main thread via `runOnUiThread()`. Neglecting either of these requirements results in silent failures or crashes.

The multipart upload handler presented a subtle challenge: NanoHTTPD stores the uploaded content in a system temp file, but the `rename` system call may fail across filesystem mount points (a known Android issue). The implementation falls back to a stream copy if rename fails, ensuring reliability on all devices.

Cookie-based token validation required extracting the `cookie` header and parsing semicolon-delimited key=value pairs manually, because NanoHTTPD does not provide a high-level cookie API.

#### 5.1.3 Addressing Development Challenges

**Mixed-content restriction.** When the WebView loads `file:///android_asset/home.html` and JavaScript on that page calls `Android.startServer()`, the subsequent `loadUrl("http://localhost:...")` transition works cleanly. However, having the WebView load a `file://` page that also makes `fetch()` calls to `http://` endpoints is blocked by the browser's mixed-content policy. This was resolved by always navigating the WebView to the `http://localhost` origin once the server starts, so all subsequent network calls originate from the same HTTP origin.

**Port conflicts.** On some Android versions, system services occupy common ports. The `findAvailablePort()` loop addresses this by trying up to 20 consecutive ports, making the app robust against collisions.

**`onActivityResult` and `ActivityResultLauncher` co-existence.** The app uses the legacy `onActivityResult` for the file chooser (required by `WebChromeClient`) and the modern `ActivityResultLauncher` / `ScanContract` for the QR scanner. Both approaches must be registered before the Activity starts, and care must be taken that the `filePathCallback` reference is not overwritten or leaked between launches.

#### 5.1.4 Design Choices and Impact on User Experience

Using a WebView as the host's own UI, rather than building a traditional native Android UI, was a deliberate choice. It means the host and the remote clients see nearly identical interfaces, the UI only needs to be designed once, and any UI improvements automatically benefit both local and remote users. The cost is a slightly heavier initial load compared to a native layout, but this is negligible given the local nature of the app.

The dark-themed CSS was chosen for comfort during extended use and is consistent with the modern Android aesthetic. The responsive grid layout ensures usability on both the 5-inch screen of the host device and the 15-inch screen of a connecting laptop.

#### 5.1.5 Significance of Chosen Technologies

NanoHTTPD's simplicity proved to be both a strength and a limitation. Its thread-per-request model is adequate for the low concurrency expected on a home or office LAN, but it would not scale to hundreds of simultaneous clients. For the intended use case, this is acceptable. The decision to avoid a heavier embedded server (Jetty, Tomcat) keeps the APK small and the startup time fast.

### 5.2 Comparison

| Feature | Android Local File Server | AirDroid | Snapdrop | SHAREit |
|---|---|---|---|---|
| No installation on receiver | ✅ | ❌ | ✅ | ❌ |
| No internet required | ✅ | Partial | ✅ | ✅ |
| Password access control | ✅ | ✅ | ❌ | ❌ |
| QR code connect | ✅ | ✅ | ❌ | ❌ |
| Owner-only delete | ✅ | N/A | N/A | N/A |
| Path traversal protection | ✅ | N/A | N/A | N/A |
| Open source / self-contained | ✅ | ❌ | ✅ | ❌ |
| Multiple simultaneous clients | ✅ | ✅ | ✅ | ❌ |
| Upload progress indicator | ✅ | ✅ | ✅ | ❌ |
| Search / sort files | ✅ | ✅ | ❌ | ❌ |

### 5.3 Evaluation

**Objective 1 – Zero-Installation Access**: Fully achieved. A remote client only needs to open a browser and enter the server URL or scan the QR code.

**Objective 2 – Tiered Security**: Fully achieved. Public mode, password-protected private mode, and QR-token fast authentication are all implemented and tested.

**Objective 3 – Localhost-Only Destructive Privileges**: Fully achieved. The `isLocalhost()` check is enforced at the server layer, and the UI hides the Delete button for non-owner clients via the `/is-owner` endpoint.

**Objective 4 – Polished Web Interface**: Largely achieved. Search, sort by name/size, file-type identification, upload progress, and responsive design are all present. File-type filtering (beyond the sort-by-type option in the dropdown) and a preview pane for images are identified as future improvements.

**Objective 5 – Automatic Port Selection**: Fully achieved. The `findAvailablePort()` method scans 20 consecutive ports and selects the first available one transparently.

**Development Challenges Overcome**: Mixed-content browser restrictions, NanoHTTPD multipart rename fallback, cookie header manual parsing, and the co-existence of legacy and modern Activity result APIs were all successfully resolved.

---

## CHAPTER 6
## CONCLUSION AND FUTURE WORK

### 6.1 Conclusion

The Android Local File Server demonstrates that an Android device can serve as a practical, secure, and user-friendly file-sharing hub for an entire local network, requiring no software installation on connecting devices. The project's core contributions are:

- **An embedded NanoHTTPD server** running inside an Android application, capable of handling simultaneous connections from multiple browsers on the local network.
- **A two-tier access control system** combining password authentication with one-time QR token fast-authentication, balancing security with usability.
- **Origin-based privilege separation** that restricts file deletion to the device owner without relying on the client-side UI, providing a meaningful security guarantee against malicious remote clients.
- **Path traversal prevention** implemented via canonical path verification, protecting the device's file system from directory escape attacks.
- **A self-contained, bundled web interface** that works identically across all modern browsers without any external dependencies.

The resulting application fills a genuine gap: it is faster than cloud-based solutions, requires no proprietary app installation on the receiving device, works offline, and provides more security than naive file-serving utilities. It is a practical tool for everyday file transfer scenarios in homes, offices, classrooms, and field environments.

### 6.2 Future Work

Several extensions would meaningfully improve the application:

- **UDP Broadcast Discovery**: Implement a UDP broadcast listener so that client devices can discover the server's IP address automatically, without the host needing to verbally communicate the address or show the QR code. The `Plan.md` document already identifies this as a priority feature.
- **Rename Files**: Add a `POST /rename` endpoint (localhost-only) and a corresponding UI control, completing the standard set of file management operations.
- **File Preview**: Render thumbnail previews for images and video files in the file listing, improving the usability of the files page for media-heavy use cases.
- **Upload Progress on the Server Side**: Expose a `GET /progress/<upload-id>` endpoint to allow the client to poll for upload progress independently of the `XHR.upload.onprogress` event, which is unavailable in some mobile browser environments.
- **HTTPS / TLS**: Generate a self-signed certificate at first run and serve the application over HTTPS. This would allow the use of `Secure` cookies and would eliminate mixed-content warnings when the app is accessed from HTTPS-only browser contexts.
- **Persistent Storage Across Sessions**: Optionally move the `uploads/` directory to external storage (`Environment.DIRECTORY_DOCUMENTS`) so that files survive `onDestroy()` and can be accessed by other apps.
- **Multiple Upload Directories / Namespaces**: Allow the host to create named folders, each with independent access tokens, enabling selective sharing (e.g., share one folder with guests and another with colleagues).
- **Rate Limiting**: Implement per-IP request rate limiting in the NanoHTTPD handler to mitigate denial-of-service from misbehaving clients on the LAN.
- **Android Notification**: Show a persistent foreground-service notification displaying the server URL while the server is running, preventing the OS from killing the app when it is sent to the background.

---

## REFERENCES

[1] NanoHTTPD – Tiny, easily embeddable HTTP server in Java. Available at: https://github.com/NanoHttpd/nanohttpd  
[2] Android WebView Documentation. Available at: https://developer.android.com/reference/android/webkit/WebView  
[3] ZXing ("Zebra Crossing") barcode image processing library. Available at: https://github.com/zxing/zxing  
[4] journeyapps/zxing-android-embedded – Barcode scanner for Android. Available at: https://github.com/journeyapps/zxing-android-embedded  
[5] Android Developer Documentation – Data and File Storage Overview. Available at: https://developer.android.com/training/data-storage  
[6] OWASP – Path Traversal. Available at: https://owasp.org/www-community/attacks/Path_Traversal  
[7] OWASP – Session Management Cheat Sheet. Available at: https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html  
[8] MDN Web Docs – XMLHttpRequest.upload. Available at: https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/upload  
[9] MDN Web Docs – Using Fetch. Available at: https://developer.mozilla.org/en-US/docs/Web/API/Fetch_API/Using_Fetch  
[10] RFC 6265 – HTTP State Management Mechanism (Cookies). Available at: https://datatracker.ietf.org/doc/html/rfc6265  
[11] RFC 7233 – Hypertext Transfer Protocol (HTTP/1.1): Range Requests. Available at: https://datatracker.ietf.org/doc/html/rfc7233  
[12] AirDroid Official Website. Available at: https://www.airdroid.com  
[13] Snapdrop – Open Source Local File Sharing. Available at: https://github.com/RobinLinus/snapdrop  
[14] Android `DownloadManager` Documentation. Available at: https://developer.android.com/reference/android/app/DownloadManager  
[15] Android `ActivityResultContracts.ScanContract` (journeyapps). Available at: https://github.com/journeyapps/zxing-android-embedded#adding-to-your-project  
