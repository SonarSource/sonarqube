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
package org.sonar.db.ce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

public class CeTaskCharacteristicDaoTest {
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CeTaskCharacteristicDao underTest = new CeTaskCharacteristicDao();

  @Test
  public void test_insert() {
    CeTaskCharacteristicDto dto = new CeTaskCharacteristicDto();
    dto.setKey("key");
    dto.setValue("value");
    dto.setUuid("uuid");
    dto.setTaskUuid("task");
    underTest.insert(dbTester.getSession(), Collections.singletonList(dto));
    dbTester.getSession().commit();

    assertThat(underTest.getTaskCharacteristics(dbTester.getSession(), "task")).containsOnly(entry("key", "value"));
  }

  @Test
  public void test_no_result() {
    assertThat(underTest.getTaskCharacteristics(dbTester.getSession(), "task")).isEmpty();

  }
}
