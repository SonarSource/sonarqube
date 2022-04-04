/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.notifications;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.sql.Types;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.SonarException;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.sql.Types.BIGINT;
import static org.assertj.core.api.Assertions.assertThat;

public class AddUuidAndCreatedAtColumnsToNotificationTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(AddUuidAndCreatedAtColumnsToNotificationTest.class, "schema.sql");

  private DdlChange underTest = new AddUuidAndCreatedAtColumnsToNotification(db.database());

  @Before
  public void setup() {
    insertNotification(1L, "data1");
    insertNotification(2L, "data2");
    insertNotification(3L, "data3");
  }

  @Test
  public void add_uuid_and_created_at_columns_to_notification() throws SQLException {
    underTest.execute();

    db.assertColumnDefinition("notifications", "uuid", Types.VARCHAR, 40, true);
    db.assertColumnDefinition("notifications", "created_at", BIGINT, null, true);

    assertThat(db.countSql("select count(id) from notifications"))
      .isEqualTo(3);
  }

  private void insertNotification(Long id, String data) {

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(data);
      objectOutputStream.close();
      byte[] byteArray = byteArrayOutputStream.toByteArray();

      db.executeInsert("notifications",
        "id", id,
        "data", byteArray);

    } catch (IOException e) {
      throw new SonarException("Unable to write notification", e);

    } finally {
      IOUtils.closeQuietly(byteArrayOutputStream);
    }
  }
}
