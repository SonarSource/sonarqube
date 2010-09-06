package org.sonar.plugins.sample;

import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

import java.util.Arrays;
import java.util.List;

public class SampleMetrics implements Metrics {

  public static final Metric MESSAGE = new Metric("message_key", "Message",
      "This is a metric to store a well known message", Metric.ValueType.STRING, -1, false,
      CoreMetrics.DOMAIN_GENERAL);


  public static final Metric RANDOM = new Metric("random", "Random",
      "Random value", Metric.ValueType.FLOAT, Metric.DIRECTION_BETTER, false,
      CoreMetrics.DOMAIN_GENERAL);

  // getMetrics() method is defined in the Metrics interface and is used by
  // Sonar to retrieve the list of new Metric
  public List<Metric> getMetrics() {
    return Arrays.asList(MESSAGE, RANDOM);
  }
}
