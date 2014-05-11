/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.apache.catalina.Globals;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.buf.B2CConverter;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.codec.binary.Base64;

import javax.servlet.ServletException;
import java.io.IOException;
import java.security.cert.X509Certificate;

public class ClientCertValve extends ValveBase {

  private static final String AUTH_HEADER = "authorization";

  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    X509Certificate certs[] = (X509Certificate[])
            request.getAttribute(Globals.CERTIFICATES_ATTR);
    if (certs != null) {
      String user = extractUserName(certs[0]);
      MessageBytes authorization =
              request.getCoyoteRequest().getMimeHeaders()
                      .getValue(AUTH_HEADER);
      if (authorization == null) {
        request.getCoyoteRequest().getMimeHeaders().addValue(AUTH_HEADER);
        authorization =
                request.getCoyoteRequest().getMimeHeaders()
                        .getValue(AUTH_HEADER);
        String authValue = "basic " + new String(encodeAuthHeader(user, SonarJDBCRealm.RANDOM_PASSWORD));
        authorization.setString(authValue);
      }
    }
    getNext().invoke(request, response);
  }

  private byte[] encodeAuthHeader(String username, String password) throws IOException {
    username = new String(username.getBytes(), 0, username.length(), B2CConverter.ISO_8859_1);
    password = new String(password.getBytes(), 0, password.length(), B2CConverter.ISO_8859_1);
    String cred = username + ":" + password;
    return Base64.encodeBase64(cred.getBytes());
  }

  private String extractUserName(X509Certificate cert) {
    return cert.getSubjectX500Principal().getName().split(",")[1].split("=")[1];
  }
}
