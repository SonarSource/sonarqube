package com.mycompany.sonar.standard;

import com.mycompany.sonar.standard.batch.RandomDecorator;
import com.mycompany.sonar.standard.batch.SampleSensor;
import com.mycompany.sonar.standard.ui.SampleFooter;
import com.mycompany.sonar.standard.ui.SampleRubyWidget;
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
        // Definitions
        SampleMetrics.class,

        // Batch
        SampleSensor.class, RandomDecorator.class,

        // UI
        SampleFooter.class, SampleRubyWidget.class);
  }
}