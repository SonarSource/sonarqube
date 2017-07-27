/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import org.picocontainer.injectors.ProviderAdapter;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.batch.bootstrapper.EnvironmentInformation;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClientFactories;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static org.apache.commons.lang.StringUtils.defaultIfBlank;

@ScannerSide
public class ScannerWsClientProvider extends ProviderAdapter {

  static final int CONNECT_TIMEOUT_MS = 5_000;
  static final String READ_TIMEOUT_SEC_PROPERTY = "sonar.ws.timeout";
  static final int DEFAULT_READ_TIMEOUT_SEC = 60;

  private ScannerWsClient wsClient;

  public synchronized ScannerWsClient provide(final GlobalProperties settings, final EnvironmentInformation env, GlobalMode globalMode) {
    if (wsClient == null) {
      String url = defaultIfBlank(settings.property("sonar.host.url"), CoreProperties.SERVER_BASE_URL_DEFAULT_VALUE);
      HttpConnector.Builder connectorBuilder = HttpConnector.newBuilder();

      String timeoutSec = defaultIfBlank(settings.property(READ_TIMEOUT_SEC_PROPERTY), valueOf(DEFAULT_READ_TIMEOUT_SEC));
      String login = defaultIfBlank(settings.property(CoreProperties.LOGIN), null);
      connectorBuilder
        .readTimeoutMilliseconds(parseInt(timeoutSec) * 1_000)
        .connectTimeoutMilliseconds(CONNECT_TIMEOUT_MS)
        .userAgent(env.toString())
        .url(url)
        .credentials(login, settings.property(CoreProperties.PASSWORD));

      // OkHttp detect 'http.proxyHost' java property, but credentials should be filled
      final String proxyUser = System.getProperty("http.proxyUser", "");
      if (!proxyUser.isEmpty()) {
        connectorBuilder.proxyCredentials(proxyUser, System.getProperty("http.proxyPassword"));
      }
      if(Boolean.valueOf( System.getProperty( "sonar.http.ssl.insecure", "false" ) )) {
        try {
          // Create a trust manager that does not validate certificate chains
          X509TrustManager easyTrust =
              new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException
                {
                  // no op
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                  // no op
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                  return new X509Certificate[0];
                }
              };

          // Init the ezy trusting trust manager
          SSLContext sslContext = SSLContext.getInstance( "SSL");
          sslContext.init( new KeyManager[0], new TrustManager[]{easyTrust}, new java.security.SecureRandom());
          // get an ssl socket factory with our all trusting manager
          SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
          connectorBuilder.setSSLSocketFactory( sslSocketFactory ).setTrustManager( easyTrust );
        } catch ( NoSuchAlgorithmException | KeyManagementException e ) {
          // should not happen but just in case log it
          throw new RuntimeException( e.getMessage(), e );
        }
      }
      if(Boolean.valueOf( System.getProperty( "sonar.http.ssl.allowall", "false" ) )) {
        connectorBuilder.hostnameVerifier( ( s, sslSession ) -> true );
      }
      wsClient = new ScannerWsClient(WsClientFactories.getDefault().newClient(connectorBuilder.build()), login != null, globalMode);
    }
    return wsClient;
  }
}
