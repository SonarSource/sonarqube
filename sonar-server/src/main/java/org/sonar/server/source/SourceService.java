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
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.UserRole;
import org.sonar.core.measure.db.MeasureDataDao;
import org.sonar.core.measure.db.MeasureDataDto;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

public class SourceService implements ServerComponent {

  private final HtmlSourceDecorator sourceDecorator;

  /**
   * Old service to colorize code
   */
  private final DeprecatedSourceDecorator deprecatedSourceDecorator;

  private final ResourceDao resourceDao;
  private final MeasureDataDao measureDataDao;

  public SourceService(HtmlSourceDecorator sourceDecorator, DeprecatedSourceDecorator deprecatedSourceDecorator, ResourceDao resourceDao, MeasureDataDao measureDataDao) {
    this.sourceDecorator = sourceDecorator;
    this.deprecatedSourceDecorator = deprecatedSourceDecorator;
    this.resourceDao = resourceDao;
    this.measureDataDao = measureDataDao;
  }

  public List<String> getSourcesForComponent(String componentKey) {
    return getSourcesByComponent(componentKey, null, null);
  }

  public List<String> getSourcesByComponent(String componentKey, @Nullable Integer from, @Nullable Integer to) {
    ResourceDto project = resourceDao.getRootProjectByComponentKey(componentKey);
    if (project == null) {
      throw new NotFoundException("This component does not exists.");
    }
    UserSession.get().checkProjectPermission(UserRole.CODEVIEWER, project.getKey());

    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(componentKey, from, to);
    if (decoratedSource != null) {
      return decoratedSource;
    } else {
     return deprecatedSourceDecorator.getSourceAsHtml(componentKey, from, to);
    }
  }

  @CheckForNull
  public String getScmAuthorData(String componentKey) {
    return findDataFromComponent(componentKey, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
  }

  @CheckForNull
  public String getScmDateData(String componentKey) {
    return findDataFromComponent(componentKey, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
  }

  @CheckForNull
  private String findDataFromComponent(String componentKey, String metricKey) {
    MeasureDataDto data = measureDataDao.findByComponentKeyAndMetricKey(componentKey, metricKey);
    if (data != null) {
      return data.getText();
    }
    return null;
  }
}
