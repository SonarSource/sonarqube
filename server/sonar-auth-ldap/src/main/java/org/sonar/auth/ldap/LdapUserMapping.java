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
package org.sonar.auth.ldap;

import org.apache.commons.lang3.StringUtils;
import org.sonar.api.config.Configuration;

import static org.sonar.auth.ldap.LdapSettingsManager.MANDATORY_LDAP_PROPERTY_ERROR;

/**
 * @author Evgeny Mandrikov
 */
public class LdapUserMapping {

  private static final String DEFAULT_NAME_ATTRIBUTE = "cn";
  private static final String DEFAULT_EMAIL_ATTRIBUTE = "mail";
  private static final String DEFAULT_REQUEST = "(&(objectClass=inetOrgPerson)(uid={login}))";

  private final String baseDn;
  private final String request;
  private final String realNameAttribute;
  private final String emailAttribute;

  /**
   * Constructs mapping from Sonar settings.
   */
  public LdapUserMapping(Configuration config, String settingsPrefix) {
    String userBaseDnSettingKey = settingsPrefix + ".user.baseDn";
    this.baseDn = config.get(userBaseDnSettingKey).orElseThrow(() -> new LdapException(String.format(MANDATORY_LDAP_PROPERTY_ERROR, userBaseDnSettingKey)));
    this.realNameAttribute = config.get(settingsPrefix + ".user.realNameAttribute").orElse(DEFAULT_NAME_ATTRIBUTE);
    this.emailAttribute = config.get(settingsPrefix + ".user.emailAttribute").orElse(DEFAULT_EMAIL_ATTRIBUTE);

    String req = config.get(settingsPrefix + ".user.request").orElse(DEFAULT_REQUEST);
    req = StringUtils.replace(req, "{login}", "{0}");
    this.request = req;
  }

  /**
   * Search for this mapping.
   */
  public LdapSearch createSearch(LdapContextFactory contextFactory, String username) {
    return new LdapSearch(contextFactory)
      .setBaseDn(getBaseDn())
      .setRequest(getRequest())
      .setParameters(username);
  }

  /**
   * Base DN. For example "ou=users,o=mycompany" or "cn=users" (Active Directory Server).
   */
  public String getBaseDn() {
    return baseDn;
  }

  /**
   * Request. For example:
   * <pre>
   * (&(objectClass=inetOrgPerson)(uid={0}))
   * (&(objectClass=user)(sAMAccountName={0}))
   * </pre>
   */
  public String getRequest() {
    return request;
  }

  /**
   * Real Name Attribute. For example "cn".
   */
  public String getRealNameAttribute() {
    return realNameAttribute;
  }

  /**
   * EMail Attribute. For example "mail".
   */
  public String getEmailAttribute() {
    return emailAttribute;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
      "baseDn=" + getBaseDn() +
      ", request=" + getRequest() +
      ", realNameAttribute=" + getRealNameAttribute() +
      ", emailAttribute=" + getEmailAttribute() +
      "}";
  }

}
