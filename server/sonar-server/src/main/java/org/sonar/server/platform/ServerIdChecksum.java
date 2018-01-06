/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.KeyValueFormat;

public class ServerIdChecksum {

  private static final String SQLSERVER_PREFIX = "jdbc:sqlserver://";

  private ServerIdChecksum() {
    // only static methods
  }

  public static String of(String serverId, String jdbcUrl) {
    return DigestUtils.sha256Hex(serverId + "|" + sanitizeJdbcUrl(jdbcUrl));
  }

  @VisibleForTesting
  static String sanitizeJdbcUrl(String jdbcUrl) {
    String result;
    if (jdbcUrl.startsWith(SQLSERVER_PREFIX)) {
      result = sanitizeSqlServerUrl(jdbcUrl);
    } else {
      // remove query parameters, they don't aim to reference the schema
      result = StringUtils.substringBefore(jdbcUrl, "?");
    }
    return StringUtils.lowerCase(result, Locale.ENGLISH);
  }

  /**
   * Deal with this strange URL format:
   * https://docs.microsoft.com/en-us/sql/connect/jdbc/building-the-connection-url
   * https://docs.microsoft.com/en-us/sql/connect/jdbc/setting-the-connection-properties
   */
  private static String sanitizeSqlServerUrl(String jdbcUrl) {
    StringBuilder result = new StringBuilder();
    result.append(SQLSERVER_PREFIX);

    String host;
    if (jdbcUrl.contains(";")) {
      host = StringUtils.substringBetween(jdbcUrl, SQLSERVER_PREFIX, ";");
    } else {
      host = StringUtils.substringAfter(jdbcUrl, SQLSERVER_PREFIX);
    }

    String queryString = StringUtils.substringAfter(jdbcUrl, ";");
    Map<String, String> parameters = KeyValueFormat.parse(queryString);
    Optional<String> server = firstValue(parameters, "serverName", "servername", "server");
    if (server.isPresent()) {
      result.append(server.get());
    } else {
      result.append(StringUtils.substringBefore(host, ":"));
    }

    Optional<String> port = firstValue(parameters, "portNumber", "port");
    if (port.isPresent()) {
      result.append(':').append(port.get());
    } else if (host.contains(":")) {
      result.append(':').append(StringUtils.substringAfter(host, ":"));
    }

    Optional<String> database = firstValue(parameters, "databaseName", "database");
    database.ifPresent(s -> result.append('/').append(s));
    return result.toString();
  }

  private static Optional<String> firstValue(Map<String, String> map, String... keys) {
    return Arrays.stream(keys)
      .map(map::get)
      .filter(Objects::nonNull)
      .findFirst();
  }
}
