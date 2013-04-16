/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.core.source;

import com.google.common.base.Strings;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.source.jdbc.SnapshotDataDao;
import org.sonar.core.source.jdbc.SnapshotDataDto;
import org.sonar.core.source.jdbc.SnapshotSourceDao;

import java.util.Collection;

/**
 * @since 3.6
 */
public class HtmlSourceDecorator {

  private final SnapshotSourceDao snapshotSourceDao;
  private final SnapshotDataDao snapshotDataDao;

  public HtmlSourceDecorator(MyBatis myBatis) {
    snapshotSourceDao = new SnapshotSourceDao(myBatis);
    snapshotDataDao = new SnapshotDataDao(myBatis);
  }

  public Collection<String> getDecoratedSourceAsHtml(long snapshotId) {

    String snapshotSource = snapshotSourceDao.selectSnapshotSource(snapshotId);
    Collection<SnapshotDataDto> snapshotDataEntries = snapshotDataDao.selectSnapshotData(snapshotId);

    if (snapshotSource != null && snapshotDataEntries != null) {
      DecorationDataHolder decorationDataHolder = new DecorationDataHolder();
      for (SnapshotDataDto snapshotDataEntry : snapshotDataEntries) {
        loadSnapshotData(decorationDataHolder, snapshotDataEntry);
      }

      HtmlTextDecorator textDecorator = new HtmlTextDecorator();
      return textDecorator.decorateTextWithHtml(snapshotSource, decorationDataHolder);
    }
    return null;
  }

  private void loadSnapshotData(DecorationDataHolder decorationDataHolder, SnapshotDataDto snapshotDataEntry) {
    if(!Strings.isNullOrEmpty(snapshotDataEntry.getData())) {
      if (snapshotDataEntry.isSyntaxHighlightingData()) {
        decorationDataHolder.loadSyntaxHighlightingData(snapshotDataEntry.getData());
      } else if (snapshotDataEntry.isSymbolData()) {
        decorationDataHolder.loadSymbolReferences(snapshotDataEntry.getData());
      }
    }
  }
}
