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

package org.sonar.server.computation.component;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComputeComponentsRefCacheTest {

  @Test
  public void add_and_get_component() throws Exception {
    ComputeComponentsRefCache cache = new ComputeComponentsRefCache();
    cache.addComponent(1, new ComputeComponentsRefCache.ComputeComponent("Key", "Uuid"));

    assertThat(cache.getByRef(1)).isNotNull();
    assertThat(cache.getByRef(1).getKey()).isEqualTo("Key");
    assertThat(cache.getByRef(1).getUuid()).isEqualTo("Uuid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void fail_on_unknown_ref() throws Exception {
    new ComputeComponentsRefCache().getByRef(1);
  }

}
