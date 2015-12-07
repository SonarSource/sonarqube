package com.sonarsource.decimal_scale_of_measures;

import java.util.Collections;
import java.util.List;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class DecimalScaleMetric implements Metrics {

  public static final String KEY = "decimal_scale";

  private static final Metric METRIC = new Metric.Builder(KEY, "Decimal Scale", Metric.ValueType.FLOAT)
    .setDescription("Numeric metric with overridden decimal scale")
    .setDecimalScale(4)
    .create();

  @Override
  public List getMetrics() {
    return Collections.singletonList(definition());
  }

  public static Metric definition() {
    return METRIC;
  }
}
