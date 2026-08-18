/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.issue.SecretIssueRedactor;
import org.sonar.server.issue.SecretIssueRedactionRules;

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
    return getLines(dbSession, fileUuid, from, toInclusive, Function.identity(), true);
  }

  public Optional<Iterable<DbFileSources.Line>> getLines(DbSession dbSession, String fileUuid, Set<Integer> lines) {
    return getLines(dbSession, fileUuid, lines, Function.identity(), true);
  }

  /**
   * Returns source lines for SCM metadata serialization without loading secret-redaction metadata.
   */
  public Optional<Iterable<DbFileSources.Line>> getScmLines(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, Function.identity(), false);
  }

  /**
   * Returns a range of lines as raw text.
   *
   * @see #getLines(DbSession, String, int, int)
   */
  public Optional<Iterable<String>> getLinesAsRawText(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, DbFileSources.Line::getSource, true);
  }

  public Optional<Iterable<String>> getLinesAsHtml(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, lineToHtml, true);
  }

  private <E> Optional<Iterable<E>> getLines(DbSession dbSession, String fileUuid, int from, int toInclusive, Function<DbFileSources.Line, E> function, boolean redactSource) {
    verifyLine(from);
    checkArgument(toInclusive >= from, String.format("Line number must greater than or equal to %d, got %d", from, toInclusive));
    FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.empty();
    }
    List<DbFileSources.Line> sourceLines = dto.getSourceData().getLinesList().stream()
      .filter(line -> line.hasLine() && line.getLine() >= from)
      .limit((toInclusive - from) + 1L)
      .toList();
    return Optional.of(redactIfNecessary(dbSession, fileUuid, sourceLines, redactSource).stream()
      .map(function)
      .toList());
  }

  private <E> Optional<Iterable<E>> getLines(DbSession dbSession, String fileUuid, Set<Integer> lines, Function<DbFileSources.Line, E> function, boolean redactSource) {
    FileSourceDto dto = dbClient.fileSourceDao().selectByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.empty();
    }
    List<DbFileSources.Line> sourceLines = dto.getSourceData().getLinesList().stream()
      .filter(line -> line.hasLine() && lines.contains(line.getLine()))
      .toList();
    return Optional.of(redactIfNecessary(dbSession, fileUuid, sourceLines, redactSource).stream()
      .map(function)
      .toList());
  }

  private List<DbFileSources.Line> redactIfNecessary(DbSession dbSession, String fileUuid, List<DbFileSources.Line> sourceLines, boolean redactSource) {
    if (!redactSource) {
      return sourceLines;
    }
    List<IssueDto> issues = dbClient.issueDao().selectSourceRedactionIssues(
      dbSession,
      fileUuid,
      SecretIssueRedactionRules.sourceRedactionRuleKeys()
    );
    return SecretIssueRedactor.redactSourceLines(sourceLines, issues);
  }

  private static void verifyLine(int line) {
    checkArgument(line >= 1, String.format("Line number must start at 1, got %d", line));
  }

  private Function<DbFileSources.Line, String> lineToHtml() {
    return line -> htmlDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols());
  }

}
