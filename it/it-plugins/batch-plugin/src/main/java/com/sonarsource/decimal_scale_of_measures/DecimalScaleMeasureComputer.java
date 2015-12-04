package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.ce.measure.Measure;
import org.sonar.api.ce.measure.MeasureComputer;

public class DecimalScaleMeasureComputer implements MeasureComputer {

  @Override
  public MeasureComputerDefinition define(MeasureComputerDefinitionContext defContext) {
    return defContext.newDefinitionBuilder()
      // Output metrics must contains at least one metric
      .setOutputMetrics(DecimalScaleMetric.KEY)

      .build();
  }

  @Override
  public void compute(MeasureComputerContext context) {
    if (context.getMeasure(DecimalScaleMetric.KEY) == null) {
      Iterable<Measure> childMeasures = context.getChildrenMeasures(DecimalScaleMetric.KEY);
      int count = 0;
      double total = 0.0;
      for (Measure childMeasure : childMeasures) {
        count++;
        total += childMeasure.getDoubleValue();
      }
      double value = 0.0;
      if (count > 0) {
        value = total / count;
      }
      context.addMeasure(DecimalScaleMetric.KEY, value);
    }
  }
}
