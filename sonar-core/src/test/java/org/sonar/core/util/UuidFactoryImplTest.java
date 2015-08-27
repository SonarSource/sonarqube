/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.util;

import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UuidFactoryImplTest {

  UuidFactory underTest = UuidFactoryImpl.INSTANCE;

  @Test
  public void create_unique() {
    Set<String> all = Sets.newHashSet();
    for (int i = 0; i < 50; i++) {
      String uuid = underTest.create();
      assertThat(uuid).isNotEmpty();
      // not in the specification, but still to be sure that there's an upper-bound.
      assertThat(uuid.length()).isLessThanOrEqualTo(20);

      // URL-safe
      assertThat(uuid).doesNotContain("/").doesNotContain("+").doesNotContain("=").doesNotContain("&");

      all.add(uuid);
    }
    assertThat(all).hasSize(50);
  }
}
