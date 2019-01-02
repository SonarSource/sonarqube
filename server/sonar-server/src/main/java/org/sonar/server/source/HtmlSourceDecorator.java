/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.source;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

public class HtmlSourceDecorator {

  @CheckForNull
  public String getDecoratedSourceAsHtml(@Nullable String sourceLine, @Nullable String highlighting, @Nullable String symbols) {
    if (sourceLine == null) {
      return null;
    }
    DecorationDataHolder decorationDataHolder = new DecorationDataHolder();
    if (StringUtils.isNotBlank(highlighting)) {
      decorationDataHolder.loadSyntaxHighlightingData(highlighting);
    }
    if (StringUtils.isNotBlank(symbols)) {
      decorationDataHolder.loadLineSymbolReferences(symbols);
    }
    HtmlTextDecorator textDecorator = new HtmlTextDecorator();
    List<String> decoratedSource = textDecorator.decorateTextWithHtml(sourceLine, decorationDataHolder, 1, 1);
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

}
