package io.elankath.ping;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

//@WebServlet("/")
public class PingServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        final PrintWriter out = resp.getWriter();
        try {
            out.println("-- Received Request on host: " + req.getLocalName() + ", socket address:" + req.getLocalAddr() + ":"
                    + req.getLocalPort() + " at time: " + new Date(System.currentTimeMillis()) + " from client: " + req.getRemoteAddr());
            out.println("***** HTTP(S) PROXY INFO *****");
            final String httpProxyHost = System.getProperty("https.proxyHost");
            final String httpPort = System.getProperty("https.proxyPort");
            out.println("https.proxyHost = " + httpProxyHost);
            out.println("https.proxyPort= " + httpPort);
            final InetAddress[] allIps = InetAddress.getAllByName(httpProxyHost);
            out.println("InetAddress(es) of https.proxyHost = " + Arrays.asList(allIps));
            out.println("***** SYSTEM PROPERTIES *****");
            Properties properties = System.getProperties();
            properties.list(out);
            out.println("***** ENVIRONMENT VARIABLES **** ");
            System.getenv().entrySet().forEach(out::println);
        } catch (Throwable t) {
            t.printStackTrace(out);
        } finally {
            out.flush();
        }
    }
}
