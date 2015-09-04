package com.sonarsource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.PropertyType;
import org.sonar.api.batch.Initializer;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

@Properties({
  @Property(
    key = DumpSettingsInitializer.SONAR_SHOW_SETTINGS,
    type = PropertyType.STRING,
    name = "Property to decide if it should output settings",
    multiValues = true,
    defaultValue = "")
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
    Set<String> settingsToDump = new HashSet<>(Arrays.asList(settings.getStringArray(SONAR_SHOW_SETTINGS)));
    if (!settingsToDump.isEmpty()) {
      TreeMap<String, String> treemap = new TreeMap<String, String>(settings.getProperties());
      for (Entry<String, String> prop : treemap.entrySet()) {
        if (settingsToDump.contains(prop.getKey())) {
          System.out.println("  o " + project.getKey() + ":" + prop.getKey() + " = " + prop.getValue());
        }
      }
    }
  }
}
