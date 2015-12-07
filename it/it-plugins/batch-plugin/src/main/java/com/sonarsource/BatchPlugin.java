package com.sonarsource;

import com.sonarsource.decimal_scale_of_measures.DecimalScaleMeasureComputer;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleMetric;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleProperty;
import com.sonarsource.decimal_scale_of_measures.DecimalScaleSensor;
import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class BatchPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(
      // SONAR-6939 decimal_scale_of_measures
      DecimalScaleMeasureComputer.class,
      DecimalScaleMetric.class,
      DecimalScaleSensor.class,
      DecimalScaleProperty.definition(),

      DumpSettingsInitializer.class,
      RaiseMessageException.class,
      TempFolderExtension.class,
      WaitingSensor.class);
  }

}
