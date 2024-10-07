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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Test;
import org.junit.runner.RunWith;
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

@RunWith(DataProviderRunner.class)
public class ServerIdFactoryImplTest {
  private static final ServerId A_SERVERID = ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(UUID_DATASET_ID_LENGTH));

  private MapSettings settings = new MapSettings();
  private Configuration config = settings.asConfig();
  private ServerIdGenerator serverIdGenerator = spy(new ServerIdGenerator());
  private JdbcUrlSanitizer jdbcUrlSanitizer = mock(JdbcUrlSanitizer.class);
  private ServerIdFactoryImpl underTest = new ServerIdFactoryImpl(config, serverIdGenerator, jdbcUrlSanitizer);

  @Test
  public void create_from_scratch_fails_with_ISE_if_JDBC_property_not_set() {
    expectMissingJdbcUrlISE(() ->  underTest.create());
  }

  @Test
  public void create_from_scratch_creates_ServerId_from_JDBC_URL_and_new_uuid() {
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
  public void create_from_ServerId_fails_with_ISE_if_JDBC_property_not_set() {
    expectMissingJdbcUrlISE(() -> underTest.create(A_SERVERID));
  }

  @Test
  @UseDataProvider("anyFormatServerId")
  public void create_from_ServerId_creates_ServerId_from_JDBC_URL_and_serverId_datasetId(ServerId currentServerId) {
    String jdbcUrl = "jdbc";
    String sanitizedJdbcUrl = "sanitized_jdbc";
    settings.setProperty(JDBC_URL.getKey(), jdbcUrl);
    when(serverIdGenerator.generate()).thenThrow(new IllegalStateException("generate should not be called"));
    when(jdbcUrlSanitizer.sanitize(jdbcUrl)).thenReturn(sanitizedJdbcUrl);

    ServerId serverId = underTest.create(currentServerId);

    assertThat(serverId.getDatabaseId()).contains(crc32Hex(sanitizedJdbcUrl));
    assertThat(serverId.getDatasetId()).isEqualTo(currentServerId.getDatasetId());
  }

  @DataProvider
  public static Object[][] anyFormatServerId() {
    return new Object[][] {
      {ServerId.parse(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))},
      {ServerId.parse(secure().nextAlphabetic(NOT_UUID_DATASET_ID_LENGTH))},
      {ServerId.parse(secure().nextAlphabetic(UUID_DATASET_ID_LENGTH))},
      {ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(NOT_UUID_DATASET_ID_LENGTH))},
      {ServerId.of(secure().nextAlphabetic(DATABASE_ID_LENGTH), secure().nextAlphabetic(UUID_DATASET_ID_LENGTH))}
    };
  }

  private void expectMissingJdbcUrlISE(ThrowingCallable callback) {
    assertThatThrownBy(callback)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Missing JDBC URL");
  }
}
