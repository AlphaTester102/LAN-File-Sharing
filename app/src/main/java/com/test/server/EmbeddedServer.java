package com.test.server;

import android.content.Context;
import fi.iki.elonen.NanoHTTPD;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EmbeddedServer extends NanoHTTPD {
    private final Context context;
    private final File uploadsDir;

    public enum ServerMode { PUBLIC, PRIVATE }
    private volatile ServerMode serverMode = ServerMode.PUBLIC;
    private volatile String serverPassword = null;
    private final Set<String> validTokens = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> validQrTokens = Collections.synchronizedSet(new HashSet<>());

    public void clearUploads() {
        File[] files = uploadsDir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.delete()) {
                    Log.w("EmbeddedServer", "Failed to delete upload: " + f.getAbsolutePath());
                }
            }
        }
    }

    public String generateQrToken() {
        if (serverMode != ServerMode.PRIVATE) return null;
        String token = UUID.randomUUID().toString();
        validQrTokens.add(token);
        return token;
    }

    public void setServerConfig(ServerMode mode, String password) {
        this.serverMode = mode;
        this.serverPassword = password;
        this.validTokens.clear();
        this.validQrTokens.clear();
    }

    public EmbeddedServer(Context context, int port) {
        super("0.0.0.0", port);
        this.context = context.getApplicationContext();
        this.uploadsDir = new File(this.context.getFilesDir(), "uploads");
        if (!uploadsDir.exists()) {
            uploadsDir.mkdirs();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (serverMode == ServerMode.PRIVATE && !isLocalhost(session)) {
            String qrToken = session.getParms().get("qr_token");
            if (qrToken != null && validQrTokens.contains(qrToken)) {
                String sessionToken = UUID.randomUUID().toString();
                validTokens.add(sessionToken);
                try {
                    Response page = serveAsset("index.html");
                    page.addHeader("Set-Cookie", "auth_token=" + sessionToken + "; Path=/; HttpOnly");
                    page.addHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                    return page;
                } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
                }
            }
            if ("/auth".equals(uri) && Method.POST.equals(session.getMethod())) {
                return handleAuth(session);
            }
            if ("/login".equals(uri)) {
                try { return serveAsset("login.html"); } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
                }
            }
            if (!uri.endsWith(".css") && !uri.endsWith(".js") && !isAuthenticated(session)) {
                try { return serveAsset("login.html"); } catch (IOException e) {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
                }
            }
        }

        try {
            if ("/".equals(uri)) {
                return serveAsset("index.html");
            }

            if ("/upload".equals(uri)) {
                if (Method.POST.equals(session.getMethod())) {
                    return handleUpload(session);
                }
                return serveAsset("upload.html");
            }

            if ("/files".equals(uri) || "/files.html".equals(uri)) {
                return serveAsset("files.html");
            }

            if ("/server-info".equals(uri)) {
                return getServerInfo();
            }
            if ("/list".equals(uri)) {
                return listFiles();
            }
            if ("/is-owner".equals(uri)) {
                return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"isOwner\":" + isLocalhost(session) + "}");
            }
            if (uri.endsWith(".css") || uri.endsWith(".js")) {
                String asset = uri.substring(1);
                return serveAsset(asset);
            }
            if (uri.startsWith("/download/")) {
                String filename = uri.substring("/download/".length());
                return serveFile(filename);
            }
            if ("/delete".equals(uri) && Method.POST.equals(session.getMethod())) {
                if (!isLocalhost(session)) {
                    return forbiddenResponse();
                }
                return handleDelete(session);
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error: " + e.getMessage());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found");
    }

    private boolean isLocalhost(IHTTPSession session) {
        String remoteIp = session.getHeaders().get("remote-addr");
        if (remoteIp == null) remoteIp = session.getHeaders().get("http-client-ip");
        return "127.0.0.1".equals(remoteIp) || "::1".equals(remoteIp);
    }

    private boolean isAuthenticated(IHTTPSession session) {
        String cookieHeader = session.getHeaders().get("cookie");
        if (cookieHeader == null) return false;
        for (String part : cookieHeader.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && "auth_token".equals(kv[0].trim())) {
                return validTokens.contains(kv[1].trim());
            }
        }
        return false;
    }

    private Response handleAuth(IHTTPSession session) {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"Invalid request\"}");
        }
        String password = session.getParms().get("password");
        if (password == null || !password.equals(serverPassword)) {
            return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", "{\"error\":\"Invalid password\"}");
        }
        String token = UUID.randomUUID().toString();
        validTokens.add(token);
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\":true}");
        response.addHeader("Set-Cookie", "auth_token=" + token + "; Path=/; HttpOnly");
        return response;
    }

    private Response forbiddenResponse() {
        return newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                "application/json",
                "{\"error\":\"Access denied.\"}"
        );
    }

    private Response serveAsset(String assetName) throws IOException {
        InputStream stream = context.getAssets().open(assetName);
        String mime = getCustomMimeType(assetName);
        return newChunkedResponse(Response.Status.OK, mime, stream);
    }

    private Response serveFile(String filename) throws IOException {
        File file = new File(uploadsDir, filename);
        if (!file.getCanonicalPath().startsWith(uploadsDir.getCanonicalPath() + File.separator)
                && !file.getCanonicalPath().equals(uploadsDir.getCanonicalPath())) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "text/plain", "Forbidden");
        }
        if (!file.exists() || !file.isFile()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
        }
        FileInputStream fis = new FileInputStream(file);
        String mime = getCustomMimeType(filename);
        Response response = newChunkedResponse(Response.Status.OK, mime, fis);
        response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        return response;
    }

    private Response handleUpload(IHTTPSession session) throws IOException {
        Map<String, String> files = new HashMap<>();
        try {
            session.parseBody(files);
        } catch (ResponseException e) {
            return newFixedLengthResponse(e.getStatus(), "text/plain", e.getMessage());
        }

        String tempFilePath = files.get("file");
        if (tempFilePath == null) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"No file uploaded\"}");
        }

        String filename = session.getParms().get("file");
        if (filename == null || filename.isEmpty()) {
            filename = new File(tempFilePath).getName();
        }

        File dest = new File(uploadsDir, filename);
        File tempFile = new File(tempFilePath);
        if (!tempFile.renameTo(dest)) {
            try (InputStream in = new FileInputStream(tempFile);
                 java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"message\":\"File uploaded successfully\"}");
    }

    private Response handleDelete(IHTTPSession session) {
        String filename = session.getParms().get("file");
        if (filename == null || filename.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json", "{\"error\":\"No filename provided\"}");
        }

        File file;
        try {
            file = new File(uploadsDir, filename);
            if (!file.getCanonicalPath().startsWith(uploadsDir.getCanonicalPath() + File.separator)) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json", "{\"error\":\"Forbidden\"}");
            }
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Server error\"}");
        }

        if (!file.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", "{\"error\":\"File not found\"}");
        }

        if (file.delete()) {
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"message\":\"File deleted successfully\"}");
        } else {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{\"error\":\"Failed to delete file\"}");
        }
    }

    private Response getServerInfo() {
        String ip = getLocalIpAddress();
        String json = "{\"url\":\"http://" + ip + ":" + getListeningPort() + "/\"}"; 
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response listFiles() {
        File[] files = uploadsDir.listFiles();
        StringBuilder json = new StringBuilder("[");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        if (files != null) {
            int count = 0;
            for (File file : files) {
                if (file.isFile()) {
                    if (count > 0) json.append(",");
                    String name = file.getName();
                    String type = "unknown";
                    int lastDot = name.lastIndexOf('.');
                    if (lastDot > 0) {
                        type = name.substring(lastDot + 1).toLowerCase();
                    }
                    json.append("{")
                        .append("\"name\":\"").append(name).append("\",")
                        .append("\"type\":\"").append(type).append("\",")
                        .append("\"size\":").append(file.length()).append(",")
                        .append("\"formattedDate\":\"").append(sdf.format(new Date(file.lastModified()))).append("\"")
                        .append("}");
                    count++;
                }
            }
        }
        json.append("]");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
    }

    private String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!address.isLoopbackAddress() && address.getAddress().length == 4) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }

    private String getCustomMimeType(String filename) {
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".html")) return "text/html";
        if (lowerName.endsWith(".css")) return "text/css";
        if (lowerName.endsWith(".js")) return "application/javascript";
        if (lowerName.endsWith(".json")) return "application/json";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".epub")) return "application/epub+zip";
        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".zip")) return "application/zip";
        return "application/octet-stream";
    }
}
