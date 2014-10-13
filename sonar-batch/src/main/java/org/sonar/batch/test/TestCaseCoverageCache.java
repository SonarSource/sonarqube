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
package org.sonar.batch.test;

import com.google.common.base.Preconditions;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.test.TestCaseCoverage;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseCoverage;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;
import org.sonar.batch.scan.filesystem.InputPathCache;

import javax.annotation.CheckForNull;

/**
 * Cache of coverage per test. This cache is shared amongst all project modules.
 */
public class TestCaseCoverageCache implements BatchComponent {

  private final Cache<TestCaseCoverage> cache;

  public TestCaseCoverageCache(Caches caches, InputPathCache inputPathCache) {
    caches.registerValueCoder(DefaultTestCaseCoverage.class, new DefaultTestCaseCoverageValueCoder(inputPathCache));
    cache = caches.createCache("testCaseCoverage");
  }

  public Iterable<Entry<TestCaseCoverage>> entries() {
    return cache.entries();
  }

  @CheckForNull
  public TestCaseCoverage getCoverage(InputFile testFile, String testCaseName, InputFile mainFile) {
    Preconditions.checkNotNull(testFile);
    Preconditions.checkNotNull(testCaseName);
    Preconditions.checkNotNull(mainFile);
    return cache.get(((DefaultInputFile) testFile).key(), testCaseName, ((DefaultInputFile) mainFile).key());
  }

  public TestCaseCoverageCache put(TestCaseCoverage coverage) {
    Preconditions.checkNotNull(coverage);
    cache.put(((DefaultInputFile) coverage.testFile()).key(), coverage.testName(), ((DefaultInputFile) coverage.coveredFile()).key(), coverage);
    return this;
  }

}
