/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version.v51;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.DateUtils;
import org.sonar.db.Database;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

public class FeedFileSourcesBinaryData extends BaseDataChange {

  public FeedFileSourcesBinaryData(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("issues");
    update.select("SELECT id,data FROM file_sources WHERE binary_data is null");
    update.update("UPDATE file_sources SET binary_data=? WHERE id=?");
    update.execute(new MassUpdate.Handler() {
      @Override
      public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
        Long fileSourceId = row.getNullableLong(1);
        update.setBytes(1, toBinary(fileSourceId, row.getNullableString(2)));
        update.setLong(2, fileSourceId);
        return true;
      }
    });
  }

  private byte[] toBinary(Long fileSourceId, @Nullable String data) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    CSVParser parser = null;
    try {
      if (data != null) {
        parser = CSVParser.parse(data, CSVFormat.DEFAULT);
        Iterator<CSVRecord> rows = parser.iterator();
        int line = 1;
        while (rows.hasNext()) {
          CSVRecord row = rows.next();
          if (row.size() == 16) {

            DbFileSources.Line.Builder lineBuilder = dataBuilder.addLinesBuilder();
            lineBuilder.setLine(line);
            String s = row.get(0);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setScmRevision(s);
            }
            s = row.get(1);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setScmAuthor(s);
            }
            Date scmDate = DateUtils.parseDateTimeQuietly(row.get(2));
            if (scmDate != null) {
              lineBuilder.setScmDate(scmDate.getTime());
            }
            s = row.get(3);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setUtLineHits(Integer.parseInt(s));
            }
            s = row.get(4);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setUtConditions(Integer.parseInt(s));
            }
            s = row.get(5);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setUtCoveredConditions(Integer.parseInt(s));
            }
            s = row.get(6);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setItLineHits(Integer.parseInt(s));
            }
            s = row.get(7);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setItConditions(Integer.parseInt(s));
            }
            s = row.get(8);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setItCoveredConditions(Integer.parseInt(s));
            }
            s = row.get(9);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setOverallLineHits(Integer.parseInt(s));
            }
            s = row.get(10);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setOverallConditions(Integer.parseInt(s));
            }
            s = row.get(11);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setOverallCoveredConditions(Integer.parseInt(s));
            }
            s = row.get(12);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setHighlighting(s);
            }
            s = row.get(13);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.setSymbols(s);
            }
            s = row.get(14);
            if (StringUtils.isNotEmpty(s)) {
              lineBuilder.addAllDuplication(splitIntegers(s));
            }
            s = row.get(15);
            if (s != null) {
              lineBuilder.setSource(s);
            }
          }
          line++;
        }
      }
      return FileSourceDto.encodeSourceData(dataBuilder.build());
    } catch (Exception e) {
      throw new IllegalStateException("Invalid FILE_SOURCES.DATA on row with ID " + fileSourceId + ": " + data, e);
    } finally {
      IOUtils.closeQuietly(parser);
    }
  }

  private static Iterable<Integer> splitIntegers(String s) {
    return Iterables.transform(Splitter.on(',').omitEmptyStrings().trimResults().split(s), new Function<String, Integer>() {
      @Override
      public Integer apply(@Nonnull String input) {
        return Integer.parseInt(input);
      }
    });
  }
}
