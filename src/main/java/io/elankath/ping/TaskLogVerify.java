package io.elankath.ping;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@WebServlet("/")
public class TaskLogVerify extends HttpServlet {
    private final Path parentDir = Paths.get("/var/vcap/data/1ea22629-916c-4eeb-857a-0d569dc7d784/tasks/");

    final ScheduledExecutorService executor = newSingleThreadScheduledExecutor();
    private PrintWriter logWriter;
    private final Path log = Paths.get(System.getProperty("java.io.tmpdir")).resolve("log.txt");
    private Path tenantDir = parentDir.resolve("nagdev");


    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        try {
            out.println("-- Received Request on host: " + req.getLocalName() + ", socket address:" + req.getLocalAddr() + ":"
                    + req.getLocalPort() + " at time: " + new Date(System.currentTimeMillis()) + " from client: " + req.getRemoteAddr());
            String tenant = req.getParameter("tenant");
            if (tenant != null) {
                tenantDir = parentDir.resolve(tenant);
            }
            if (logWriter != null) {
                resp.sendError(409, "Operation already in progress");
            } else {
                this.logWriter = new PrintWriter(Files.newBufferedWriter(log, UTF_8));
                executor.submit(() -> cleanupExpiredLogFiles(logWriter));
                out.println("-- Scheduled cleanup of expired task log files under " + tenantDir);
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
        } finally {
            out.flush();
        }
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        try {
            out.println("-- Received Request on host: " + req.getLocalName() + ", socket address:" + req.getLocalAddr() + ":"
                    + req.getLocalPort() + " at time: " + new Date(System.currentTimeMillis()) + " from client: " + req.getRemoteAddr());
            if (Files.exists(log)) {
                final byte[] bytes = Files.readAllBytes(log);
                out.println(new String(bytes, UTF_8));
            } else {
                out.println("no log file created");
            }
        } catch (Throwable t) {
            t.printStackTrace(out);
        } finally {
            out.flush();
        }
    }

    public void closeLogWriter() {
        if (logWriter != null) {
            try {
                logWriter.close();
                logWriter = null;
            } catch (Exception e) {
                System.out.println("Error closing logWriter: " + logWriter);
            }
        }
    }

    void cleanupExpiredLogFiles(final PrintWriter out) {
        try {
            walkAndCleanup(tenantDir, out);
        } catch (Throwable t) {
            t.printStackTrace(out);
        } finally {
            out.flush();
            closeLogWriter();
        }
    }

    void walkAndCleanup(final Path dir, final PrintWriter out) throws IOException {
        out.println("(walkAndCleanup) Commencing walking dir: " + tenantDir + " ............." + " at " + now());
        out.flush();
        try (final Stream<Path> paths = Files.walk(dir)) {
            out.println("(walkAndCleanup) Got stream: " + paths + " at " + now());
            paths.peek(p -> out.println("(walkAndCleanup) Analyzing candidate path: " + p + " at " + now()))
                    .peek(p -> out.flush())
                    .filter(p -> hasExpired(p, out))
                    .peek(p -> out.println("(walkAndCleanup) Deleting file: " + p + " at " + now()))
                    .peek(p -> out.flush())
                    .forEach(p -> deletePath(p, out));
        }
        out.println("(walkAndCleanup) Finished at " + now());
    }

    public void deletePath(final Path p, final PrintWriter out) {
        try {
            if (Objects.equals(p, tenantDir)) {
                out.println("(deletePath) Will not delete: " + tenantDir);
            }
            if (Files.isDirectory(p)) {
                try (final DirectoryStream<Path> children = Files.newDirectoryStream(p)) {
                    if (children.iterator().hasNext()) {
                        out.println("(deletePath) Cannot delete: " + p + " since it has children");
                        return;
                    }
                }
            }
            Files.delete(p);
            out.println("Deleted: " + p + " at " + now());
        } catch (IOException e) {
            out.println("(deletePath) Error deleting " + p);
            e.printStackTrace(out);
        } finally {
            out.flush();
        }
    }

    public static boolean isOlder(final long time, final Duration expiry) { //TODO rename this method to hasExpired
        return currentTimeMillis() - time > expiry.toMillis();
    }

    boolean hasExpired(final Path p, final PrintWriter out) {
        try {
            final BasicFileAttributes attributes = Files.readAttributes(p, BasicFileAttributes.class);
            final FileTime lastModifiedTime = attributes.lastModifiedTime();
//            final long lastModifiedTime = lastModifiedTime.toMillis();
            final Duration expiry = Duration.ofDays(1);
            boolean hasExpired = isOlder(lastModifiedTime.toMillis(), Duration.ofDays(1));
            out.printf("(hasExpired) %s, lastModifiedTime: %s, expiry: %s, hasExpired: %s\n", p, lastModifiedTime, expiry, hasExpired);
            return hasExpired;
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
