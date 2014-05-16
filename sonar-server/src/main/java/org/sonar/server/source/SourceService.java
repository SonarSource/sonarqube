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
import org.sonar.core.measure.db.MeasureDao;
import org.sonar.core.measure.db.MeasureDto;
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

  private final MeasureDao measureDao;

  public SourceService(HtmlSourceDecorator sourceDecorator, DeprecatedSourceDecorator deprecatedSourceDecorator, MeasureDao measureDao) {
    this.sourceDecorator = sourceDecorator;
    this.deprecatedSourceDecorator = deprecatedSourceDecorator;
    this.measureDao = measureDao;
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
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
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
    MeasureDto data = measureDao.findByComponentKeyAndMetricKey(fileKey, metricKey);
    if (data != null) {
      return data.getData();
    }
    return null;
  }
}
