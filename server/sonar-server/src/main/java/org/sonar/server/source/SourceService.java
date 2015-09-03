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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import javax.annotation.Nonnull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;

public class SourceService {

  private final DbClient dbClient;
  private final HtmlSourceDecorator htmlDecorator;

  public SourceService(DbClient dbClient, HtmlSourceDecorator htmlDecorator) {
    this.dbClient = dbClient;
    this.htmlDecorator = htmlDecorator;
  }

  /**
   * Returns a range of lines as raw db data. User permission is not verified.
   * @param from starts from 1
   * @param toInclusive starts from 1, must be greater than or equal param {@code from}
   */
  public Optional<Iterable<DbFileSources.Line>> getLines(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, Functions.<DbFileSources.Line>identity());
  }

  /**
   * Returns a range of lines as raw text.
   * @see #getLines(DbSession, String, int, int)
   */
  public Optional<Iterable<String>> getLinesAsRawText(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, LineToRaw.INSTANCE);
  }

  public Optional<Iterable<String>> getLinesAsHtml(DbSession dbSession, String fileUuid, int from, int toInclusive) {
    return getLines(dbSession, fileUuid, from, toInclusive, lineToHtml());
  }

  /**
   * Returns a single line from a source file. {@code Optional.absent()} is returned if the
   * file or the line do not exist.
   * @param line starts from 1
   */
  public Optional<DbFileSources.Line> getLine(DbSession dbSession, String fileUuid, int line) {
    verifyLine(line);
    FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.absent();
    }
    DbFileSources.Data data = dto.getSourceData();
    return FluentIterable.from(data.getLinesList())
      .filter(new IsGreaterOrEqualThanLine(line))
      .first();
  }

  private <E> Optional<Iterable<E>> getLines(DbSession dbSession, String fileUuid, int from, int toInclusive, Function<DbFileSources.Line, E> function) {
    verifyLine(from);
    Preconditions.checkArgument(toInclusive >= from, String.format("Line number must greater than or equal to %d, got %d", from, toInclusive));
    FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(dbSession, fileUuid);
    if (dto == null) {
      return Optional.absent();
    }
    DbFileSources.Data data = dto.getSourceData();
    return Optional.of((Iterable<E>) FluentIterable.from(data.getLinesList())
      .filter(new IsGreaterOrEqualThanLine(from))
      .limit(toInclusive - from + 1)
      .transform(function));
  }

  private static void verifyLine(int line) {
    Preconditions.checkArgument(line >= 1, String.format("Line number must start at 1, got %d", line));
  }

  private Function<DbFileSources.Line, String> lineToHtml() {
    return new Function<DbFileSources.Line, String>() {
      @Override
      public String apply(@Nonnull DbFileSources.Line line) {
        return htmlDecorator.getDecoratedSourceAsHtml(line.getSource(), line.getHighlighting(), line.getSymbols());
      }
    };
  }

  private enum LineToRaw implements Function<DbFileSources.Line, String> {
    INSTANCE;
    @Override
    public String apply(@Nonnull DbFileSources.Line line) {
      return line.getSource();
    }

  }

  private static class IsGreaterOrEqualThanLine implements Predicate<DbFileSources.Line> {
    private final int from;

    IsGreaterOrEqualThanLine(int from) {
      this.from = from;
    }

    @Override
    public boolean apply(@Nonnull DbFileSources.Line line) {
      return line.hasLine() && line.getLine() >= from;
    }
  }
}
