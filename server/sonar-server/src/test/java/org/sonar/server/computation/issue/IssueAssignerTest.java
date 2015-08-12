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

import java.util.Date;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.computation.batch.BatchReportReaderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.es.EsTester;
import org.sonar.server.source.index.SourceLineDoc;
import org.sonar.server.source.index.SourceLineIndex;
import org.sonar.server.source.index.SourceLineIndexDefinition;

import static org.mockito.Mockito.mock;

public class IssueAssignerTest {

  @ClassRule
  public static EsTester esTester = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  DefaultAssignee defaultAssignee = mock(DefaultAssignee.class);
  Component file = ReportComponent.builder(Component.Type.FILE, 1).build();

  IssueAssigner underTest;

  @Before
  public void setUp() {
    esTester.truncateIndices();
    underTest = new IssueAssigner(new SourceLineIndex(esTester.client()), reportReader, scmAccountToUser, defaultAssignee);
  }

  @Test
  public void line_author_from_report() {
    reportReader.putChangesets(BatchReport.Changesets.newBuilder()
      .setComponentRef(123_456_789)
      .addChangeset(newChangeset("charb", "123-456-789", 123_456_789L))
      .addChangeset(newChangeset("wolinski", "987-654-321", 987_654_321L))
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(0)
      .addChangesetIndexByLine(1)
      .build());

    underTest.beforeComponent(file);
    // underTest.onIssue(file, issue);
    // sut.init("ANY_UUID", 123_456_789, reportReader);
    //
    // assertThat(sut.lineAuthor(1)).isEqualTo("charb");
    // assertThat(sut.lineAuthor(2)).isEqualTo("charb");
    // assertThat(sut.lineAuthor(3)).isEqualTo("wolinski");
    // // compute last author
    // assertThat(sut.lineAuthor(4)).isEqualTo("wolinski");
    // assertThat(sut.lineAuthor(null)).isEqualTo("wolinski");
  }

  // @Test
  // public void line_author_from_index() throws Exception {
  // esTester.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
  // newSourceLine("cabu", "123-456-789", 123_456_789, 1),
  // newSourceLine("cabu", "123-456-789", 123_456_789, 2),
  // newSourceLine("cabu", "123-123-789", 123_456_789, 3),
  // newSourceLine("wolinski", "987-654-321", 987_654_321, 4),
  // newSourceLine("cabu", "123-456-789", 123_456_789, 5)
  // );
  //
  // sut.init("DEFAULT_UUID", 123, reportReader);
  //
  // assertThat(sut.lineAuthor(1)).isEqualTo("cabu");
  // assertThat(sut.lineAuthor(2)).isEqualTo("cabu");
  // assertThat(sut.lineAuthor(3)).isEqualTo("cabu");
  // assertThat(sut.lineAuthor(4)).isEqualTo("wolinski");
  // assertThat(sut.lineAuthor(5)).isEqualTo("cabu");
  // assertThat(sut.lineAuthor(6)).isEqualTo("wolinski");
  // }
  //
  // @Test(expected = IllegalStateException.class)
  // public void fail_when_component_ref_is_not_filled() {
  // sut.init("ANY_UUID", null, reportReader);
  // sut.lineAuthor(0);
  // }

  private BatchReport.Changesets.Changeset.Builder newChangeset(String author, String revision, long date) {
    return BatchReport.Changesets.Changeset.newBuilder()
      .setAuthor(author)
      .setRevision(revision)
      .setDate(date);
  }

  private SourceLineDoc newSourceLine(String author, String revision, long date, int lineNumber) {
    return new SourceLineDoc()
      .setScmAuthor(author)
      .setScmRevision(revision)
      .setScmDate(new Date(date))
      .setLine(lineNumber)
      .setProjectUuid("PROJECT_UUID")
      .setFileUuid("DEFAULT_UUID");
  }
}
