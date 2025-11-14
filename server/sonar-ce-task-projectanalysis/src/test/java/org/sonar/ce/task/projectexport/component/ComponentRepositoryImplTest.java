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
package org.sonar.ce.task.projectexport.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ComponentRepositoryImplTest {
  private static final int SOME_REF = 121;
  private static final String SOME_UUID = "uuid";


  private ComponentRepositoryImpl underTest = new ComponentRepositoryImpl();

  @Test
  public void register_throws_NPE_if_uuid_is_null() {
    assertThatThrownBy(() -> underTest.register(SOME_REF, null, true))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid can not be null");
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_different_refs() {
    underTest.register(SOME_REF, SOME_UUID, true);

    assertThatThrownBy(() -> underTest.register(946512, SOME_UUID, true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Uuid '" + SOME_UUID + "' already registered under ref '" + SOME_REF + "' in repository");
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_as_file() {
    underTest.register(SOME_REF, SOME_UUID, true);

    assertThatThrownBy(() -> underTest.register(SOME_REF, SOME_UUID, false))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Uuid '" + SOME_UUID + "' already registered but as a File");
  }

  @Test
  public void register_throws_IAE_same_uuid_added_with_as_not_file() {
    underTest.register(SOME_REF, SOME_UUID, false);

    assertThatThrownBy(() -> underTest.register(SOME_REF, SOME_UUID, true))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Uuid '" + SOME_UUID + "' already registered but not as a File");
  }

  @Test
  public void getRef_throws_NPE_if_uuid_is_null() {
    assertThatThrownBy(() -> underTest.getRef(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid can not be null");
  }

  @Test
  public void getRef_throws_ISE_if_uuid_not_in_repository() {
    assertThatThrownBy(() -> underTest.getRef(SOME_UUID))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("No reference registered in the repository for uuid '" + SOME_UUID + "'");
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
