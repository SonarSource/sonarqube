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
package org.sonar.core.util;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidsTest {

  @Test
  public void create_unique() {
    Set<String> all = new HashSet<>();
    for (int i = 0; i < 50; i++) {
      String uuid = Uuids.create();
      assertThat(uuid).isNotEmpty();
      all.add(uuid);
    }
    assertThat(all).hasSize(50);
  }

  @Test
  public void constructor_is_private() {
    TestUtils.hasOnlyPrivateConstructors(Uuids.class);
  }
}
