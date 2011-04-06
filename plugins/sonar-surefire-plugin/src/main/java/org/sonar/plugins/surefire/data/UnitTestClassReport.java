/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.plugins.surefire.data;

import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

public final class UnitTestClassReport {
  private long errors = 0L, failures = 0L, skipped = 0L, tests = 0L, durationMilliseconds = 0L;
  private List<UnitTestResult> results = null;

  public UnitTestClassReport add(UnitTestClassReport other) {
    for (UnitTestResult otherResult : other.getResults()) {
      add(otherResult);
    }
    return this;
  }

  public UnitTestClassReport add(UnitTestResult result) {
    initResults();
    results.add(result);
    if (result.getStatus().equals(UnitTestResult.STATUS_SKIPPED)) {
      skipped += 1;

    } else if (result.getStatus().equals(UnitTestResult.STATUS_FAILURE)) {
      failures += 1;

    } else if (result.getStatus().equals(UnitTestResult.STATUS_ERROR)) {
      errors += 1;
    }
    tests += 1;
    durationMilliseconds += result.getDurationMilliseconds();
    return this;
  }

  private void initResults() {
    if (results == null) {
      results = Lists.newArrayList();
    }
  }

  public long getErrors() {
    return errors;
  }

  public long getFailures() {
    return failures;
  }

  public long getSkipped() {
    return skipped;
  }

  public long getTests() {
    return tests;
  }

  public long getDurationMilliseconds() {
    return durationMilliseconds;
  }

  public List<UnitTestResult> getResults() {
    if (results==null) {
      return Collections.emptyList();
    }
    return results;
  }

  public String toXml() {
    StringBuilder sb = new StringBuilder(256);
    sb.append("<tests-details>");
    for (UnitTestResult result : results) {
      result.appendXml(sb);
    }
    sb.append("</tests-details>");
    return sb.toString();
  }
}
