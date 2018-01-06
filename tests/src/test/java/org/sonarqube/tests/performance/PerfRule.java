/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonarqube.tests.performance;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.CustomMatcher;
import org.junit.rules.ErrorCollector;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public abstract class PerfRule extends ErrorCollector {

  private final int runCount;
  private final List<List<Long>> recordedResults = new ArrayList<>();

  private int currentRun;

  public PerfRule(int runCount) {
    this.runCount = runCount;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        for (currentRun = 1; currentRun <= runCount; currentRun++) {
          recordedResults.add(new ArrayList<>());
          beforeEachRun();
          base.evaluate();
        }
        verify();
      }

    };
  }

  protected abstract void beforeEachRun();

  public void assertDurationAround(long duration, long expectedDuration) {
    currentResults().add(duration);
    if (isLastRun()) {
      long meanDuration = computeAverageDurationOfCurrentStep();
      double variation = 100.0 * (0.0 + meanDuration - expectedDuration) / expectedDuration;
      checkThat(String.format("Expected %d ms in average, got %d ms [%s]", expectedDuration, meanDuration, Joiner.on(",").join(getAllResultsOfCurrentStep())), Math.abs(variation),

        new CustomMatcher<Double>(
          "a value less than "
            + AbstractPerfTest.ACCEPTED_DURATION_VARIATION_IN_PERCENTS) {
          @Override
          public boolean matches(Object item) {
            return ((item instanceof Double) && ((Double) item).compareTo(AbstractPerfTest.ACCEPTED_DURATION_VARIATION_IN_PERCENTS) < 0);
          }
        });
    }
  }

  private Long[] getAllResultsOfCurrentStep() {
    Long[] result = new Long[runCount];
    for (int i = 0; i < runCount; i++) {
      result[i] = recordedResults.get(i).get(currentResults().size() - 1);
    }
    return result;
  }

  private long computeAverageDurationOfCurrentStep() {
    Long[] result = getAllResultsOfCurrentStep();
    // Compute a truncated mean by ignoring greater value
    Arrays.sort(result);
    long meanDuration = 0;
    for (int i = 0; i < (runCount - 1); i++) {
      meanDuration += result[i];
    }
    meanDuration /= (runCount - 1);
    return meanDuration;
  }

  private List<Long> currentResults() {
    return recordedResults.get(currentRun - 1);
  }

  private boolean isLastRun() {
    return currentRun == runCount;
  }

}
