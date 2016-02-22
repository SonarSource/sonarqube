/*
 * Copyright (C) 2009-2016 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonarsource.sonarqube.upgrade;

import com.sonar.orchestrator.config.Configuration;
import com.sonar.orchestrator.version.Version;
import org.apache.commons.lang.StringUtils;

public class MssqlConfig {

  /**
   * Versions prior to 5.2 support only jTDS driver. Versions greater than or equal to 5.2
   * support only MS driver. The problem is that the test is configured with only
   * the MS URL, so it must be changed at runtime for versions < 5.2.
   */
  public static String fixUrl(Configuration conf, Version sqVersion) {
    String jdbcUrl = conf.getString("sonar.jdbc.url");
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
