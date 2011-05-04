package com.mycompany.sonar.standard.batch;

import com.mycompany.sonar.standard.SampleMetrics;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;

public class SampleSensor implements Sensor {

  public boolean shouldExecuteOnProject(Project project) {
    // This sensor is executed on any type of projects
    return true;
  }

  public void analyse(Project project, SensorContext sensorContext) {
    Measure measure = new Measure(SampleMetrics.MESSAGE, "Hello World!");
    sensorContext.saveMeasure(measure);
  }
}
