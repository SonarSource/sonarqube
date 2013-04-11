/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.index;

import org.junit.Test;
import org.sonar.api.database.model.Snapshot;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class SnapshotCacheTest {

  private Snapshot snapshot = mock(Snapshot.class);

  @Test
  public void should_cache_snapshots() throws Exception {
    SnapshotCache cache = new SnapshotCache();
    String componentKey = "org.apache.struts:struts-core";
    cache.put(componentKey, snapshot);
    assertThat(cache.get(componentKey)).isSameAs(snapshot);
    assertThat(cache.get("other")).isNull();
  }

  @Test
  public void should_fail_if_put_twice() throws Exception {
    SnapshotCache cache = new SnapshotCache();
    String componentKey = "org.apache.struts:struts-core";
    cache.put(componentKey, snapshot);
    try {
      cache.put(componentKey, mock(Snapshot.class));
      fail();
    } catch (IllegalStateException e) {
      // success
    }
  }
}
