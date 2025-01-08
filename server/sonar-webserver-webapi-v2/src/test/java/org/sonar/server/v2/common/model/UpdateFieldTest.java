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
package org.sonar.server.v2.common.model;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UpdateFieldTest {

  @Test
  public void orElse_whenNoValueDefined_useElseValue() {
    UpdateField<String> updateField = UpdateField.undefined();
    assertThat(updateField.orElse("foo")).isEqualTo("foo");
  }

  @Test
  public void orElse_whenNoNullValueDefined_useValue() {
    UpdateField<String> updateField = UpdateField.withValue("bar");
    assertThat(updateField.orElse("foo")).isEqualTo("bar");
  }

  @Test
  public void orElse_whenNullValueDefined_useValue() {
    UpdateField<String> updateField = UpdateField.withValue(null);
    assertThat(updateField.orElse("foo")).isNull();
  }
}
