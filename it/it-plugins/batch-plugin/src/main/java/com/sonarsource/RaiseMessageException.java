package com.sonarsource;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.MessageException;

public class RaiseMessageException implements Sensor {

  private final Settings settings;

  public RaiseMessageException(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return settings.getBoolean("raiseMessageException");
  }

  @Override
  public void analyse(Project project, SensorContext sensorContext) {
    throw MessageException.of("Error message from plugin");
  }
}