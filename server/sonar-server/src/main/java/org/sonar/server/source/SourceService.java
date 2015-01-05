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

import com.google.common.base.Splitter;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class SourceService implements ServerComponent {

  private static final int THREE_THOUSAND_LINES_SIZE = 3000 * 20;

  private final DbClient dbClient;
  private final SnapshotSourceDao snapshotSourceDao;
  private final HtmlSourceDecorator sourceDecorator;

  /**
   * Old service to colorize code
   */
  private final DeprecatedSourceDecorator deprecatedSourceDecorator;

  public SourceService(DbClient dbClient, SnapshotSourceDao snapshotSourceDao, HtmlSourceDecorator sourceDecorator, DeprecatedSourceDecorator deprecatedSourceDecorator) {
    this.dbClient = dbClient;
    this.snapshotSourceDao = snapshotSourceDao;
    this.sourceDecorator = sourceDecorator;
    this.deprecatedSourceDecorator = deprecatedSourceDecorator;
  }

  @CheckForNull
  public List<String> getLinesAsHtml(String fileKey) {
    return getLinesAsHtml(fileKey, null, null);
  }

  @CheckForNull
  public List<String> getLinesAsHtml(String fileKey, @Nullable Integer from, @Nullable Integer to) {
    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto component = dbClient.componentDao().getByKey(session, fileKey);
      checkPermission(fileKey);
      String source = snapshotSourceDao.selectSnapshotSourceByComponentKey(fileKey, session);
      if (source != null) {
        if (sizeBiggerThan3000Lines(source)) {
          return getLinesAsTxt(source);
        }

        List<String> decoratedSource = sourceDecorator.getDecoratedSourceAsHtml(session, fileKey, source, from, to);
        if (decoratedSource != null) {
          return decoratedSource;
        }
        return deprecatedSourceDecorator.getSourceAsHtml(component, source, from, to);
      }
      return null;
    } finally {
      MyBatis.closeQuietly(session);
    }
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
      MeasureDto data = dbClient.measureDao().getNullableByKey(session, MeasureKey.of(fileKey, metricKey));
      if (data != null) {
        return data.getData();
      }
      return null;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private static boolean sizeBiggerThan3000Lines (String source){
    return source.length() > THREE_THOUSAND_LINES_SIZE;
  }

  private static List<String> getLinesAsTxt(@Nullable String source) {
    if (source != null) {
      return newArrayList(Splitter.onPattern("\r?\n|\r").split(source));
    }
    return Collections.emptyList();
  }

}
