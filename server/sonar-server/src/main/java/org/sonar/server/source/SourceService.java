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
import org.sonar.api.ServerSide;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;

import javax.annotation.Nullable;

import java.util.List;

@ServerSide
public class SourceService {

  private final HtmlSourceDecorator sourceDecorator;
  private final SourceLineIndex sourceLineIndex;

  public SourceService(HtmlSourceDecorator sourceDecorator, SourceLineIndex sourceLineIndex) {
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
}
