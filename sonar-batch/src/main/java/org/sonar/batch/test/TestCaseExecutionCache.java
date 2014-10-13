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
import org.sonar.api.batch.sensor.test.TestCaseExecution;
import org.sonar.api.batch.sensor.test.internal.DefaultTestCaseExecution;
import org.sonar.batch.index.Cache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.index.Caches;
import org.sonar.batch.scan.filesystem.InputPathCache;

import javax.annotation.CheckForNull;

/**
 * Cache of all TestCases. This cache is shared amongst all project modules.
 */
public class TestCaseExecutionCache implements BatchComponent {

  private final Cache<TestCaseExecution> cache;

  public TestCaseExecutionCache(Caches caches, InputPathCache inputPathCache) {
    caches.registerValueCoder(DefaultTestCaseExecution.class, new DefaultTestCaseExecutionValueCoder(inputPathCache));
    cache = caches.createCache("testCaseExecutions");
  }

  public Iterable<Entry<TestCaseExecution>> entries() {
    return cache.entries();
  }

  @CheckForNull
  public TestCaseExecution get(InputFile testFile, String testCaseName) {
    Preconditions.checkNotNull(testFile);
    Preconditions.checkNotNull(testCaseName);
    return cache.get(((DefaultInputFile) testFile).key(), testCaseName);
  }

  public TestCaseExecutionCache put(InputFile testFile, TestCaseExecution testCase) {
    Preconditions.checkNotNull(testFile);
    Preconditions.checkNotNull(testCase);
    cache.put(((DefaultInputFile) testFile).key(), testCase.name(), testCase);
    return this;
  }

  public boolean contains(InputFile testFile, String name) {
    return cache.containsKey(((DefaultInputFile) testFile).key(), name);
  }

}
