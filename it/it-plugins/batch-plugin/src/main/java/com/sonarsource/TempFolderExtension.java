package com.sonarsource;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.Initializer;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.TempFolder;

@Properties({
  @Property(
    key = TempFolderExtension.CREATE_TEMP_FILES,
    type = PropertyType.BOOLEAN,
    name = "Property to decide if it should create temp files",
    defaultValue = "false")
})
public class TempFolderExtension extends Initializer {

  public static final String CREATE_TEMP_FILES = "sonar.createTempFiles";
  private Settings settings;
  private TempFolder tempFolder;

  public TempFolderExtension(Settings settings, TempFolder tempFolder) {
    this.settings = settings;
    this.tempFolder = tempFolder;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void execute(Project project) {
    if (settings.getBoolean(CREATE_TEMP_FILES)) {
      System.out.println("Creating temp directory: " + tempFolder.newDir("sonar-it").getAbsolutePath());
      System.out.println("Creating temp file: " + tempFolder.newFile("sonar-it", ".txt").getAbsolutePath());
    }
  }
}
