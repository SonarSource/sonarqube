/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactory;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PopulateNotificationUuidAndCreatedAtTest {

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(PopulateNotificationUuidAndCreatedAtTest.class, "schema.sql");

  private UuidFactory uuidFactory = UuidFactoryFast.getInstance();
  private System2 system2 = mock(System2.class);
  private DataChange underTest = new PopulateNotificationUuidAndCreatedAt(db.database(), uuidFactory, system2);

  @Before
  public void before() {
    // exactly one week before now + 1ms, so that ID are exactly equals to timestamp in the tests
    when(system2.now()).thenReturn((1000 * 60 * 60 * 24 * 7) + 1L);
  }

  @Test
  public void populate_uuids_and_created_at() throws IOException, SQLException {
    insertNotification(1L, "data1");
    insertNotification(2L, "data2");
    insertNotification(3L, "data3");

    underTest.execute();

    verifyUuidsAreNotNull();
    verifyCreatedAt();
  }

  @Test
  public void migration_is_reentrant() throws IOException, SQLException {
    insertNotification(1L, "data1");
    insertNotification(2L, "data2");
    insertNotification(3L, "data3");

    underTest.execute();
    // re-entrant
    underTest.execute();

    verifyUuidsAreNotNull();
    verifyCreatedAt();
  }

  private void verifyUuidsAreNotNull() {
    assertThat(db.select("select uuid from notifications")
      .stream()
      .map(row -> row.get("UUID"))
      .filter(Objects::isNull)
      .collect(Collectors.toList())).isEmpty();
  }

  private void verifyCreatedAt() {
    assertThat(db.select("select id, created_at from notifications")
      .stream()
      .filter(row -> !row.get("CREATED_AT").equals(row.get("ID")))
      .collect(Collectors.toList())).isEmpty();

  }

  private void insertNotification(Long id, String data) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
    objectOutputStream.writeObject(data);
    objectOutputStream.close();
    byte[] byteArray = byteArrayOutputStream.toByteArray();

    db.executeInsert("notifications",
      "id", id,
      "data", byteArray);
  }

}
