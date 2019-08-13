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

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

import static com.google.common.base.Preconditions.checkArgument;

public class SourceService {

  private final DbClient dbClient;
  private final HtmlSourceDecorator htmlDecorator;
  private final Function<DbFileSources.Line, String> lineToHtml;

  public SourceService(DbClient dbClient, HtmlSourceDecorator htmlDecorator) {
    this.dbClient = dbClient;
    this.htmlDecorator = htmlDecorator;
    this.lineToHtml = lineToHtml();
  }

  /**
   * Returns a range of lines as raw db data. User permission is not verified.
   *
   * @param from        starts from 1
   * @param toInclusive starts from 1, must be greater than or equal param {@code from}
   */
  public Optional<Iterable<DbFileSources.Line>> getLines(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, Function.identity());
  }

  public Optional<Iterable<DbFileSources.Line>> getLines(DbSession dbSession, String fileUuid, Set<Integer> lines) {
    return getLines(dbSession, fileUuid, lines, Function.identity());
  }

  /**
   * Returns a range of lines as raw text.
   *
   * @see #getLines(DbSession, String, int, int)
   */
  public Optional<Iterable<String>> getLinesAsRawText(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, DbFileSources.Line::getSource);
  }

  public Optional<Iterable<String>> getLinesAsHtml(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, lineToHtml);
  }

  private <E> Optional<Iterable<E>> getLines(DbSession dbSession, String fileUuid, int from, int toInclusive, Function<DbFileSources.Line, E> function) {
    verifyLine(from);
    checkArgument(toInclusive >= from, String.format("Line number must greater than or equal to %d, got %d", from, toInclusive));
    FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.empty();
    }
    return Optional.of(dto.getSourceData().getLinesList().stream()
      .filter(line -> line.hasLine() && line.getLine() >= from)
      .limit((toInclusive - from) + 1L)
      .map(function)
      .collect(MoreCollectors.toList()));
  }

  private <E> Optional<Iterable<E>> getLines(DbSession dbSession, String fileUuid, Set<Integer> lines, Function<DbFileSources.Line, E> function) {
    FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.empty();
    }
    return Optional.of(dto.getSourceData().getLinesList().stream()
      .filter(line -> line.hasLine() && lines.contains(line.getLine()))
      .map(function)
      .collect(MoreCollectors.toList()));
  }

  private static void verifyLine(int line) {
    checkArgument(line >= 1, String.format("Line number must start at 1, got %d", line));
  }

  private Function<DbFileSources.Line, String> lineToHtml() {
    return line -> htmlDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols());
  }

}
