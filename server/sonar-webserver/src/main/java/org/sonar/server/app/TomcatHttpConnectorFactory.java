/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.app;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.process.Props;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.lang.String.format;
import static org.sonar.process.ProcessProperties.Property.*;

public class TomcatHttpConnectorFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(TomcatHttpConnectorFactory.class);

  static final String HTTP_PROTOCOL = "HTTP/1.1";
  // Max HTTP headers size must be 48kb to accommodate the authentication token used for negotiate protocol of windows authentication.
  static final int MAX_HTTP_HEADER_SIZE_BYTES = 48 * 1024;
  private static final int MAX_POST_SIZE = -1;


  public Connector createConnector(Props props) {
    boolean sslEnabled = props.valueAsBoolean(SONAR_SSL_ENABLED.getKey(), false);
    Connector connector = new Connector(HTTP_PROTOCOL);

    configureConnector(connector, props, sslEnabled);
    configurePool(props, connector);
    configureCompression(connector);

    if (sslEnabled) {
      configureSsl(connector, props);
    }
    return connector;
  }

  private static void configureConnector(Connector connector, Props props, boolean sslEnabled) {
    connector.setURIEncoding(StandardCharsets.UTF_8.name());
    connector.setProperty("address", props.value(WEB_HOST.getKey(), "0.0.0.0"));
    connector.setProperty("socket.soReuseAddress", "true");
    connector.setProperty("relaxedQueryChars", "\"<>[\\]^`{|}");
    connector.setProperty("maxHttpHeaderSize", String.valueOf(MAX_HTTP_HEADER_SIZE_BYTES));
    connector.setMaxPostSize(MAX_POST_SIZE);

    int port = determinePort(props);
    connector.setPort(port);
    LOGGER.info("Starting Tomcat on {} port {}", sslEnabled ? "HTTPS" : "HTTP", port);
  }

  private static int determinePort(Props props) {
    int port = props.valueAsInt(WEB_PORT.getKey(), 9000);
    if (port < 0) {
      throw new IllegalStateException(format("Port %s is invalid", port));
    }
    return port;
  }

  private static void configurePool(Props props, Connector connector) {
    connector.setProperty("minSpareThreads", String.valueOf(props.valueAsInt(WEB_HTTP_MIN_THREADS.getKey(), 5)));
    connector.setProperty("maxThreads", String.valueOf(props.valueAsInt(WEB_HTTP_MAX_THREADS.getKey(), 50)));
    connector.setProperty("acceptCount", String.valueOf(props.valueAsInt(WEB_HTTP_ACCEPT_COUNT.getKey(), 25)));
    connector.setProperty("keepAliveTimeout", String.valueOf(props.valueAsInt(WEB_HTTP_KEEP_ALIVE_TIMEOUT.getKey(), 60000)));
  }

  private static void configureCompression(Connector connector) {
    connector.setProperty("compression", "on");
    connector.setProperty("compressionMinSize", "1024");
    connector.setProperty("compressibleMimeType", "text/html,text/xml,text/plain,text/css,application/json,application/javascript,text/javascript");
  }

  private static void configureSsl(Connector connector, Props props) {
    String certBase64 = props.value(SONAR_SSL_PEM_BASE64_CERTIFICATE.getKey());
    String keyBase64 = props.value(SONAR_SSL_PEM_BASE64_KEY.getKey());

    if (certBase64 == null || keyBase64 == null) {
      throw new IllegalStateException("SSL is enabled, but Base64 certificate and key values are not set.");
    }

    File certFile = decodeBase64ToFile(certBase64, "cert.pem");
    File keyFile = decodeBase64ToFile(keyBase64, "key.pem");

    LOGGER.debug("Certificate file created at: {}", certFile.getAbsolutePath());
    LOGGER.debug("Key file created at: {}", keyFile.getAbsolutePath());

    SSLHostConfig sslHostConfig = new SSLHostConfig();
    sslHostConfig.setProtocols("TLSv1.2,TLSv1.3");

    SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, SSLHostConfigCertificate.Type.RSA);
    certificate.setCertificateFile(certFile.getAbsolutePath());
    certificate.setCertificateKeyFile(keyFile.getAbsolutePath());

    sslHostConfig.addCertificate(certificate);
    connector.addSslHostConfig(sslHostConfig);

    connector.setSecure(true);
    connector.setScheme("https");
    connector.setProperty("SSLEnabled", "true");

    LOGGER.info("SSL configuration loaded from Base64 properties.");
  }

  private static File decodeBase64ToFile(String base64Content, String fileName) {
    byte[] decodedBytes = Base64.getDecoder().decode(base64Content.getBytes(StandardCharsets.UTF_8));
    try {
      File tempFile = File.createTempFile(fileName, null);
      tempFile.deleteOnExit();
      try (FileOutputStream fos = new FileOutputStream(tempFile)) {
        fos.write(decodedBytes);
      }
      return tempFile;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to decode Base64 and write to file: " + fileName, e);
    }
  }
}
