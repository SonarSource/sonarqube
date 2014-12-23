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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDao;
import org.sonar.core.source.db.SnapshotDataDto;
import org.sonar.core.source.db.SnapshotSourceDao;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class HtmlSourceDecorator implements ServerComponent {

  private final SnapshotSourceDao snapshotSourceDao;
  private final SnapshotDataDao snapshotDataDao;

  public HtmlSourceDecorator(SnapshotSourceDao snapshotSourceDao, SnapshotDataDao snapshotDataDao) {
    this.snapshotSourceDao = snapshotSourceDao;
    this.snapshotDataDao = snapshotDataDao;
  }

  @CheckForNull
  public List<String> getDecoratedSourceAsHtml(SqlSession session, String componentKey, String snapshotSource, @Nullable Integer from, @Nullable Integer to) {
    Collection<SnapshotDataDto> snapshotDataEntries = snapshotDataDao.selectSnapshotDataByComponentKey(componentKey, highlightingDataTypes(), session);
    if (!snapshotDataEntries.isEmpty()) {
      return decorate(snapshotSource, snapshotDataEntries, from, to);
    }
    return null;
  }

  /**
   * Only used on rails side to display source in issue viewer (display 10 lines, so no need to return only source code if more than 3000 lines)
   */
  @CheckForNull
  public List<String> getDecoratedSourceAsHtml(long snapshotId) {
    Collection<SnapshotDataDto> snapshotDataEntries = snapshotDataDao.selectSnapshotData(snapshotId, highlightingDataTypes());
    if (!snapshotDataEntries.isEmpty()) {
      String snapshotSource = snapshotSourceDao.selectSnapshotSource(snapshotId);
      if (snapshotSource != null) {
        return decorate(snapshotSource, snapshotDataEntries, null, null);
      }
    }
    return null;
  }

  @CheckForNull
  private List<String> decorate(@Nullable String snapshotSource, Collection<SnapshotDataDto> snapshotDataEntries, @Nullable Integer from, @Nullable Integer to) {
    if (snapshotSource != null) {
      DecorationDataHolder decorationDataHolder = new DecorationDataHolder();
      for (SnapshotDataDto snapshotDataEntry : snapshotDataEntries) {
        loadSnapshotData(decorationDataHolder, snapshotDataEntry);
      }

      HtmlTextDecorator textDecorator = new HtmlTextDecorator();
      return textDecorator.decorateTextWithHtml(snapshotSource, decorationDataHolder, from, to);
    }
    return null;
  }

  private List<String> highlightingDataTypes() {
    return Lists.newArrayList(SnapshotDataTypes.SYNTAX_HIGHLIGHTING,
      SnapshotDataTypes.SYMBOL_HIGHLIGHTING);
  }

  private void loadSnapshotData(DecorationDataHolder dataHolder, SnapshotDataDto entry) {
    if (!Strings.isNullOrEmpty(entry.getData())) {
      if (SnapshotDataTypes.SYNTAX_HIGHLIGHTING.equals(entry.getDataType())) {
        dataHolder.loadSyntaxHighlightingData(entry.getData());
      } else if (SnapshotDataTypes.SYMBOL_HIGHLIGHTING.equals(entry.getDataType())) {
        dataHolder.loadSymbolReferences(entry.getData());
      }
    }
  }
}
