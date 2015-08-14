package com.sonarsource;

import java.util.Arrays;
import java.util.List;
import org.sonar.api.SonarPlugin;

public class BatchPlugin extends SonarPlugin {

  public List getExtensions() {
    return Arrays.asList(
      DumpSettingsInitializer.class,
      RaiseMessageException.class,
      TempFolderExtension.class
      );
  }

}
