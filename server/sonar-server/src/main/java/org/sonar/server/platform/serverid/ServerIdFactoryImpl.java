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
package org.sonar.server.platform.serverid;

import com.google.common.annotations.VisibleForTesting;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.CRC32;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.Configuration;
import org.sonar.core.platform.ServerId;
import org.sonar.core.util.UuidFactory;

import static org.sonar.process.ProcessProperties.Property.JDBC_URL;

public class ServerIdFactoryImpl implements ServerIdFactory {

  private final Configuration config;
  private final UuidFactory uuidFactory;
  private final JdbcUrlSanitizer jdbcUrlSanitizer;

  public ServerIdFactoryImpl(Configuration config, UuidFactory uuidFactory, JdbcUrlSanitizer jdbcUrlSanitizer) {
    this.config = config;
    this.uuidFactory = uuidFactory;
    this.jdbcUrlSanitizer = jdbcUrlSanitizer;
  }

  @Override
  public ServerId create() {
    return ServerId.of(computeDatabaseId(), uuidFactory.create());
  }

  @Override
  public ServerId create(ServerId currentServerId) {
    return ServerId.of(computeDatabaseId(), currentServerId.getDatasetId());
  }

  private String computeDatabaseId() {
    String jdbcUrl = config.get(JDBC_URL.getKey()).orElseThrow(() -> new IllegalStateException("Missing JDBC URL"));
    return crc32Hex(jdbcUrlSanitizer.sanitize(jdbcUrl));
  }

  @VisibleForTesting
  static String crc32Hex(String str) {
    CRC32 crc32 = new CRC32();
    crc32.update(str.getBytes(StandardCharsets.UTF_8));
    long hash = crc32.getValue();
    String s = Long.toHexString(hash).toUpperCase(Locale.ENGLISH);
    return StringUtils.leftPad(s, ServerId.DATABASE_ID_LENGTH, "0");
  }

}
