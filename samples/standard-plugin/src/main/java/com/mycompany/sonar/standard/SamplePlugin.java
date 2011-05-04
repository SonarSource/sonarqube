package com.mycompany.sonar.standard;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

/**
 * This class is the entry point for all extensions
 */
public final class SamplePlugin extends SonarPlugin {

  // This is where you're going to declare all your Sonar extensions
  public List getExtensions() {
    return Arrays.asList(
        SampleMetrics.class, SampleSensor.class, SampleRubyWidget.class,

        // UI
        SampleFooter.class);
  }
}