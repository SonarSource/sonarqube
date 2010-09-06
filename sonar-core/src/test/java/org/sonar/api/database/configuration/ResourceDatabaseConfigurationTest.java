/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.api.database.configuration;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ResourceDatabaseConfigurationTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldNotLoadGlobalProperties() {
    setupData("shouldNotLoadGlobalProperties");

    ResourceDatabaseConfiguration conf = new ResourceDatabaseConfiguration(getSessionFactory(), 100);
    assertEquals(2, CollectionUtils.size(conf.getKeys()));
    assertEquals("project_value1", conf.getString("key1"));
    assertNull(conf.getString("key2"));
  }

}
