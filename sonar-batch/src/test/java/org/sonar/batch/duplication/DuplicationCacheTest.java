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
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.duplication.internal.DefaultDuplication;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import static org.assertj.core.api.Assertions.assertThat;

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

    DefaultDuplication group1 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("foo", 1, 2));
    group1.duplicates().add(new Duplication.Block("foo", 1, 2));
    group1.duplicates().add(new Duplication.Block("foo2", 12, 22));
    group1.duplicates().add(new Duplication.Block("foo3", 13, 23));

    DefaultDuplication group2 = new DefaultDuplication()
      .setOriginBlock(new Duplication.Block("2foo", 1, 2));
    group2.duplicates().add(new Duplication.Block("2foo", 1, 2));
    group2.duplicates().add(new Duplication.Block("2foo2", 12, 22));
    group2.duplicates().add(new Duplication.Block("2foo3", 13, 23));

    assertThat(cache.componentKeys()).hasSize(0);

    cache.put("foo", group1);
    cache.put("foo", group2);

    assertThat(cache.componentKeys()).hasSize(1);
    assertThat(cache.byComponent("foo")).hasSize(2);

    Iterable<DefaultDuplication> entry = cache.byComponent("foo");
    assertThat(entry.iterator().next().originBlock().resourceKey()).isEqualTo("foo");

  }

}
