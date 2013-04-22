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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.database.configuration.Property;
import org.sonar.core.properties.PropertyDto;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.server.platform.PersistentSettings;

import java.util.Arrays;
import java.util.Collection;

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
  public void shouldExportProperties() {
    when(persistentSettings.getGlobalProperties()).thenReturn(Lists.newArrayList(new PropertyDto().setKey("key1").setValue("value1"), new PropertyDto().setKey("key2").setValue("value2")));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key1", "value1"), new Property("key2", "value2"));
  }

  @Test
  public void shouldNotExportServerId() {
    when(persistentSettings.getGlobalProperties()).thenReturn(Lists.newArrayList(new PropertyDto().setKey(CoreProperties.SERVER_ID).setValue("111"), new PropertyDto().setKey("key").setValue("value")));

    SonarConfig config = new SonarConfig();
    backup.exportXml(config);

    assertThat(config.getProperties()).containsOnly(new Property("key", "value"));
  }

  @Test
  public void shouldImportBackupOfProperties() {
    SonarConfig config = new SonarConfig();
    config.setProperties(Arrays.asList(new Property("key1", "value1")));

    backup.importXml(config);

    verify(persistentSettings).saveProperties(argThat(IsMapContaining.hasEntry("key1", "value1")));
  }

  @Test
  public void shouldNotImportServerId() {
    // initial server id
    when(persistentSettings.getString(CoreProperties.SERVER_ID)).thenReturn("111");

    Collection<Property> newProperties = Arrays.asList(new Property(CoreProperties.SERVER_ID, "999"));
    SonarConfig config = new SonarConfig();
    config.setProperties(newProperties);
    backup.importXml(config);

    verify(persistentSettings).saveProperties(argThat(IsMapContaining.hasEntry(CoreProperties.SERVER_ID, "111")));
  }
}
