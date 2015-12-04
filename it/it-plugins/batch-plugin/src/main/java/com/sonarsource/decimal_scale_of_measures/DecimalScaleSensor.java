package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FilePredicate;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

public class DecimalScaleSensor implements Sensor {
  private static final Logger LOG = Loggers.get(DecimalScaleSensor.class);

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void analyse(Project module, SensorContext context) {
    if (context.settings().getBoolean(DecimalScaleProperty.KEY)) {
      FilePredicate all = context.fileSystem().predicates().all();
      Iterable<InputFile> files = context.fileSystem().inputFiles(all);
      double value = 0.0001;
      for (InputFile file : files) {
        LOG.info("Value for {}: {}", file.relativePath(), value);
        context.newMeasure()
          .on(file)
          .forMetric(DecimalScaleMetric.definition())
          .withValue(value)
          .save();
        value += 0.0001;
      }
    }
  }
}
