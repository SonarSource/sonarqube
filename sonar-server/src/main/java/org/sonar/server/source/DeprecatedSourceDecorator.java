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

import com.google.common.base.Splitter;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.source.db.SnapshotSourceDao;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.ui.CodeColorizers;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * When a plugin do not use the new API to add syntax highlighting on source code, this class is called to add html info on source code
 */
public class DeprecatedSourceDecorator implements ServerComponent {

  private final MyBatis mybatis;

  private final ResourceDao resourceDao;
  private final CodeColorizers codeColorizers;
  private final SnapshotSourceDao snapshotSourceDao;

  public DeprecatedSourceDecorator(MyBatis mybatis, ResourceDao resourceDao, CodeColorizers codeColorizers, SnapshotSourceDao snapshotSourceDao) {
    this.mybatis = mybatis;
    this.resourceDao = resourceDao;
    this.codeColorizers = codeColorizers;
    this.snapshotSourceDao = snapshotSourceDao;
  }

  @CheckForNull
  public List<String> getSourceAsHtml(String componentKey) {
    return getSourceAsHtml(componentKey, null, null);
  }

  @CheckForNull
  public List<String> getSourceAsHtml(String componentKey, @Nullable Integer from, @Nullable Integer to) {
    SqlSession session = mybatis.openSession();
    try {
      ResourceDto component = resourceDao.getResource(ResourceQuery.create().setKey(componentKey), session);
      if (component == null) {
        throw new NotFoundException("The component '" + componentKey + "' does not exists.");
      }
      String source = snapshotSourceDao.selectSnapshotSourceByComponentKey(componentKey, session);
      if (source != null) {
        return splitSourceByLine(source, component.getLanguage(), from, to);
      } else {
        return null;
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private List<String> splitSourceByLine(String source, String language, @Nullable Integer from, @Nullable Integer to) {
    String htmlSource = codeColorizers.toHtml(source, language);
    List<String> splitSource = newArrayList(Splitter.onPattern("\r?\n|\r").split(htmlSource));
    List<String> result = newArrayList();
    for (int i = 0; i < splitSource.size(); i++) {
      int currentLine = i + 1;
      if (to != null && to < currentLine) {
        break;
      } else if (from == null || currentLine >= from) {
        result.add(splitSource.get(currentLine - 1));
      }
    }
    return result;
  }
}
