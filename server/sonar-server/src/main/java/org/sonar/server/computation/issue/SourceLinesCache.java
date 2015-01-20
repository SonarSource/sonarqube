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
package org.sonar.server.computation.issue;

import com.google.common.base.Function;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;

import javax.annotation.CheckForNull;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache of the lines of the currently processed file. Only a single file
 * is kept in memory at a time. Moreover data is loaded on demand to avoid
 * useless db trips.
 * <p/>
 * It assumes that db table FILE_SOURCES is up-to-date before using of this
 * cache.
 */
public class SourceLinesCache {

  private final DbClient dbClient;
  private final List<String> authors = new ArrayList<>();
  private final FileDataParser parserFunction = new FileDataParser();
  private boolean loaded = false;
  private String currentFileUuid = null;

  public SourceLinesCache(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Marks the currently processed component
   */
  void init(String fileUuid) {
    loaded = false;
    currentFileUuid = fileUuid;
  }

  /**
   * Last committer of the line, can be null.
   * @param lineId starts at 1
   */
  @CheckForNull
  public String lineAuthor(int lineId) {
    loadIfNeeded();
    if (lineId <= authors.size()) {
      return authors.get(lineId - 1);
    }
    return null;
  }

  /**
   * Load only on demand, to avoid useless db requests on files without any new issues
   */
  private void loadIfNeeded() {
    if (!loaded) {
      dbClient.fileSourceDao().readDataStream(currentFileUuid, parserFunction);
      loaded = true;
    }
  }

  /**
   * Makes cache eligible to GC
   */
  public void clear() {
    authors.clear();
  }

  /**
   * Number of lines in cache of the current file
   */
  int countLines() {
    return authors.size();
  }

  class FileDataParser implements Function<Reader, Void> {
    @Override
    public Void apply(Reader input) {
      CSVParser csvParser = null;
      try {
        csvParser = new CSVParser(input, CSVFormat.DEFAULT);
        authors.clear();
        for (CSVRecord csvRecord : csvParser) {
          // do not keep all fields in memory
          String author = csvRecord.get(FileSourceDto.CSV_INDEX_SCM_AUTHOR);
          authors.add(author);
        }
        return null;
      } catch (Exception e) {
        throw new IllegalStateException("Fail to parse CSV data", e);
      } finally {
        IOUtils.closeQuietly(csvParser);
      }
    }
  }
}
