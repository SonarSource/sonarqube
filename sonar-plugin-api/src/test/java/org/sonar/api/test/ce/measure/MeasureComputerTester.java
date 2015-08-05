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

package org.sonar.api.test.ce.measure;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputerProvider;

public class MeasureComputerTester {

  private final MeasureComputerProviderContext context = new MeasureComputerProviderContext();

  public MeasureComputerTester(MeasureComputerProvider... providers) {
    for (MeasureComputerProvider provider : providers) {
      provider.register(context);
    }
  }

  public MeasureComputerProvider.Context context() {
    return context;
  }

  public List<MeasureComputer> getMeasureComputers() {
    return context.getMeasureComputers();
  }

  /**
   * Return the measure computer that provide the output metric
   */
  @CheckForNull
  public MeasureComputer measureComputer(String outputMetric){
    Optional<MeasureComputer> measureComputer = FluentIterable.from(getMeasureComputers()).firstMatch(new MatchingOuputMetricKey(outputMetric));
    return measureComputer.isPresent() ? measureComputer.get() : null;
  }

  private static class MatchingOuputMetricKey implements Predicate<MeasureComputer> {

    private final String metricKey;

    public MatchingOuputMetricKey(String metricKey) {
      this.metricKey = metricKey;
    }

    @Override
    public boolean apply(MeasureComputer input) {
      return input.getOutputMetrics().contains(metricKey);
    }
  }


}
