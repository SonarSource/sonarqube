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
package org.sonar.db.ce;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class CeTaskCharacteristicDaoTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTaskCharacteristicDao underTest = new CeTaskCharacteristicDao();

  @Test
  public void selectByTaskUuids() {
    insert("key1", "value1", "uuid1", "task1");
    insert("key2", "value2", "uuid2", "task2");

    dbTester.getSession().commit();

    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), asList("task1", "task2")))
      .extracting(CeTaskCharacteristicDto::getTaskUuid, CeTaskCharacteristicDto::getUuid, CeTaskCharacteristicDto::getKey, CeTaskCharacteristicDto::getValue)
      .containsOnly(
        tuple("task1", "uuid1", "key1", "value1"),
        tuple("task2", "uuid2", "key2", "value2"));
    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), singletonList("unknown"))).isEmpty();
  }


  @Test
  public void deleteByTaskUuids() {
    insert("key1", "value1", "uuid1", "task1");
    insert("key2", "value2", "uuid2", "task2");
    insert("key3", "value3", "uuid3", "task3");

    underTest.deleteByTaskUuids(dbTester.getSession(), ImmutableSet.of("task1", "task3"));
    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), singletonList("task1"))).hasSize(0);
    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), singletonList("task2"))).hasSize(1);
    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), singletonList("task3"))).hasSize(0);
  }

  @Test
  public void deleteByTaskUuids_does_nothing_if_uuid_does_not_exist() {
    insert("key1", "value1", "uuid1", "task1");

    // must not fail
    underTest.deleteByTaskUuids(dbTester.getSession(), singleton("task2"));

    assertThat(underTest.selectByTaskUuids(dbTester.getSession(), singletonList("task1"))).hasSize(1);
  }

  private void insert(String key, String value, String uuid, String taskUuid) {
    CeTaskCharacteristicDto dto1 = new CeTaskCharacteristicDto()
      .setKey(key)
      .setValue(value)
      .setUuid(uuid)
      .setTaskUuid(taskUuid);
    underTest.insert(dbTester.getSession(), singleton(dto1));
  }
}
