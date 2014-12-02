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
import org.sonar.api.ServerComponent;
import org.sonar.core.source.SnapshotDataTypes;
import org.sonar.core.source.db.SnapshotDataDto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public class HtmlSourceDecorator implements ServerComponent {

  private static final String SINGLE_LINE_SYMBOLS = "single_line_symbols";

  @CheckForNull
  public String getDecoratedSourceAsHtml(@Nullable String sourceLine, @Nullable String highlighting, @Nullable String symbols) {
    Collection<SnapshotDataDto> snapshotDataEntries = Lists.newArrayList();
    if (highlighting != null) {
      SnapshotDataDto highlightingDto = new SnapshotDataDto();
      highlightingDto.setData(highlighting);
      highlightingDto.setDataType(SnapshotDataTypes.SYNTAX_HIGHLIGHTING);
      snapshotDataEntries.add(highlightingDto);
    }
    if (symbols != null) {
      SnapshotDataDto symbolsDto = new SnapshotDataDto();
      symbolsDto.setData(symbols);
      symbolsDto.setDataType(SINGLE_LINE_SYMBOLS);
      snapshotDataEntries.add(symbolsDto);
    }
    List<String> decoratedSource = decorate(sourceLine, snapshotDataEntries, 1, 1);
    if (decoratedSource == null) {
      return null;
    } else {
      if (decoratedSource.isEmpty()) {
        return "";
      } else {
        return decoratedSource.get(0);
      }
    }
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

  private void loadSnapshotData(DecorationDataHolder dataHolder, SnapshotDataDto entry) {
    if (!Strings.isNullOrEmpty(entry.getData())) {
      if (SnapshotDataTypes.SYNTAX_HIGHLIGHTING.equals(entry.getDataType())) {
        dataHolder.loadSyntaxHighlightingData(entry.getData());
      } else if (SnapshotDataTypes.SYMBOL_HIGHLIGHTING.equals(entry.getDataType())) {
        dataHolder.loadSymbolReferences(entry.getData());
      } else if (SINGLE_LINE_SYMBOLS.equals(entry.getDataType())) {
        dataHolder.loadLineSymbolReferences(entry.getData());
      }
    }
  }
}
