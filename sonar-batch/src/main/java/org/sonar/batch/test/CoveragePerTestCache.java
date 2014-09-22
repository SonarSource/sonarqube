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
import org.sonar.api.batch.sensor.test.TestCase;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCase;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;

import javax.annotation.CheckForNull;

import java.util.List;

/**
 * Cache of coverage per test. This cache is shared amongst all project modules.
 */
public class CoveragePerTestCache implements BatchComponent {

  private final Cache<List<Integer>> cache;

  public CoveragePerTestCache(Caches caches) {
    cache = caches.createCache("coveragePerTest");
  }

  public Iterable<Entry<List<Integer>>> entries() {
    return cache.entries();
  }

  @CheckForNull
  public List<Integer> getCoveredLines(InputFile testFile, String testCaseName, InputFile mainFile) {
    Preconditions.checkNotNull(testFile);
    Preconditions.checkNotNull(testCaseName);
    return cache.get(((DefaultInputFile) testFile).key(), testCaseName, ((DefaultInputFile) mainFile).key());
  }

  public CoveragePerTestCache put(TestCase testCase, InputFile mainFile, List<Integer> coveredLines) {
    Preconditions.checkNotNull(testCase);
    Preconditions.checkNotNull(mainFile);
    Preconditions.checkNotNull(coveredLines);
    cache.put(((DefaultInputFile) ((DefaultTestCase) testCase).testFile()).key(), testCase.name(), ((DefaultInputFile) mainFile).key(), coveredLines);
    return this;
  }

}
