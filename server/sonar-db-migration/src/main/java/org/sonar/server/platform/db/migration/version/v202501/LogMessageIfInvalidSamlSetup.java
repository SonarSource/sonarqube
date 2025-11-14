/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202501;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.step.DataChange;

public class LogMessageIfInvalidSamlSetup extends DataChange {
  private static final Logger LOG = LoggerFactory.getLogger(LogMessageIfInvalidSamlSetup.class);

  @VisibleForTesting
  static final String SAML_ENABLED = "sonar.auth.saml.enabled";
  @VisibleForTesting
  static final String SIGN_REQUESTS_ENABLED = "sonar.auth.saml.signature.enabled";
  @VisibleForTesting
  static final String SERVICE_PROVIDER_CERTIFICATE = "sonar.auth.saml.sp.certificate.secured";
  @VisibleForTesting
  static final String SERVICE_PROVIDER_PRIVATE_KEY = "sonar.auth.saml.sp.privateKey.secured";

  public LogMessageIfInvalidSamlSetup(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {

    if (!isPropertyDefined(context, SAML_ENABLED)) {
      return;
    }

    boolean isSignRequestEnabled = isPropertyEnabled(context, SIGN_REQUESTS_ENABLED);

    boolean serviceProviderCertificate = isPropertyDefined(context, SERVICE_PROVIDER_CERTIFICATE);
    boolean serviceProviderPrivateKey = isPropertyDefined(context, SERVICE_PROVIDER_PRIVATE_KEY);

    if (isSignRequestEnabled) {
      return;
    }

    // With oneLogin library, setting the private key was enough to decrypt the response
    // With Spring security, both the private key and the certificate are needed to decrypt the response
    if (serviceProviderPrivateKey && !serviceProviderCertificate) {
      LOG.warn("We detected an invalid SAML configuration that will prevent users to login with SAML: Service provider certificate is needed to decrypt SAML response.");
    }
  }

  private static boolean isPropertyEnabled(Context context, String property) throws SQLException {
    return Boolean.TRUE.equals(context.prepareSelect("select count(*) from properties where prop_key = ? and text_value = ?")
      .setString(1, property)
      .setString(2, "true")
      .get(row -> 1 == row.getInt(1)));
  }

  private static boolean isPropertyDefined(Context context, String property) throws SQLException {
    return Boolean.TRUE.equals(context.prepareSelect("select count(*) from properties where prop_key = ? and text_value is not null")
      .setString(1, property)
      .get(t -> 1 == t.getInt(1)));
  }
}
