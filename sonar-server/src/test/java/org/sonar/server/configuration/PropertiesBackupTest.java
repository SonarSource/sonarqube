/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.database.configuration.Property;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.test.TestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.collection.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PropertiesBackupTest extends AbstractDbUnitTestCase {

  private SonarConfig sonarConfig;

  @Before
  public void setup() {
    sonarConfig = new SonarConfig();
  }

  @Test
  public void shouldExportProperties() {
    setupData("shouldExportProperties");

    new PropertiesBackup(getSession()).exportXml(sonarConfig);

    Property prop1 = new Property("key1", "value1");
    Property prop2 = new Property("key2", "value2");

    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), Arrays.asList(prop1, prop2)));
  }

  @Test
  public void shouldNotExportPropertiesLinkedToResources() {
    setupData("shouldNotExportPropertiesLinkedToResources");

    new PropertiesBackup(getSession()).exportXml(sonarConfig);

    Property prop1 = new Property("key1", "value1");
    Property prop2 = new Property("key2", "value2");

    assertTrue(CollectionUtils.isEqualCollection(sonarConfig.getProperties(), Arrays.asList(prop1, prop2)));
  }


  @Test
  public void shouldExportAnArrayProperty() {
    setupData("shouldExportAnArrayProperty");

    new PropertiesBackup(getSession()).exportXml(sonarConfig);

    assertThat(sonarConfig.getProperties(), hasItem(new Property("key1", "value1,value2,value3")));
  }

  @Test
  public void shouldImportProperties() {
    setupData("shouldImportProperties");

    Collection<Property> newProperties = Arrays.asList(new Property("key1", "value1"), new Property("key2", "value2"), new Property("key3", "value3"));
    sonarConfig.setProperties(newProperties);

    new PropertiesBackup(getSession()).importXml(sonarConfig);

    checkTables("shouldImportProperties", "properties");
  }

  @Test
  public void shouldImportMultilineProperties() throws Exception {
    setupData("shouldImportMultilineProperties");

    new Backup(getSession()).doImportXml(
        FileUtils.readFileToString(
            TestUtils.getResource(getClass(), "backup-with-multiline-property.xml"), CharEncoding.UTF_8));

    getSession().commit();

    Property property = getSession().getSingleResult(Property.class, "key", "sonar.multiline.secured");
    assertThat(property.getValue(), startsWith("ONQwdcwcwwdadalkdmaiQGMqMVnhtAbhxwjjoVkHbWgx"));
    assertThat(property.getValue(), endsWith("mmmm"));

  }
}
