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
package org.sonar.batch.scan.measure;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.RuleMeasure;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import static org.fest.assertions.Assertions.assertThat;

public class MeasureCacheTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

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
  public void should_add_measure() throws Exception {
    MeasureCache cache = new MeasureCache(caches);
    Project p = new Project("struts");

    assertThat(cache.entries()).hasSize(0);

    assertThat(cache.byResource(p)).hasSize(0);
    assertThat(cache.byMetric(p, "ncloc")).hasSize(0);

    Measure m = new Measure(CoreMetrics.NCLOC, 1.0);
    cache.put(p, m);

    assertThat(cache.entries()).hasSize(1);

    assertThat(cache.byMetric(p, "ncloc")).hasSize(1);
    assertThat(cache.byMetric(p, "ncloc").iterator().next()).isEqualTo(m);
    assertThat(cache.byResource(p)).hasSize(1);
    assertThat(cache.byResource(p).iterator().next()).isEqualTo(m);

    Measure mRule = RuleMeasure.createForPriority(CoreMetrics.CRITICAL_VIOLATIONS, RulePriority.BLOCKER, 1.0);
    cache.put(p, mRule);

    assertThat(cache.entries()).hasSize(2);

    assertThat(cache.byResource(p)).hasSize(2);
    assertThat(cache.byMetric(p, "ncloc")).hasSize(1);
  }
}
