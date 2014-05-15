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

package org.sonar.server.component.ws;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.properties.PropertyQuery;
import org.sonar.core.resource.ResourceDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ComponentAppActionTest {

  @Mock
  ResourceDao resourceDao;

  @Mock
  PropertiesDao propertiesDao;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    tester = new WsTester(new ComponentsWs(new ComponentAppAction(resourceDao, propertiesDao)));
  }

  @Test
  public void app() throws Exception {
    String projectKey = "org.codehaus.sonar:sonar-plugin-api:api";
    String componentKey = "org.codehaus.sonar:sonar-plugin-api:src/main/java/org/sonar/api/Plugin.java";
    MockUserSession.set().addProjectPermissions(UserRole.CODEVIEWER, projectKey).addComponent(componentKey, projectKey);

    ComponentDto file = new ComponentDto().setId(10L).setQualifier("FIL").setKey(componentKey).setName("Plugin.java")
      .setPath("src/main/java/org/sonar/api/Plugin.java").setSubProjectId(5L).setProjectId(1L);
    when(resourceDao.selectComponentByKey(componentKey)).thenReturn(file);
    when(resourceDao.findById(5L)).thenReturn(new ComponentDto().setId(5L).setLongName("SonarQube :: Plugin API"));
    when(resourceDao.findById(1L)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));
    when(propertiesDao.selectByQuery(any(PropertyQuery.class))).thenReturn(newArrayList(new PropertyDto()));

    WsTester.TestRequest request = tester.newGetRequest("api/components", "app").setParam("key", componentKey);
    request.execute().assertJson(getClass(), "app.json");
  }

}
