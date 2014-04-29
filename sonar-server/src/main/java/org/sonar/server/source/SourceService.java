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

  public List<String> getLinesAsHtml(String fileKey) {
    return getLinesAsHtml(fileKey, null, null);
  }

  public List<String> getLinesAsHtml(String fileKey, @Nullable Integer from, @Nullable Integer to) {
    checkPermission(fileKey);

    List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(fileKey, from, to);
    if (!decoratedSource.isEmpty()) {
      return decoratedSource;
    }
    return deprecatedSourceDecorator.getSourceAsHtml(fileKey, from, to);
  }

  public void checkPermission(String fileKey) {
    ResourceDto project = resourceDao.getRootProjectByComponentKey(fileKey);
    if (project == null) {
      throw new NotFoundException("File does not exist");
    }
    UserSession.get().checkProjectPermission(UserRole.CODEVIEWER, project.getKey());
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public String getScmAuthorData(String fileKey) {
    return findDataFromComponent(fileKey, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
  }

  /**
   * Warning - does not check permission
   */
  @CheckForNull
  public String getScmDateData(String fileKey) {
    return findDataFromComponent(fileKey, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
  }

  @CheckForNull
  private String findDataFromComponent(String fileKey, String metricKey) {
    MeasureDataDto data = measureDataDao.findByComponentKeyAndMetricKey(fileKey, metricKey);
    if (data != null) {
      return data.getText();
    }
    return null;
  }
}
