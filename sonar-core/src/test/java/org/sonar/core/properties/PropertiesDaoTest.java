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
package org.sonar.core.properties;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PropertiesDaoTest extends AbstractDaoTestCase {

  private PropertiesDao dao;

  @Before
  public void createDao() throws Exception {
    dao = new PropertiesDao(getMyBatis());
  }

  @Test
  public void shouldFindUserIdsForFavouriteResource() throws Exception {
    setupData("shouldFindUserIdsForFavouriteResource");
    List<String> userIds = dao.findUserIdsForFavouriteResource(2L);
    assertThat(userIds.size(), is(2));
    assertThat(userIds, hasItems("user3", "user4"));
  }

  @Test
  public void selectGlobalProperties() throws Exception {
    setupData("selectGlobalProperties");
    List<PropertyDto> properties = dao.selectGlobalProperties();
    assertThat(properties.size(), is(2));

    PropertyDto first = findById(properties, 1);
    assertThat(first.getKey(), is("global.one"));
    assertThat(first.getValue(), is("one"));

    PropertyDto second = findById(properties, 2);
    assertThat(second.getKey(), is("global.two"));
    assertThat(second.getValue(), is("two"));
  }

  @Test
  public void selectProjectProperties() throws Exception {
    setupData("selectProjectProperties");
    List<PropertyDto> properties = dao.selectProjectProperties("org.struts:struts");
    assertThat(properties.size(), is(1));

    PropertyDto first = properties.get(0);
    assertThat(first.getKey(), is("struts.one"));
    assertThat(first.getValue(), is("one"));
  }

  @Test
  public void setProperty_update() throws Exception {
    setupData("update");

    dao.setProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.setProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.setProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));
    dao.setProperty(new PropertyDto().setKey("null.value").setValue(null));

    checkTables("update", "properties");
  }

  @Test
  public void setProperty_insert() throws Exception {
    setupData("insert");

    dao.setProperty(new PropertyDto().setKey("global.key").setValue("new_global"));
    dao.setProperty(new PropertyDto().setKey("project.key").setResourceId(10L).setValue("new_project"));
    dao.setProperty(new PropertyDto().setKey("user.key").setUserId(100L).setValue("new_user"));

    checkTables("insert", "properties");
  }

  private PropertyDto findById(List<PropertyDto> properties, int id) {
    for (PropertyDto property : properties) {
      if (property.getId() == id) {
        return property;
      }
    }
    return null;
  }
}
