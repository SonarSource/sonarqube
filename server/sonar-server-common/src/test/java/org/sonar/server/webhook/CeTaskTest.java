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
package org.sonar.server.webhook;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CeTaskTest {

  private final CeTask underTest = new CeTask("A", CeTask.Status.SUCCESS);

  @Test
  public void constructor_throws_NPE_if_id_is_null() {
    assertThatThrownBy(() -> new CeTask(null, CeTask.Status.SUCCESS))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("id can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_status_is_null() {
    assertThatThrownBy(() -> new CeTask("B", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status can't be null");
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.id()).isEqualTo("A");
    assertThat(underTest.status()).isEqualTo(CeTask.Status.SUCCESS);
  }
}
