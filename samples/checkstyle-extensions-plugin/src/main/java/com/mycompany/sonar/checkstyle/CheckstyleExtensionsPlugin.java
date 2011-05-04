package com.mycompany.sonar.checkstyle;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class CheckstyleExtensionsPlugin extends SonarPlugin {
  
  public List getExtensions() {
    return Arrays.asList(CheckstyleExtensionRepository.class);
  }
  
}
