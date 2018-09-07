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
package org.sonar.db.ce;

import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CeTaskMessageDaoTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTaskMessageDao underTest = new CeTaskMessageDao();

  @Test
  public void insert() {
    underTest.insert(dbTester.getSession(), new CeTaskMessageDto()
      .setUuid("uuid_1")
      .setTaskUuid("task_uuid_1")
      .setMessage("message_1")
      .setCreatedAt(1_222_333L));
    dbTester.getSession().commit();

    assertThat(dbTester.select("select uuid as \"UUID\", task_uuid as \"TASK_UUID\", message as \"MESSAGE\", created_at as \"CREATED_AT\" from ce_task_message"))
      .hasSize(1)
      .extracting(t -> t.get("UUID"), t -> t.get("TASK_UUID"), t -> t.get("MESSAGE"), t -> t.get("CREATED_AT"))
      .containsOnly(Tuple.tuple("uuid_1", "task_uuid_1", "message_1", 1_222_333L));

  }
}
