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

import org.apache.commons.lang.ObjectUtils;
import org.elasticsearch.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.UserRole;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.List;

public class SourceService implements ServerComponent {

  private final DbClient dbClient;
  private final HtmlSourceDecorator sourceDecorator;
  private final SourceLineIndex sourceLineIndex;

  public SourceService(DbClient dbClient, HtmlSourceDecorator sourceDecorator, SourceLineIndex sourceLineIndex) {
    this.dbClient = dbClient;
    this.sourceDecorator = sourceDecorator;
    this.sourceLineIndex = sourceLineIndex;
  }

  /**
   * Raw lines of source file.
   */
  public List<String> getLinesAsTxt(String fileUuid, @Nullable Integer fromParam, @Nullable Integer toParam) {
    int from = (Integer) ObjectUtils.defaultIfNull(fromParam, 1);
    int to = (Integer) ObjectUtils.defaultIfNull(toParam, Integer.MAX_VALUE);
    List<String> lines = Lists.newArrayList();
    for (SourceLineDoc lineDoc : sourceLineIndex.getLines(fileUuid, from, to)) {
      lines.add(lineDoc.source());
    }
    return lines;
  }

  /**
   * Decorated lines of source file.
   */
  public List<String> getLinesAsHtml(String fileUuid, @Nullable Integer fromParam, @Nullable Integer toParam) {
    int from = (Integer) ObjectUtils.defaultIfNull(fromParam, 1);
    int to = (Integer) ObjectUtils.defaultIfNull(toParam, Integer.MAX_VALUE);
    List<String> lines = Lists.newArrayList();
    for (SourceLineDoc lineDoc : sourceLineIndex.getLines(fileUuid, from, to)) {
      lines.add(sourceDecorator.getDecoratedSourceAsHtml(lineDoc.source(), lineDoc.highlighting(), lineDoc.symbols()));
    }
    return lines;
  }

  @CheckForNull
  public String getScmAuthorData(String fileKey) {
    checkPermission(fileKey);
    return findDataFromComponent(fileKey, CoreMetrics.SCM_AUTHORS_BY_LINE_KEY);
  }

  @CheckForNull
  public String getScmDateData(String fileKey) {
    checkPermission(fileKey);
    return findDataFromComponent(fileKey, CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE_KEY);
  }

  private void checkPermission(String fileKey) {
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);
  }

  @CheckForNull
  private String findDataFromComponent(String fileKey, String metricKey) {
    DbSession session = dbClient.openSession(false);
    try {
      MeasureDto data = dbClient.measureDao().findByComponentKeyAndMetricKey(session, fileKey, metricKey);
      if (data != null) {
        return data.getData();
      }
      return null;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }
}
