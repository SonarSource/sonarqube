/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class BatchComponentCacheTest {
  @Test
  public void should_cache_resource() {
    BatchComponentCache cache = new BatchComponentCache();
    String componentKey = "struts:src/org/struts/Action.java";
    Resource resource = File.create("org/struts/Action.java").setEffectiveKey(componentKey);
    cache.add(resource, null);

    assertThat(cache.get(componentKey).resource()).isSameAs(resource);
    assertThat(cache.get("other")).isNull();
  }

  @Test
  public void should_fail_if_missing_component_key() {
    BatchComponentCache cache = new BatchComponentCache();
    Resource resource = File.create("org/struts/Action.java").setEffectiveKey(null);
    try {
      cache.add(resource, null);
      fail();
    } catch (IllegalStateException e) {
      // success
      assertThat(e).hasMessage("Missing resource effective key");
    }
  }
}
