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
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;
import org.sonar.batch.index.CachesTest;

import java.util.Iterator;

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

    Measure m = new Measure(CoreMetrics.NCLOC, 1.0);
    cache.put(p, m);

    assertThat(cache.contains(p, m)).isTrue();
    assertThat(cache.entries()).hasSize(1);
    Iterator<Entry<Measure>> iterator = cache.entries().iterator();
    iterator.hasNext();
    Entry<Measure> next = iterator.next();
    assertThat(next.value()).isEqualTo(m);
    assertThat(next.key()[0]).isEqualTo("struts");

    assertThat(cache.byResource(p)).hasSize(1);
    assertThat(cache.byResource(p).iterator().next()).isEqualTo(m);

    Measure mRule = RuleMeasure.createForPriority(CoreMetrics.CRITICAL_VIOLATIONS, RulePriority.BLOCKER, 1.0);
    cache.put(p, mRule);

    assertThat(cache.entries()).hasSize(2);

    assertThat(cache.byResource(p)).hasSize(2);
  }

  @Test
  public void should_add_measure_with_same_metric() throws Exception {
    MeasureCache cache = new MeasureCache(caches);
    Project p = new Project("struts");

    assertThat(cache.entries()).hasSize(0);
    assertThat(cache.byResource(p)).hasSize(0);

    Measure m1 = new Measure(CoreMetrics.NCLOC, 1.0);
    Measure m2 = new Measure(CoreMetrics.NCLOC, 1.0).setCharacteristic(new DefaultCharacteristic().setKey("charac"));
    Measure m3 = new Measure(CoreMetrics.NCLOC, 1.0).setPersonId(2);
    Measure m4 = new RuleMeasure(CoreMetrics.NCLOC, RuleKey.of("repo", "rule"), RulePriority.BLOCKER, null);
    cache.put(p, m1);
    cache.put(p, m2);
    cache.put(p, m3);
    cache.put(p, m4);

    assertThat(cache.entries()).hasSize(4);

    assertThat(cache.byResource(p)).hasSize(4);
  }

  @Test
  public void should_get_measures() throws Exception {
    MeasureCache cache = new MeasureCache(caches);
    Project p = new Project("struts");
    Resource dir = new Directory("foo/bar").setEffectiveKey("struts:foo/bar");
    Resource file1 = new File("foo/bar/File1.txt").setEffectiveKey("struts:foo/bar/File1.txt");
    Resource file2 = new File("foo/bar/File2.txt").setEffectiveKey("struts:foo/bar/File2.txt");

    assertThat(cache.entries()).hasSize(0);

    assertThat(cache.byResource(p)).hasSize(0);
    assertThat(cache.byResource(dir)).hasSize(0);

    Measure mFile1 = new Measure(CoreMetrics.NCLOC, 1.0);
    cache.put(file1, mFile1);
    Measure mFile2 = new Measure(CoreMetrics.NCLOC, 3.0);
    cache.put(file2, mFile2);

    assertThat(cache.entries()).hasSize(2);
    assertThat(cache.byResource(p)).hasSize(0);
    assertThat(cache.byResource(dir)).hasSize(0);

    Measure mDir = new Measure(CoreMetrics.NCLOC, 4.0);
    cache.put(dir, mDir);

    assertThat(cache.entries()).hasSize(3);
    assertThat(cache.byResource(p)).hasSize(0);
    assertThat(cache.byResource(dir)).hasSize(1);
    assertThat(cache.byResource(dir).iterator().next()).isEqualTo(mDir);

    Measure mProj = new Measure(CoreMetrics.NCLOC, 4.0);
    cache.put(p, mProj);

    assertThat(cache.entries()).hasSize(4);
    assertThat(cache.byResource(p)).hasSize(1);
    assertThat(cache.byResource(p).iterator().next()).isEqualTo(mProj);
    assertThat(cache.byResource(dir)).hasSize(1);
    assertThat(cache.byResource(dir).iterator().next()).isEqualTo(mDir);
  }
}
