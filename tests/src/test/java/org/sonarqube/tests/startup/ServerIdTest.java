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
package org.sonarqube.tests.startup;

import com.sonar.orchestrator.Orchestrator;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Condition;
import org.junit.After;
import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;

public class ServerIdTest {
  private Orchestrator orchestrator;

  @After
  public void tearDown() {
    if (orchestrator != null) {
      orchestrator.stop();
    }
  }

  @Test
  public void server_id_various_behavior_at_startup() throws SQLException {
    orchestrator = Orchestrator.builderEnv().build();
    orchestrator.start();

    // 1 - check serverId generated on blank DB
    String step1ServerId = readServerId();
    assertThat(step1ServerId)
      .describedAs("serverId should have format with databaseId and datasetId")
      .hasSize(29)
      .has(new Condition<String>() {
        @Override
        public boolean matches(String value) {
          return value.charAt(8) == '-';
        }
      });
    String databaseId = step1ServerId.substring(0, 8);

    // 2 - check behavior for simulated serverId of SQ prior to 6.7 (no checksum and serverId without databaseId)
    String oldServerId = randomAlphanumeric(20);
    writeServerId(oldServerId);
    deleteCheckSum();

    orchestrator.restartServer();

    String step2ServerId = readServerId();
    assertThat(step2ServerId)
      .describedAs("serverId should have new format and be generated from existing ServerId")
      .isEqualTo(databaseId + "-" + oldServerId);

    // 3 - check behavior in simulated copy of prod DB onto staging DB
    // (by changing databaseId in serverId, checksum should also be different but changing serverId alone should be enough)
    writeServerId(randomAlphanumeric(8) + "-" + oldServerId);

    orchestrator.restartServer();

    String step3ServerId = readServerId();
    assertThat(step3ServerId)
      .describedAs("Since datasetId hasn't changed, the new serverId should be the same as before")
      .isEqualTo(step2ServerId);
  }

  private void deleteCheckSum() throws SQLException {
    try (Connection connection = orchestrator.getDatabase().openConnection();
      Statement statement = connection.createStatement()) {
      statement.execute("delete from internal_properties where kee = 'server.idChecksum'");
    }
  }

  private String readServerId() {
    List<Map<String, String>> rows = orchestrator.getDatabase().executeSql("select p.text_value from properties p" +
      " where" +
      " p.prop_key='sonar.core.id'" +
      " and p.resource_id is null" +
      " and p.user_id is null");
    if (rows.size() == 1) {
      return rows.iterator().next().get("TEXT_VALUE");
    }
    throw new IllegalArgumentException("Failed to retrieve serverId from DB");
  }

  private void writeServerId(String newServerId) throws SQLException {
    try (Connection connection = orchestrator.getDatabase().openConnection();
      Statement statement = connection.createStatement()) {
      statement.execute("update properties set text_value = '" + newServerId + "'" +
        " where" +
        " prop_key='sonar.core.id'" +
        " and resource_id is null" +
        " and user_id is null");
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
    }
  }
}
