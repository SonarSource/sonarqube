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
package org.sonar.core.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.sonar.api.ServerComponent;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;
import org.sonar.core.source.jdbc.SnapshotSourceDao;

import java.util.Collection;
import java.util.List;

/**
 * @since 3.6
 */
public class HtmlSourceDecorator implements ServerComponent {

  private final SnapshotSourceDao snapshotSourceDao;
  private final SnapshotDataDao snapshotDataDao;

  public HtmlSourceDecorator(MyBatis myBatis) {
    this.snapshotSourceDao = new SnapshotSourceDao(myBatis);
    this.snapshotDataDao = new SnapshotDataDao(myBatis);
  }

  @VisibleForTesting
  protected HtmlSourceDecorator(SnapshotSourceDao snapshotSourceDao, SnapshotDataDao snapshotDataDao) {
    this.snapshotSourceDao = snapshotSourceDao;
    this.snapshotDataDao= snapshotDataDao;
  }

  public Collection<String> getDecoratedSourceAsHtml(long snapshotId) {

    List<String> highlightingDataTypes = Lists.newArrayList(SnapshotDataType.SYNTAX_HIGHLIGHTING.getValue(),
      SnapshotDataType.SYMBOL_HIGHLIGHTING.getValue());

    Collection<SnapshotDataDto> snapshotDataEntries = snapshotDataDao.selectSnapshotData(snapshotId, highlightingDataTypes);

    if (!snapshotDataEntries.isEmpty()) {
      String snapshotSource = snapshotSourceDao.selectSnapshotSource(snapshotId);
      if(snapshotSource != null) {
        DecorationDataHolder decorationDataHolder = new DecorationDataHolder();
        for (SnapshotDataDto snapshotDataEntry : snapshotDataEntries) {
          loadSnapshotData(decorationDataHolder, snapshotDataEntry);
        }

        HtmlTextDecorator textDecorator = new HtmlTextDecorator();
        return textDecorator.decorateTextWithHtml(snapshotSource, decorationDataHolder);
      }
    }
    return null;
  }

  private void loadSnapshotData(DecorationDataHolder decorationDataHolder, SnapshotDataDto snapshotDataEntry) {
    if(!Strings.isNullOrEmpty(snapshotDataEntry.getData())) {
      if (SnapshotDataType.isSyntaxHighlighting(snapshotDataEntry.getDataType())) {
        decorationDataHolder.loadSyntaxHighlightingData(snapshotDataEntry.getData());
      } else if (SnapshotDataType.isSymbolHighlighting(snapshotDataEntry.getDataType())) {
        decorationDataHolder.loadSymbolReferences(snapshotDataEntry.getData());
      }
    }
  }
}
