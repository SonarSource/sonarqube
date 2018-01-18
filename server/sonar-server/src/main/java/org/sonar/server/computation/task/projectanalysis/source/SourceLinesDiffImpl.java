/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.source;

import java.util.ArrayList;
import java.util.List;
import org.sonar.core.hash.SourceLinesHashesComputer;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.source.FileSourceDao;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class SourceLinesDiffImpl implements SourceLinesDiff {

  private final SourceLinesRepository sourceLinesRepository;

  private final DbClient dbClient;
  private final FileSourceDao fileSourceDao;

  public SourceLinesDiffImpl(DbClient dbClient, FileSourceDao fileSourceDao, SourceLinesRepository sourceLinesRepository) {
    this.dbClient = dbClient;
    this.fileSourceDao = fileSourceDao;
    this.sourceLinesRepository = sourceLinesRepository;
  }

  @Override
  public int[] getMatchingLines(Component component) {

    List<String> database;
    try (DbSession dbSession = dbClient.openSession(false)) {
      database = fileSourceDao.selectLineHashes(dbSession, component.getUuid());
      if (database == null) {
        database = new ArrayList<>();
      }
    }

    List<String> report;
    SourceLinesHashesComputer linesHashesComputer = new SourceLinesHashesComputer();
    try (CloseableIterator<String> lineIterator = sourceLinesRepository.readLines(component)) {
      while (lineIterator.hasNext()) {
        String line = lineIterator.next();
        linesHashesComputer.addLine(line);
      }
    }
    report = linesHashesComputer.getLineHashes();

    return new SourceLinesDiffFinder(database, report).findMatchingLines();

  }

}
