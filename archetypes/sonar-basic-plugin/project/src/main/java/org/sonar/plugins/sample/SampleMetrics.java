package org.sonar.plugins.sample;

import org.sonar.api.measures.Metrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.CoreMetrics;

import java.util.List;
import java.util.Arrays;

public class SampleMetrics implements Metrics {

  public static final String MESSAGE_KEY = "message_key";
  public static final Metric MESSAGE = new Metric.Builder(MESSAGE_KEY, "Message", Metric.ValueType.STRING)
    .setDescription("This is a metric to store a well known message")
    .setDirection(Metric.DIRECTION_BETTER)
    .setQualitative(false)
    .setDomain(CoreMetrics.DOMAIN_GENERAL)
    .create();
  

  public static final String RANDOM_KEY = "random";
  public static final Metric RANDOM = new Metric.Builder(RANDOM_KEY, "Random", Metric.ValueType.FLOAT)
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
