package org.sonar.plugins.sample;

import org.apache.commons.lang.math.RandomUtils;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

public class SampleSensor implements Sensor {

  public boolean shouldExecuteOnProject(Project project) {
    // this sensor is executed on any type of project
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    saveLabelMeasure(sensorContext);
    saveNumericMeasure(sensorContext);
  }

  private void saveNumericMeasure(SensorContext context) {
    // Sonar API includes many libraries like commons-lang and google-collections
    context.saveMeasure(SampleMetrics.RANDOM, RandomUtils.nextDouble());
  }

  private void saveLabelMeasure(SensorContext context) {
    Measure measure = new Measure(SampleMetrics.MESSAGE, "Hello World!");
    context.saveMeasure(measure);
  }
}
