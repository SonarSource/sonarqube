package com.sonarsource;

import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.Initializer;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.util.Map.Entry;
import java.util.TreeMap;

@Properties({
  @Property(
    key = DumpSettingsInitializer.SONAR_SHOW_SETTINGS,
    type = PropertyType.BOOLEAN,
    name = "Property to decide if it should output settings",
    defaultValue = "false")
})
public class DumpSettingsInitializer extends Initializer {

  public static final String SONAR_SHOW_SETTINGS = "sonar.showSettings";
  private Settings settings;

  public DumpSettingsInitializer(Settings settings) {
    this.settings = settings;
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  @Override
  public void execute(Project project) {
    if (settings.getBoolean(SONAR_SHOW_SETTINGS)) {
      TreeMap<String, String> treemap = new TreeMap<String, String>(settings.getProperties());
      for (Entry<String, String> prop : treemap.entrySet()) {
        System.out.println("  o " + project.getKey() + ":" + prop.getKey() + " = " + prop.getValue());
      }
    }
  }
}
