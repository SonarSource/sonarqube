package com.mycompany.sonar.standard;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public final class SampleMetrics implements Metrics {

  public static final Metric MESSAGE = new Metric.Builder("message_key", "Message", Metric.ValueType.STRING)
      .setDescription("This is a metric to store a well known message")
      .setDirection(Metric.DIRECTION_WORST)
      .setQualitative(false)
      .setDomain(CoreMetrics.DOMAIN_GENERAL)
      .create();

  public static final Metric RANDOM = new Metric.Builder("random", "Random", Metric.ValueType.FLOAT)
      .setDescription("Random value")
      .setDirection(Metric.DIRECTION_BETTER)
      .setQualitative(false)
      .setDomain(CoreMetrics.DOMAIN_GENERAL)
      .create();

  // getMetrics() method is defined in the Metrics interface and is used by
  // Sonar to retrieve the list of new Metric
  public List<Metric> getMetrics() {
    return Arrays.asList(MESSAGE, RANDOM);
  }
}
