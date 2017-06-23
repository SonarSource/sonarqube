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
package org.sonarqube.tests.upgrade;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import org.apache.commons.lang.StringUtils;

import static java.util.Objects.requireNonNull;

public class MssqlConfig {

  /**
   * Versions prior to 5.2 support only jTDS driver. Versions greater than or equal to 5.2
   * support only MS driver. The problem is that the test is configured with only
   * the MS URL, so it must be changed at runtime for versions < 5.2.
   */
  public static String fixUrl(Configuration conf, Version sqVersion) {
    String jdbcUrl = requireNonNull(conf.getString("sonar.jdbc.url"), "No JDBC url configured");
    if (jdbcUrl.startsWith("jdbc:sqlserver:") && !sqVersion.isGreaterThanOrEquals("5.2")) {
      // Job is configured with the new Microsoft driver, which is not supported by old versions of SQ
      String host = StringUtils.substringBetween(jdbcUrl, "jdbc:sqlserver://", ";databaseName=");
      String db = StringUtils.substringAfter(jdbcUrl, "databaseName=");
      jdbcUrl = "jdbc:jtds:sqlserver://" + host + "/" + db;
      System.out.println("Replaced JDBC url to: " + jdbcUrl);
      return jdbcUrl;
    }
    return jdbcUrl;
  }
}
