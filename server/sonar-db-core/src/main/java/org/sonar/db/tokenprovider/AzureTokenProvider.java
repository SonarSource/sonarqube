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
package org.sonar.db.tokenprovider;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import java.util.Properties;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.DialectUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

// https://learn.microsoft.com/en-us/azure/developer/java/sdk/authentication/credential-chains
public class AzureTokenProvider implements TokenProvider {
  private static final String SONAR_JDBC_DIALECT = "sonar.jdbc.dialect";
  private final DefaultAzureCredential credential;

  public AzureTokenProvider() {
    this.credential = new DefaultAzureCredentialBuilder().build();
  }

  @Override
  public String getToken(Properties properties) {
    String jdbcUrl = properties.getProperty(JDBC_URL.getKey());
    Dialect dialect = DialectUtils.find(properties.getProperty(SONAR_JDBC_DIALECT), jdbcUrl);

    TokenRequestContext context = switch (dialect.getId()) {
      case MsSql.ID -> new TokenRequestContext()
        .addScopes("https://database.windows.net/.default");

      case PostgreSql.ID -> new TokenRequestContext()
        .addScopes("https://ossrdbms-aad.database.windows.net/.default");

      default -> throw new IllegalArgumentException("Unsupported dialect type");
    };

    AccessToken token = credential.getToken(context).block();
    if (token == null) {
      throw new IllegalStateException("Failed to acquire Azure token");
    }

    return token.getToken();
  }
}
