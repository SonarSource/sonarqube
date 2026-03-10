/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.core.platform.ServerId;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.platform.NodeInformation;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.CoreProperties.SERVER_ID;
import static org.sonar.core.platform.ServerId.DATABASE_ID_LENGTH;
import static org.sonar.core.platform.ServerId.NOT_UUID_DATASET_ID_LENGTH;
import static org.sonar.core.platform.ServerId.UUID_DATASET_ID_LENGTH;

class ServerIdManagerTest {

  @RegisterExtension
  public DbTester db = DbTester.create();

  private final ServerIdChecksum serverIdChecksum = mock(ServerIdChecksum.class);
  private final ServerIdFactory serverIdFactory = mock(ServerIdFactory.class);
  private final SonarRuntime runtime = mock(SonarRuntime.class);
  private final NodeInformation nodeInformation = mock(NodeInformation.class);

  private final ServerIdManager underTest = new ServerIdManager(serverIdChecksum, serverIdFactory, db.getDbClient(), runtime, nodeInformation);

  @Test
  void start_fails_when_deprecated_date_format_serverId_in_database() {
    // Given a deprecated server ID (date format) in the database
    String deprecatedServerId = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    db.properties().insertProperty(new PropertyDto().setKey(SERVER_ID).setValue(deprecatedServerId), null, null, null, null);

    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);
    when(nodeInformation.isStartupLeader()).thenReturn(true);

    // When/Then starting should fail
    assertThatThrownBy(underTest::start)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("serverId does not have a supported length");
  }

  @Test
  void start_fails_when_no_database_id_format_serverId_in_database_uuid_length() {
    // Given a server ID without database ID (UUID length) in the database
    String noDatabaseIdServerId = secure().nextAlphabetic(UUID_DATASET_ID_LENGTH);
    db.properties().insertProperty(new PropertyDto().setKey(SERVER_ID).setValue(noDatabaseIdServerId), null, null, null, null);

    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);
    when(nodeInformation.isStartupLeader()).thenReturn(true);

    // When/Then starting should fail
    assertThatThrownBy(underTest::start)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("serverId does not have a supported length");
  }

  @Test
  void start_fails_when_no_database_id_format_serverId_in_database_non_uuid_length() {
    // Given a server ID without database ID (non-UUID length) in the database
    String noDatabaseIdServerId = secure().nextAlphabetic(NOT_UUID_DATASET_ID_LENGTH);
    db.properties().insertProperty(new PropertyDto().setKey(SERVER_ID).setValue(noDatabaseIdServerId), null, null, null, null);

    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);
    when(nodeInformation.isStartupLeader()).thenReturn(true);

    // When/Then starting should fail
    assertThatThrownBy(underTest::start)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("serverId does not have a supported length");
  }

  @Test
  void start_succeeds_with_valid_serverId_format() {
    // Given a valid server ID with database ID in the database
    String validServerId = ServerId.of(
      secure().nextAlphabetic(DATABASE_ID_LENGTH),
      secure().nextAlphabetic(UUID_DATASET_ID_LENGTH)).toString();

    db.properties().insertProperty(new PropertyDto().setKey(SERVER_ID).setValue(validServerId), null, null, null, null);

    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);
    when(nodeInformation.isStartupLeader()).thenReturn(true);
    when(serverIdChecksum.computeFor(validServerId)).thenReturn("checksum");

    // When/Then starting should succeed without throwing any exception
    assertThatCode(underTest::start).doesNotThrowAnyException();
  }
}
