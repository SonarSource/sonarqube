/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.platform.ServerId;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.sonar.core.platform.ServerId.DATABASE_ID_LENGTH;
import static org.sonar.core.platform.ServerId.NOT_UUID_DATASET_ID_LENGTH;
import static org.sonar.core.platform.ServerId.UUID_DATASET_ID_LENGTH;
import static org.sonar.process.ProcessProperties.Property.JDBC_URL;
import static org.sonar.server.platform.serverid.ServerIdFactoryImpl.crc32Hex;

class ServerIdFactoryImplTest {
  private static final ServerId A_SERVERID = ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(UUID_DATASET_ID_LENGTH));

  private final MapSettings settings = new MapSettings();
  private final Configuration config = settings.asConfig();
  private final ServerIdGenerator serverIdGenerator = spy(new ServerIdGenerator());
  private final JdbcUrlSanitizer jdbcUrlSanitizer = mock(JdbcUrlSanitizer.class);
  private final ServerIdFactoryImpl underTest = new ServerIdFactoryImpl(config, serverIdGenerator, jdbcUrlSanitizer);

  @Test
  void create_from_scratch_fails_with_ISE_if_JDBC_property_not_set() {
    expectMissingJdbcUrlISE(underTest::create);
  }

  @Test
  void create_from_scratch_creates_ServerId_from_JDBC_URL_and_new_uuid() {
    String jdbcUrl = "jdbc";
    String sanitizedJdbcUrl = "sanitized_jdbc";

    String uuid = serverIdGenerator.generate();
    when(serverIdGenerator.generate()).thenReturn(uuid);

    settings.setProperty(JDBC_URL.getKey(), jdbcUrl);
    when(jdbcUrlSanitizer.sanitize(jdbcUrl)).thenReturn(sanitizedJdbcUrl);

    ServerId serverId = underTest.create();

    assertThat(serverId.getDatabaseId()).contains(crc32Hex(sanitizedJdbcUrl));
    assertThat(serverId.getDatasetId()).isEqualTo(uuid);
  }

  @Test
  void create_from_ServerId_fails_with_ISE_if_JDBC_property_not_set() {
    expectMissingJdbcUrlISE(() -> underTest.create(A_SERVERID));
  }

  @ParameterizedTest
  @MethodSource("anyFormatServerId")
  void create_from_ServerId_creates_ServerId_from_JDBC_URL_and_serverId_datasetId(ServerId currentServerId) {
    String jdbcUrl = "jdbc";
    String sanitizedJdbcUrl = "sanitized_jdbc";
    settings.setProperty(JDBC_URL.getKey(), jdbcUrl);
    when(serverIdGenerator.generate()).thenThrow(new IllegalStateException("generate should not be called"));
    when(jdbcUrlSanitizer.sanitize(jdbcUrl)).thenReturn(sanitizedJdbcUrl);

    ServerId serverId = underTest.create(currentServerId);

    assertThat(serverId.getDatabaseId()).contains(crc32Hex(sanitizedJdbcUrl));
    assertThat(serverId.getDatasetId()).isEqualTo(currentServerId.getDatasetId());
  }

  static Stream<ServerId> anyFormatServerId() {
    return Stream.of(
      ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(NOT_UUID_DATASET_ID_LENGTH)),
      ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(UUID_DATASET_ID_LENGTH))
    );
  }

  private void expectMissingJdbcUrlISE(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Missing JDBC URL");
  }
}
