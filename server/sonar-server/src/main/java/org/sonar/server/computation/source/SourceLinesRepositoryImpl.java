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

package org.sonar.server.computation.source;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import javax.annotation.Nonnull;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;

import static com.google.common.collect.FluentIterable.from;
import static org.sonar.server.computation.component.Component.Type.FILE;

public class SourceLinesRepositoryImpl implements SourceLinesRepository {

  private final DbClient dbClient;
  private final BatchReportReader reportReader;

  public SourceLinesRepositoryImpl(DbClient dbClient, BatchReportReader reportReader) {
    this.dbClient = dbClient;
    this.reportReader = reportReader;
  }

  @Override
  public CloseableIterator<String> readLines(Component component) {
    Preconditions.checkNotNull(component, "Component should not be bull");
    if (!component.getType().equals(FILE)) {
      throw new IllegalArgumentException(String.format("Component '%s' is not a file", component));
    }

    Optional<CloseableIterator<String>> linesIteratorOptional = reportReader.readFileSource(component.getReportAttributes().getRef());
    if (linesIteratorOptional.isPresent()) {
      return linesIteratorOptional.get();
    }
    DbSession session = dbClient.openSession(false);
    try {
      return readLinesFromDb(session, component);
    } finally {
      dbClient.closeSession(session);
    }
  }

  private CloseableIterator<String> readLinesFromDb(DbSession session, Component component) {
    FileSourceDto dto = dbClient.fileSourceDao().selectSourceByFileUuid(session, component.getUuid());
    if (dto == null) {
      throw new IllegalStateException(String.format("The file '%s' has no source", component));
    }
    DbFileSources.Data data = dto.getSourceData();
    return CloseableIterator.from(from(data.getLinesList()).transform(LineToRaw.INSTANCE).iterator());
  }

  private enum LineToRaw implements Function<DbFileSources.Line, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull DbFileSources.Line line) {
      return line.getSource();
    }
  }

}
