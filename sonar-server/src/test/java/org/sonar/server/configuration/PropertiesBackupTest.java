/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.configuration;

import com.google.common.collect.Lists;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.configuration.Property;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.platform.PersistentSettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PropertiesBackupTest {

  private PersistentSettings persistentSettings;
  private PropertiesBackup backup;

  @Before
  public void setup() {
    persistentSettings = mock(PersistentSettings.class);
    backup = new PropertiesBackup(persistentSettings);
  }

  @Test
  public void shouldExportProperties() {
    when(persistentSettings.getGlobalProperties())
      .thenReturn(Lists.newArrayList(new PropertyDto().setKey("key1").setValue("value1"), new PropertyDto().setKey("key2").setValue("value2")));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key1", "value1"), new Property("key2", "value2"));
  }

  @Test
  public void shouldNotExportServerId() {
    when(persistentSettings.getGlobalProperties())
      .thenReturn(Lists.newArrayList(new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue("111"), new PropertyDto().setKey("key").setValue("value")));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key", "value"));
  }

  @Test
  public void shouldImportBackupOfProperties() {
    SonarConfig config = new SonarConfig();
    config.setProperties(Arrays.asList(new Property("key1", "value1")));

    backup.importXml(config);

    Map<String, String> expectedProperties = new HashMap<String, String>();
    expectedProperties.put("key1", "value1");
    verify(persistentSettings).saveProperties(argThat(IsMap.containing(expectedProperties)));
  }

  @Test
  public void shouldNotImportServerId() {
    // initial server id
    when(persistentSettings.getGlobalProperties()).thenReturn(Lists.newArrayList(
      new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue("111")));

    Collection<Property> newProperties = Arrays.asList(new Property(CoreProperties.SERVER_ID, "999"));
    SonarConfig config = new SonarConfig();
    config.setProperties(newProperties);
    backup.importXml(config);

    Map<String, String> expectedProperties = new HashMap<String, String>();
    expectedProperties.put(CoreProperties.SERVER_ID, "111");
    verify(persistentSettings).saveProperties(argThat(IsMap.containing(expectedProperties)));
  }

  @Test
  public void shouldNotImportPermissionProperties() throws Exception {
    when(persistentSettings.getGlobalProperties()).thenReturn(Lists.newArrayList(
      new PropertyDto().setKey("sonar.permission.template.default").setValue("default_template"),
      new PropertyDto().setKey("sonar.permission.template.TRK.default").setValue("default_template_for_projects"),
      new PropertyDto().setKey("erasable_key").setValue("erasable_value")));

    Collection<Property> newProperties = Arrays.asList(
      new Property("sonar.permission.template.default", "another_default"),
      new Property("key_to_import", "value_to_import"),
      new Property("erasable_key", "new_value"));
    SonarConfig config = new SonarConfig();
    config.setProperties(newProperties);
    backup.importXml(config);

    Map<String, String> expectedProperties = new HashMap<String, String>();
    expectedProperties.put("key_to_import", "value_to_import");
    expectedProperties.put("erasable_key", "new_value");
    expectedProperties.put("sonar.permission.template.default", "default_template");
    expectedProperties.put("sonar.permission.template.TRK.default", "default_template_for_projects");
    verify(persistentSettings).saveProperties(argThat(IsMap.containing(expectedProperties)));
  }

  @Test
  public void shouldNotExportPermissionProperties() throws Exception {
    when(persistentSettings.getGlobalProperties()).thenReturn(Lists.newArrayList(
      new PropertyDto().setKey("sonar.permission.template.default").setValue("default_template"),
      new PropertyDto().setKey("sonar.permission.template.TRK.default").setValue("default_template_for_projects"),
      new PropertyDto().setKey("key").setValue("value")));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key", "value"));
  }

  private static class IsMap extends ArgumentMatcher<Map<String, String>> {

    private final Map<String, String> referenceMap;

    private IsMap(Map<String, String> referenceMap) {
      this.referenceMap = referenceMap;
    }

    static IsMap containing(Map<String, String> keyValuePairs) {
      return new IsMap(keyValuePairs);
    }

    @Override
    public boolean matches(Object argument) {
      if (argument != null && argument instanceof Map) {
        Map<String, String> argAsMap = (Map<String, String>) argument;
        for (String key : argAsMap.keySet()) {
          if (!referenceMap.containsKey(key) || !referenceMap.get(key).equals(argAsMap.get(key))) {
            return false;
          }
        }
        return true;
      }
      return false;
    }

    @Override
    public void describeTo(Description description) {
      if (referenceMap != null) {
        description.appendText(referenceMap.toString());
      }
    }
  }
}
