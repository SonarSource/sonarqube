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
import org.apache.commons.lang.StringUtils;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Cache of the lines of the currently processed file. Only a single file
 * is kept in memory at a time. Moreover data is loaded on demand to avoid
 * useless db trips.
 * <p/>
 * It assumes that db table FILE_SOURCES is up-to-date before using this
 * cache.
 */
public class SourceLinesCache {

  private final DbClient dbClient;
  private final FileDataParser parserFunction = new FileDataParser();

  private final List<String> authors = new ArrayList<>();
  private boolean loaded = false;
  private String currentFileUuid = null;

  // date of the latest commit on the file
  private long lastCommitDate = 0L;

  // author of the latest commit on the file
  private String lastCommitAuthor = null;

  public SourceLinesCache(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  /**
   * Marks the currently processed component
   */
  void init(String fileUuid) {
    loaded = false;
    currentFileUuid = fileUuid;
    authors.clear();
    lastCommitDate = 0L;
    lastCommitAuthor = null;
  }

  /**
   * Last committer of the line, can be null.
   * @param lineId starts at 0
   */
  @CheckForNull
  public String lineAuthor(@Nullable Integer lineId) {
    loadIfNeeded();

    if (lineId == null) {
      // issue on file, approximately estimate that author is the last committer on the file
      return lastCommitAuthor;
    }
    String author = null;
    if (lineId <= authors.size()) {
      author = authors.get(lineId - 1);
    }
    return StringUtils.defaultIfEmpty(author, lastCommitAuthor);
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

  /**
   * Parse lines from db and collect SCM information
   */
  class FileDataParser implements Function<InputStream, Void> {
    @Override
    public Void apply(InputStream input) {
      FileSourceDb.Data data = FileSourceDto.decodeData(input);
      for (FileSourceDb.Line line : data.getLinesList()) {
        String author = null;
        if (line.hasScmAuthor()) {
          author = line.getScmAuthor();
        }
        authors.add(author);
        if (line.hasScmDate() && line.getScmDate() > lastCommitDate && author != null) {
          lastCommitDate = line.getScmDate();
          lastCommitAuthor = author;
        }
      }
      return null;
    }
  }
}
