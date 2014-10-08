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

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessConstants;
import org.sonar.process.Props;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdbcSettings {

  static enum Provider {
    h2(null), jtds("lib/jdbc/jtds"), mysql("lib/jdbc/mysql"), oracle("extensions/jdbc-driver/oracle"),
    postgresql("lib/jdbc/postgresql");

    final String path;

    Provider(@Nullable String path) {
      this.path = path;
    }
  }

  public void checkAndComplete(File homeDir, Props props) {
    String url = props.nonNullValue(ProcessConstants.JDBC_URL);
    Provider provider = driverProvider(url);
    checkUrlParameters(provider, url);
    String driverPath = driverPath(homeDir, provider);
    if (driverPath != null) {
      props.set(ProcessConstants.JDBC_DRIVER_PATH, driverPath);
    }
  }

  @CheckForNull
  String driverPath(File homeDir, Provider provider) {
    String dirPath = provider.path;
    if (dirPath == null) {
      return null;
    }
    File dir = new File(homeDir, dirPath);
    if (!dir.exists()) {
      throw new MessageException("Directory does not exist: " + dirPath);
    }
    List<File> files = new ArrayList<File>(FileUtils.listFiles(dir, new String[] {"jar"}, false));
    if (files.isEmpty()) {
      throw new MessageException("Directory does not contain JDBC driver: " + dirPath);
    }
    if (files.size() > 1) {
      throw new MessageException("Directory must contain only one JAR file: " + dirPath);
    }
    return files.get(0).getAbsolutePath();
  }

  Provider driverProvider(String url) {
    Pattern pattern = Pattern.compile("jdbc:(\\w+):.+");
    Matcher matcher = pattern.matcher(url);
    if (!matcher.find()) {
      throw new MessageException(String.format("Bad format of JDBC URL: " + url));
    }
    String key = matcher.group(1);
    try {
      return Provider.valueOf(key);
    } catch (IllegalArgumentException e) {
      throw new MessageException(String.format(String.format("Unsupported JDBC driver provider: %s. Accepted values are %s", key,
        Arrays.toString(Provider.values()))));
    }
  }

  void checkUrlParameters(Provider provider, String url) {
    if (Provider.mysql.equals(provider)) {
      checkRequiredParameter(url, "useUnicode=true");
      checkRequiredParameter(url, "characterEncoding=utf8");
      checkRecommendedParameter(url, "rewriteBatchedStatements=true");
      checkRecommendedParameter(url, "useConfigs=maxPerformance");
    }
  }

  private void checkRequiredParameter(String url, String val) {
    if (!url.contains(val)) {
      throw new MessageException(String.format("JDBC URL must have the property '%s'", val));
    }
  }

  private void checkRecommendedParameter(String url, String val) {
    if (!url.contains(val)) {
      LoggerFactory.getLogger(getClass()).warn(String.format("JDBC URL is recommended to have the property '%s'", val));
    }
  }
}
