/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.posttask;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProjectImplTest {
  private static final String SOME_UUID = "some uuid";
  private static final String SOME_KEY = "some key";
  private static final String SOME_NAME = "some name";


  @Test
  public void constructor_throws_NPE_if_uuid_is_null() {
    assertThatThrownBy(() -> new ProjectImpl(null, SOME_KEY, SOME_NAME))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("uuid can not be null");
  }

  @Test
  public void constructor_throws_NPE_if_key_is_null() {
    assertThatThrownBy(() ->  new ProjectImpl(SOME_UUID, null, SOME_NAME))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("key can not be null");
  }

  @Test
  public void constructor_throws_NPE_if_name_is_null() {
    assertThatThrownBy(() ->  new ProjectImpl(SOME_UUID, SOME_KEY, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("name can not be null");
  }

  @Test
  public void verify_getters() {
    ProjectImpl underTest = new ProjectImpl(SOME_UUID, SOME_KEY, SOME_NAME);

    assertThat(underTest.getUuid()).isEqualTo(SOME_UUID);
    assertThat(underTest.getKey()).isEqualTo(SOME_KEY);
    assertThat(underTest.getName()).isEqualTo(SOME_NAME);
  }

  @Test
  public void verify_toString() {
    assertThat(new ProjectImpl(SOME_UUID, SOME_KEY, SOME_NAME))
      .hasToString("ProjectImpl{uuid='some uuid', key='some key', name='some name'}");

  }
}
