/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * @author Evgeny Mandrikov
 */
public class Krb5LoginConfiguration extends Configuration {
  private static final AppConfigurationEntry[] CONFIG_LIST = new AppConfigurationEntry[1];

  static {
    String loginModule = "com.sun.security.auth.module.Krb5LoginModule";
    AppConfigurationEntry.LoginModuleControlFlag flag = AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
    CONFIG_LIST[0] = new AppConfigurationEntry(loginModule, flag, new HashMap<>());
  }

  /**
   * Creates a new instance of Krb5LoginConfiguration.
   */
  public Krb5LoginConfiguration() {
    super();
  }

  /**
   * Interface method requiring us to return all the LoginModules we know about.
   */
  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
    // We will ignore the applicationName, since we want all apps to use Kerberos V5
    return CONFIG_LIST.clone();
  }

  /**
   * Interface method for reloading the configuration. We don't need this.
   */
  @Override
  public void refresh() {
    // Right now this is a load once scheme and we will not implement the refresh method
  }
}
