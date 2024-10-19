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
package org.sonar.server.platform.serverid;

import org.apache.commons.codec.digest.DigestUtils;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;

import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

@ServerSide
@ComputeEngineSide
public class ServerIdChecksum {

  private final Configuration config;
  private final JdbcUrlSanitizer jdbcUrlSanitizer;

  public ServerIdChecksum(Configuration config, JdbcUrlSanitizer jdbcUrlSanitizer) {
    this.config = config;
    this.jdbcUrlSanitizer = jdbcUrlSanitizer;
  }

  public String computeFor(String serverId) {
    String jdbcUrl = config.get(JDBC_URL.getKey()).orElseThrow(() -> new IllegalStateException("Missing JDBC URL"));
    return DigestUtils.sha256Hex(serverId + "|" + jdbcUrlSanitizer.sanitize(jdbcUrl));
  }

}
