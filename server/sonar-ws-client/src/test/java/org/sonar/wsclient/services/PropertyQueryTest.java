/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.wsclient.services;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PropertyQueryTest extends QueryTestCase {

  @Test
  public void test_global_properties() {
    PropertyQuery query = PropertyQuery.createForAll();
    assertThat(query.getUrl(), is("/api/properties?"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }

  @Test
  public void test_project_properties() {
    PropertyQuery query = PropertyQuery.createForAll().setResourceKeyOrId("org.apache:struts");
    assertThat(query.getUrl(), is("/api/properties?resource=org.apache%3Astruts&"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }

  @Test
  public void test_global_property() {
    PropertyQuery query = PropertyQuery.createForKey("myprop");
    assertThat(query.getUrl(), is("/api/properties/myprop?"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }

  @Test
  public void test_project_property() {
    PropertyQuery query = PropertyQuery.createForResource("myprop", "my:resource");
    assertThat(query.getUrl(), is("/api/properties/myprop?resource=my%3Aresource&"));
    assertThat(query.getModelClass().getName(), is(Property.class.getName()));
  }
}
