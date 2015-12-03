package com.sonarsource;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

public class WaitingSensor implements Sensor {
  private Settings settings;

  public WaitingSensor(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return settings.getBoolean("sonar.it.enableWaitingSensor");
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    try {
      Thread.sleep(10_000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
