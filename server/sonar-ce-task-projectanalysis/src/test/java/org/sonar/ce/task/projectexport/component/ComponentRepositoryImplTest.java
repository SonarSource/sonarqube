/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.ce.task.projectexport.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentRepositoryImplTest {
  private static final int SOME_REF = 121;
  private static final String SOME_UUID = "uuid";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ComponentRepositoryImpl underTest = new ComponentRepositoryImpl();

  @Test
  public void register_throws_NPE_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can not be null");

    underTest.register(SOME_REF, null, true);
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_different_refs() {
    underTest.register(SOME_REF, SOME_UUID, true);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Uuid '" + SOME_UUID + "' already registered under ref '" + SOME_REF + "' in repository");

    underTest.register(946512, SOME_UUID, true);
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_as_file() {
    underTest.register(SOME_REF, SOME_UUID, true);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Uuid '" + SOME_UUID + "' already registered but as a File");

    underTest.register(SOME_REF, SOME_UUID, false);
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_as_not_file() {
    underTest.register(SOME_REF, SOME_UUID, false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Uuid '" + SOME_UUID + "' already registered but not as a File");

    underTest.register(SOME_REF, SOME_UUID, true);
  }

  @Test
  public void getRef_throws_NPE_if_uuid_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("uuid can not be null");

    underTest.getRef(null);
  }

  @Test
  public void getRef_throws_ISE_if_uuid_not_in_repository() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("No reference registered in the repository for uuid '" + SOME_UUID + "'");

    underTest.getRef(SOME_UUID);
  }

  @Test
  public void getRef_returns_ref_added_with_register_when_file() {
    underTest.register(SOME_REF, SOME_UUID, true);

    assertThat(underTest.getRef(SOME_UUID)).isEqualTo(SOME_REF);
  }

  @Test
  public void getRef_returns_ref_added_with_register_when_not_file() {
    underTest.register(SOME_REF, SOME_UUID, false);

    assertThat(underTest.getRef(SOME_UUID)).isEqualTo(SOME_REF);
  }

  @Test
  public void getFileUuids_returns_empty_when_repository_is_empty() {
    assertThat(underTest.getFileUuids()).isEmpty();
  }

  @Test
  public void getFileUuids_returns_uuids_of_only_components_added_with_file_flag_is_true() {
    underTest.register(SOME_REF, "file id 1", true);
    underTest.register(546, SOME_UUID, false);
    underTest.register(987, "file id 2", true);
    underTest.register(123, "not file id", false);

    assertThat(underTest.getFileUuids()).containsOnly("file id 1", "file id 2");
  }
}
