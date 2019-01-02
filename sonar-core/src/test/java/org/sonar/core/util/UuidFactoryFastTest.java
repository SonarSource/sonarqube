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
package org.sonar.core.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidFactoryFastTest {
  UuidFactory underTest = UuidFactoryFast.getInstance();

  @Test
  public void create_different_uuids() {
    // this test is not enough to ensure that generated strings are unique,
    // but it still does a simple and stupid verification
    assertThat(underTest.create()).isNotEqualTo(underTest.create());
  }

  @Test
  public void test_format_of_uuid() {
    String uuid = underTest.create();

    assertThat(uuid.length()).isGreaterThan(10).isLessThan(40);

    // URL-safe: only letters, digits, dash and underscore.
    assertThat(uuid).matches("^[\\w\\-_]+$");
  }
}
