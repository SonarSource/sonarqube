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
package org.sonar.auth.ldap.server;

import org.junit.rules.ExternalResource;

public class LdapServer extends ExternalResource {

  private ApacheDS server;
  private String ldif;
  private final String realm;
  private final String baseDn;

  public LdapServer(String ldifResourceName) {
    this(ldifResourceName, "example.org", "dc=example,dc=org");
  }

  public LdapServer(String ldifResourceName, String realm, String baseDn) {
    this.ldif = ldifResourceName;
    this.realm = realm;
    this.baseDn = baseDn;
  }

  @Override
  protected void before() throws Throwable {
    server = ApacheDS.start(realm, baseDn);
    server.importLdif(LdapServer.class.getResourceAsStream(ldif));
  }

  @Override
  protected void after() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public String getUrl() {
    return server.getUrl();
  }

  public void disableAnonymousAccess() {
    server.disableAnonymousAccess();
  }

  public void enableAnonymousAccess() {
    server.enableAnonymousAccess();
  }

}
