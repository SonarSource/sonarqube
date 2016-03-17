/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.cache;

import java.util.HashMap;
import org.junit.Test;
import org.sonar.batch.bootstrap.GlobalProperties;
import org.sonar.core.platform.ComponentContainer;
import org.sonar.home.cache.PersistentCache;
import org.sonar.scanner.protocol.input.ProjectRepositories;
import org.sonarqube.ws.client.WsClient;

import static org.mockito.Mockito.mock;

public class ProjectSyncContainerTest {
  private ComponentContainer createParentContainer() {
    PersistentCache cache = mock(PersistentCache.class);
    WsClient server = mock(WsClient.class);

    GlobalProperties globalProps = new GlobalProperties(new HashMap<String, String>());
    ComponentContainer parent = new ComponentContainer();
    parent.add(cache);
    parent.add(server);
    parent.add(globalProps);
    return parent;
  }

  @Test
  public void testProjectRepository() {
    ProjectSyncContainer container = new ProjectSyncContainer(createParentContainer(), "my:project", true);
    container.doBeforeStart();
    container.getPicoContainer().start();
    container.getComponentByType(ProjectRepositories.class);
  }
}
