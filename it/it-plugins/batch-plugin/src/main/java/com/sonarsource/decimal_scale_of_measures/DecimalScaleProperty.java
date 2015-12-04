package com.sonarsource.decimal_scale_of_measures;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public class DecimalScaleProperty {

  public static final String KEY = "sonar.scanner.feedDecimalScaleMetric";

  public static PropertyDefinition definition() {
    return PropertyDefinition.builder(KEY).name("Enable test decimal_scale_of_measures").type(PropertyType.BOOLEAN).defaultValue(String.valueOf(false)).build();
  }
}
