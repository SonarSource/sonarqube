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

package org.sonar.server.component;

import org.junit.Before;
import org.sonar.api.component.Component;
import org.sonar.core.resource.ResourceDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultRubyComponentServiceTest {

  private ResourceDao resourceDao;
  private DefaultRubyComponentService componentService;

  @Before
  public void before(){
    resourceDao = mock(ResourceDao.class);
    componentService = new DefaultRubyComponentService(resourceDao);
  }

  @Before
  public void should_find_by_key() {
    Component component = mock(Component.class);
    when(resourceDao.findByKey("struts")).thenReturn(component);

    assertThat(componentService.findByKey("struts")).isEqualTo(component);
  }
}
