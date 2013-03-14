package org.sonar.application;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/* ------------------------------------------------------------ */
/**
 * TODO Duplicate code from Jetty 8 waiting for upgrade.
 *
 *
 * A handler that shuts the server down on a valid request. Used to do "soft" restarts from Java. If _exitJvm ist set to true a hard System.exit() call is being
 * made.
 *
 * This handler is a contribution from Johannes Brodwall: https://bugs.eclipse.org/bugs/show_bug.cgi?id=357687
 *
 * Usage:
 *
 * <pre>
    Server server = new Server(8080);
    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[]
    { someOtherHandler, new ShutdownHandler(server,&quot;secret password&quot;) });
    server.setHandler(handlers);
    server.start();
   </pre>
 *
   <pre>
   public static void attemptShutdown(int port, String shutdownCookie) {
        try {
            URL url = new URL("http://localhost:" + port + "/shutdown?token=" + shutdownCookie);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.getResponseCode();
            logger.info("Shutting down " + url + ": " + connection.getResponseMessage());
        } catch (SocketException e) {
            logger.debug("Not running");
            // Okay - the server is not running
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
  </pre>
 */
public class ShutdownHandler extends AbstractHandler {

  private final String _shutdownToken;

  private final Server _server;

  private boolean _exitJvm = false;

  /**
   * Creates a listener that lets the server be shut down remotely (but only from localhost).
   *
   * @param server
   *            the Jetty instance that should be shut down
   * @param shutdownToken
   *            a secret password to avoid unauthorized shutdown attempts
   */
  public ShutdownHandler(Server server, String shutdownToken) {
    this._server = server;
    this._shutdownToken = shutdownToken;
  }

  public void handle(String target, HttpServletRequest request, HttpServletResponse response,
      int dispatch) throws IOException, ServletException {
    if (!target.equals("/shutdown")) {
      return;
    }

    if (!request.getMethod().equals("POST")) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    if (!hasCorrectSecurityToken(request)) {
      System.err.println("Unauthorized shutdown attempt from " + getRemoteAddr(request));
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    if (!requestFromLocalhost(request)) {
      System.err.println("Unauthorized shutdown attempt from " + getRemoteAddr(request));
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    System.out.println("Shutting down by request from " + getRemoteAddr(request));

    new Thread() {
      public void run() {
        try {
          shutdownServer();
        } catch (InterruptedException e) {
          // Ignored
        } catch (Exception e) {
          throw new RuntimeException("Shutting down server", e);
        }
      }
    }.start();
  }

  private boolean requestFromLocalhost(HttpServletRequest request) {
    return "127.0.0.1".equals(getRemoteAddr(request));
  }

  protected String getRemoteAddr(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private boolean hasCorrectSecurityToken(HttpServletRequest request) {
    return _shutdownToken.equals(request.getParameter("token"));
  }

  private void shutdownServer() throws Exception {
    _server.stop();

    if (_exitJvm)
    {
      System.exit(0);
    }
  }

  public void setExitJvm(boolean exitJvm) {
    this._exitJvm = exitJvm;
  }

}
