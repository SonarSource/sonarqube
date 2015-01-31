package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http11.Http11Protocol;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class StartupLogsTest {

  Tomcat tomcat = mock(Tomcat.class, Mockito.RETURNS_DEEP_STUBS);
  Logger logger = mock(Logger.class);
  StartupLogs sut = new StartupLogs(logger);

  @Test
  public void logAjp() throws Exception {
    Connector connector = newConnector("AJP/1.3", "http");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("AJP/1.3 connector enabled on port 1234");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void logHttp() throws Exception {
    Connector connector = newConnector("HTTP/1.1", "http");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("HTTP connector enabled on port 1234");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void logHttps_default_ciphers() throws Exception {
    Connector connector = newConnector("HTTP/1.1", "https");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("HTTPS connector enabled on port 1234 | ciphers=JVM defaults");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void logHttps_overridden_ciphers() throws Exception {
    Connector connector = newConnector("HTTP/1.1", "https");
    connector.setProtocolHandlerClassName("org.apache.coyote.http11.Http11Protocol");
    ((Http11Protocol) connector.getProtocolHandler()).setCiphers("SSL_RSA,TLS_RSA_WITH_RC4");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});

    sut.log(tomcat);

    verify(logger).info("HTTPS connector enabled on port 1234 | ciphers=SSL_RSA,TLS_RSA_WITH_RC4");
    verifyNoMoreInteractions(logger);
  }

  @Test
  public void unsupported_connector() throws Exception {
    Connector connector = mock(Connector.class, Mockito.RETURNS_DEEP_STUBS);
    when(connector.getProtocol()).thenReturn("SPDY/1.1");
    when(connector.getScheme()).thenReturn("spdy");
    when(tomcat.getService().findConnectors()).thenReturn(new Connector[] {connector});
    try {
      sut.log(tomcat);
      fail();
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  private Connector newConnector(String protocol, String schema) {
    Connector httpConnector = new Connector(protocol);
    httpConnector.setScheme(schema);
    httpConnector.setPort(1234);
    return httpConnector;
  }
}
