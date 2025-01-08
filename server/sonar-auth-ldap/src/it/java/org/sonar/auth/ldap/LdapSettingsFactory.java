/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.auth.ldap;

import javax.annotation.Nullable;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.auth.ldap.server.LdapServer;

/**
 * Create Settings for most used test cases.
 */
public class LdapSettingsFactory {

  /**
   * Generate simple settings for 2 ldap servers that allows anonymous access.
   *
   * @return The specific settings.
   */
  public static MapSettings generateSimpleAnonymousAccessSettings(LdapServer exampleServer, @Nullable LdapServer infosupportServer) {
    MapSettings settings = new MapSettings();

    if (infosupportServer != null) {
      settings.setProperty("ldap.servers", "example,infosupport");

      settings.setProperty("ldap.example.url", exampleServer.getUrl())
        .setProperty("ldap.example.user.baseDn", "ou=users,dc=example,dc=org")
        .setProperty("ldap.example.group.baseDn", "ou=groups,dc=example,dc=org");
      settings.setProperty("ldap.infosupport.url", infosupportServer.getUrl())
        .setProperty("ldap.infosupport.user.baseDn", "ou=users,dc=infosupport,dc=com")
        .setProperty("ldap.infosupport.group.baseDn", "ou=groups,dc=infosupport,dc=com");
    } else {
      settings.setProperty("ldap.url", exampleServer.getUrl())
        .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
        .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org");
    }
    return settings;
  }

  /**
   * Generate settings for 2 ldap servers.
   *
   * @param exampleServer     The first ldap server.
   * @param infosupportServer The second ldap server.
   * @return The specific settings.
   */
  public static MapSettings generateAuthenticationSettings(LdapServer exampleServer, @Nullable LdapServer infosupportServer, String authMethod) {
    MapSettings settings = new MapSettings();

    if (infosupportServer != null) {
      settings.setProperty("ldap.servers", "example,infosupport");

      settings.setProperty("ldap.example.url", exampleServer.getUrl())
        .setProperty("ldap.example.bindDn", LdapContextFactory.AUTH_METHOD_SIMPLE.equals(authMethod) ? "cn=bind,ou=users,dc=example,dc=org" : "bind")
        .setProperty("ldap.example.bindPassword", "bindpassword")
        .setProperty("ldap.example.authentication", authMethod)
        .setProperty("ldap.example.realm", "example.org")
        .setProperty("ldap.example.user.baseDn", "ou=users,dc=example,dc=org")
        .setProperty("ldap.example.group.baseDn", "ou=groups,dc=example,dc=org");

      settings.setProperty("ldap.infosupport.url", infosupportServer.getUrl())
        .setProperty("ldap.infosupport.bindDn", LdapContextFactory.AUTH_METHOD_SIMPLE.equals(authMethod) ? "cn=bind,ou=users,dc=infosupport,dc=com" : "bind")
        .setProperty("ldap.infosupport.bindPassword", "bindpassword")
        .setProperty("ldap.infosupport.authentication", authMethod)
        .setProperty("ldap.infosupport.realm", "infosupport.com")
        .setProperty("ldap.infosupport.user.baseDn", "ou=users,dc=infosupport,dc=com")
        .setProperty("ldap.infosupport.group.baseDn", "ou=groups,dc=infosupport,dc=com");
    } else {
      settings.setProperty("ldap.url", exampleServer.getUrl())
        .setProperty("ldap.bindDn", LdapContextFactory.AUTH_METHOD_SIMPLE.equals(authMethod) ? "cn=bind,ou=users,dc=example,dc=org" : "bind")
        .setProperty("ldap.bindPassword", "bindpassword")
        .setProperty("ldap.authentication", authMethod)
        .setProperty("ldap.realm", "example.org")
        .setProperty("ldap.user.baseDn", "ou=users,dc=example,dc=org")
        .setProperty("ldap.group.baseDn", "ou=groups,dc=example,dc=org");
    }
    return settings;
  }
}
