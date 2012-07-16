/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.configuration.Property;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.server.platform.PersistentSettings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PropertiesBackupTest extends AbstractDbUnitTestCase {

  private PersistentSettings persistentSettings;
  private PropertiesBackup backup;

  @Before
  public void setup() {
    persistentSettings = mock(PersistentSettings.class);
    backup = new PropertiesBackup(persistentSettings);
  }

  @Test
  public void export_properties() {
    when(persistentSettings.getProperties()).thenReturn(ImmutableMap.of("key1", "value1", "key2", "value2"));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key1", "value1"), new Property("key2", "value2"));
  }

  @Test
  public void do_not_export_server_id() {
    when(persistentSettings.getProperties()).thenReturn(ImmutableMap.of(CoreProperties.SERVER_ID, "111"));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).isEmpty();
  }


  @Test
  public void import_backup_of_properties() {
    Collection<Property> newProperties = Arrays.asList(new Property("key1", "value1"), new Property("key2", "value2"));
    SonarConfig config = new SonarConfig();
    config.setProperties(newProperties);

    backup.importXml(config);

    verify(persistentSettings).saveProperties(argThat(new ArgumentMatcher<Map<String, String>>() {
      @Override
      public boolean matches(Object o) {
        Map<String, String> map = (Map<String, String>) o;
        return map.get("key1").equals("value1") && map.get("key2").equals("value2");
      }
    }));
  }

  @Test
  public void do_not_import_server_id() {
    // initial server id
    when(persistentSettings.getString(CoreProperties.SERVER_ID)).thenReturn("111");

    Collection<Property> newProperties = Arrays.asList(new Property(CoreProperties.SERVER_ID, "999"));
    SonarConfig config = new SonarConfig();
    config.setProperties(newProperties);
    backup.importXml(config);

    verify(persistentSettings).saveProperties(argThat(new ArgumentMatcher<Map<String, String>>() {
      @Override
      public boolean matches(Object o) {
        Map<String, String> map = (Map<String, String>) o;
        return map.get(CoreProperties.SERVER_ID).equals("111");
      }
    }));
  }
}
