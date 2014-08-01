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
package org.sonar.batch.duplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.sensor.duplication.DuplicationGroup;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DuplicationCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  Caches caches;

  @Before
  public void start() throws Exception {
    caches = CachesTest.createCacheOnTemp(temp);
    caches.start();
  }

  @After
  public void stop() {
    caches.stop();
  }

  @Test
  public void should_add_clone_groups() throws Exception {
    DuplicationCache cache = new DuplicationCache(caches);

    DuplicationGroup group1 = new DuplicationGroup(new DuplicationGroup.Block("foo", 1, 2))
      .addDuplicate(new DuplicationGroup.Block("foo", 1, 2))
      .addDuplicate(new DuplicationGroup.Block("foo2", 12, 22))
      .addDuplicate(new DuplicationGroup.Block("foo3", 13, 23));

    DuplicationGroup group2 = new DuplicationGroup(new DuplicationGroup.Block("2foo", 1, 2))
      .addDuplicate(new DuplicationGroup.Block("2foo", 1, 2))
      .addDuplicate(new DuplicationGroup.Block("2foo2", 12, 22))
      .addDuplicate(new DuplicationGroup.Block("2foo3", 13, 23));

    assertThat(cache.entries()).hasSize(0);

    cache.put("foo", new ArrayList<DuplicationGroup>(Arrays.asList(group1, group2)));

    assertThat(cache.entries()).hasSize(1);

    List<DuplicationGroup> entry = cache.byComponent("foo");
    assertThat(entry.get(0).originBlock().resourceKey()).isEqualTo("foo");

  }

}
