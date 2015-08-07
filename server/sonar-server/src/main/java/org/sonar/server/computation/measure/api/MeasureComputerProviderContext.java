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

package org.sonar.server.computation.measure.api;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.ce.measure.MeasureComputer;
import org.sonar.api.ce.measure.MeasureComputerProvider;

public class MeasureComputerProviderContext implements MeasureComputerProvider.Context {

  private final List<MeasureComputer> measureComputers = new ArrayList<>();
  private final Map<String, MeasureComputer> computerByOutputMetrics = new HashMap<>();

  @Override
  public MeasureComputerProvider.Context add(MeasureComputer inputMeasureComputer) {
    MeasureComputer measureComputer = validateMeasureComputer(inputMeasureComputer);
    checkOutputMetricsNotAlreadyDefinedByAnotherComputer(measureComputer);
    this.measureComputers.add(measureComputer);
    for (String metric : measureComputer.getOutputMetrics()) {
      computerByOutputMetrics.put(metric, measureComputer);
    }
    return this;
  }

  private MeasureComputer validateMeasureComputer(MeasureComputer measureComputer) {
    if (measureComputer instanceof MeasureComputerImpl) {
      return measureComputer;
    }
    // If the computer has not been created by the builder, we recreate it to make sure it's valid
    Set<String> inputMetrics = measureComputer.getInputMetrics();
    Set<String> outputMetrics = measureComputer.getOutputMetrics();
    return newMeasureComputerBuilder()
      .setInputMetrics(inputMetrics.toArray(new String[inputMetrics.size()]))
      .setOutputMetrics(outputMetrics.toArray(new String[outputMetrics.size()]))
      .setImplementation(measureComputer.getImplementation())
      .build();
  }

  public List<MeasureComputer> getMeasureComputers() {
    return measureComputers;
  }

  private void checkOutputMetricsNotAlreadyDefinedByAnotherComputer(MeasureComputer measureComputer) {
    Set<String> duplicated = ImmutableSet.copyOf(Sets.intersection(computerByOutputMetrics.keySet(), measureComputer.getOutputMetrics()));
    if (!duplicated.isEmpty()) {
      throw new UnsupportedOperationException(generateErrorMsg(duplicated));
    }
  }

  private String generateErrorMsg(Set<String> duplicated){
    StringBuilder errorMsg = new StringBuilder();
    for (String duplicationMetric : duplicated) {
      MeasureComputer otherComputer = computerByOutputMetrics.get(duplicationMetric);
      errorMsg.append(String.format(
        "The output metric '%s' is already declared by another computer. This computer has these input metrics '%s' and these output metrics '%s'. ",
        duplicationMetric, otherComputer.getInputMetrics(), otherComputer.getOutputMetrics()));
    }
    return errorMsg.toString();
  }

  @Override
  public MeasureComputer.MeasureComputerBuilder newMeasureComputerBuilder() {
    return new MeasureComputerImpl.MeasureComputerBuilderImpl();
  }
}
