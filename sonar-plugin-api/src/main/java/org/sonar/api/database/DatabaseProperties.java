/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.database;

/**
 * @deprecated since 7.1, plugins don't connect to database
 */
@Deprecated
public interface DatabaseProperties {

  String PROP_URL = "sonar.jdbc.url";
  String PROP_DRIVER = "sonar.jdbc.driverClassName";
  String PROP_USER = "sonar.jdbc.username";

  String PROP_USER_DEFAULT_VALUE = "";
  String PROP_PASSWORD = "sonar.jdbc.password";
  String PROP_PASSWORD_DEFAULT_VALUE = "";

  /**
   * @since 3.2
   */
  String PROP_EMBEDDED_PORT = "sonar.embeddedDatabase.port";

  /**
   * @since 3.2
   */
  String PROP_EMBEDDED_PORT_DEFAULT_VALUE = "9092";
}
