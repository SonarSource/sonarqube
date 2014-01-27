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

package org.sonar.server.source;

import org.sonar.api.ServerComponent;
import org.sonar.api.web.UserRole;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.source.HtmlSourceDecorator;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.List;

public class SourceService implements ServerComponent {

  private final HtmlSourceDecorator sourceDecorator;
  private final ResourceDao resourceDao;

  public SourceService(HtmlSourceDecorator sourceDecorator, ResourceDao resourceDao) {
    this.sourceDecorator = sourceDecorator;
    this.resourceDao = resourceDao;
  }

  public List<String> sourcesFromComponent(String componentKey) {
    return sourcesFromComponent(componentKey, null, null);
  }

  public List<String> sourcesFromComponent(String componentKey, @Nullable Integer from, @Nullable Integer to) {
    ResourceDto project = resourceDao.getRootProjectByComponentKey(componentKey);
    if (project == null) {
      throw new NotFoundException("This component does not exists.");
    }
    UserSession.get().checkProjectPermission(UserRole.CODEVIEWER, project.getKey());
    return sourceDecorator.getDecoratedSourceAsHtml(componentKey, from, to);
  }
}
