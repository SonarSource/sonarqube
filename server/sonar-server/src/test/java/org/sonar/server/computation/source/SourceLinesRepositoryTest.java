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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.db.source.FileSourceDto;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.component.ReportComponent.builder;

public class SourceLinesRepositoryTest {

  static final String FILE_UUID = "FILE_UUID";
  static final String FILE_KEY = "FILE_KEY";
  static final int FILE_REF = 2;

  static final Component FILE = builder(Component.Type.FILE, FILE_REF)
    .setKey(FILE_KEY)
    .setUuid(FILE_UUID)
    .build();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();

  DbSession session = db.getSession();

  SourceLinesRepositoryImpl underTest = new SourceLinesRepositoryImpl(db.getDbClient(), reportReader);

  @Test
  public void read_lines_from_report() throws Exception {
    reportReader.putFileSourceLines(FILE_REF, "line1", "line2");

    assertThat(underTest.readLines(FILE)).containsOnly("line1", "line2");
  }

  @Test
  public void not_fail_to_read_lines_on_empty_file_from_report() throws Exception {
    // File exist but there's no line
    reportReader.putFileSourceLines(FILE_REF);

    // Should not try to read source file from the db
    assertThat(underTest.readLines(FILE)).isEmpty();
  }

  @Test
  public void read_lines_from_database() throws Exception {
    insertFileSourceInDb("line1", "line2");

    assertThat(underTest.readLines(FILE)).containsOnly("line1", "line2");
  }

  @Test
  public void read_from_report_even_if_source_exist_in_db() throws Exception {
    reportReader.putFileSourceLines(FILE_REF, "report line1", "report line2");
    insertFileSourceInDb("db line1", "db line2");

    assertThat(underTest.readLines(FILE)).containsOnly("report line1", "report line2");
  }

  @Test
  public void fail_with_NPE_to_read_lines_on_null_component() throws Exception {
    thrown.expect(NullPointerException.class);
    thrown.expectMessage("Component should not be bull");

    underTest.readLines(null);
  }

  @Test
  public void fail_with_IAE_to_read_lines_on_not_file_component() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Component 'ReportComponent{ref=123, key='NotFile', type=PROJECT}' is not a file");

    underTest.readLines(builder(Component.Type.PROJECT, 123).setKey("NotFile").build());
  }

  private void insertFileSourceInDb(String... lines) {
    DbFileSources.Data.Builder dataBuilder = DbFileSources.Data.newBuilder();
    for (int i = 0; i < lines.length; i++) {
      dataBuilder.addLinesBuilder().setLine(i + 1).setSource(lines[i]).build();
    }
    db.getDbClient().fileSourceDao().insert(session,
      new FileSourceDto()
        .setFileUuid(FILE_UUID).setProjectUuid("PROJECT_UUID")
        .setSourceData(dataBuilder.build()));
    session.commit();
  }
}

