package dev.cringekid.mediatools;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP shim that checks for yt-dlp/ffmpeg availability on the local machine.
 * This does not depend on the MediaTools class and can be run standalone from the repo.
 * Usage: java -cp media-tools.jar java.dev.cringekid.mediatools.MediaToolsHttpServer [toolsRoot] [port]
 */
public final class MediaToolsHttpServer {
    public static void main(String[] args) throws Exception {
        Path toolsRoot = Path.of(args.length > 0 ? args[0] : "media_radio_tools");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8765;

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/status", new TextHandler(() -> "media-tools-http-shim ready"));
        server.createContext("/isYtDlpAvailable", new TextHandler(() -> Boolean.toString(isExecutableAvailable(toolsRoot, "yt-dlp"))));
        server.createContext("/isFfmpegAvailable", new TextHandler(() -> Boolean.toString(isExecutableAvailable(toolsRoot, "ffmpeg"))));
        server.createContext("/isSupportedPlatform", new TextHandler(() -> "true"));

        server.createContext("/requireYtDlpCommand", new TextHandler(() -> findExecutableCommand(toolsRoot, "yt-dlp")));
        server.createContext("/requireFfmpegCommand", new TextHandler(() -> findExecutableCommand(toolsRoot, "ffmpeg")));

        server.createContext("/resolveYtDlpCommand", new TextHandler(() -> findExecutablePath(toolsRoot, "yt-dlp")));
        server.createContext("/resolveFfmpegCommand", new TextHandler(() -> findExecutablePath(toolsRoot, "ffmpeg")));

        server.createContext("/getExpectedYtDlpPath", new TextHandler(() -> findExecutablePath(toolsRoot, "yt-dlp")));
        server.createContext("/getExpectedFfmpegPath", new TextHandler(() -> findExecutablePath(toolsRoot, "ffmpeg")));
        server.createContext("/getFfmpegLocationForYtDlp", new TextHandler(() -> ""));

        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("MediaTools HTTP shim starting on port " + port + ", toolsRoot=" + toolsRoot);
        server.start();
    }

    private static boolean isExecutableAvailable(Path toolsRoot, String name) {
        // Check common local tool paths first
        Path p1 = toolsRoot.resolve(name);
        Path p2 = toolsRoot.resolve(name + (isWindows() ? ".exe" : ""));
        if (Files.exists(p1) || Files.exists(p2)) {
            return true;
        }
        // Try PATH by invoking '<name> --version'
        try {
            ProcessBuilder pb = new ProcessBuilder(name, "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int rc = p.waitFor();
            return rc == 0 || rc == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static String findExecutableCommand(Path toolsRoot, String name) {
        if (isExecutableAvailable(toolsRoot, name)) {
            return name;
        }
        return "";
    }

    private static String findExecutablePath(Path toolsRoot, String name) {
        Path p1 = toolsRoot.resolve(name);
        Path p2 = toolsRoot.resolve(name + (isWindows() ? ".exe" : ""));
        if (Files.exists(p1)) return p1.toAbsolutePath().toString();
        if (Files.exists(p2)) return p2.toAbsolutePath().toString();
        // Not found in toolsRoot; try PATH lookup via 'which' or 'where'
        try {
            if (isWindows()) {
                ProcessBuilder pb = new ProcessBuilder("where", name);
                Process p = pb.start();
                try (var s = new java.util.Scanner(p.getInputStream())) {
                    if (s.hasNextLine()) return s.nextLine().trim();
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder("which", name);
                Process p = pb.start();
                try (var s = new java.util.Scanner(p.getInputStream())) {
                    if (s.hasNextLine()) return s.nextLine().trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static final class TextHandler implements HttpHandler {
        private final TextSupplier supplier;

        TextHandler(TextSupplier supplier) {
            this.supplier = supplier;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String text = supplier.get();
                byte[] bytes = text == null ? new byte[0] : text.getBytes();
                exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (Exception e) {
                byte[] bytes = new byte[0];
                exchange.sendResponseHeaders(500, bytes.length);
                exchange.close();
            }
        }
    }

    @FunctionalInterface
    private interface TextSupplier {
        String get() throws Exception;
    }
}
