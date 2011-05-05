package com.mycompany.sonar.pmd;

import org.sonar.api.SonarPlugin;

import java.util.Arrays;
import java.util.List;

public class PmdExtensionsPlugin extends SonarPlugin {
  
  public List getExtensions() {
    return Arrays.asList(PmdExtensionRepository.class);
  }
  
}
