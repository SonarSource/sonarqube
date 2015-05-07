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
package org.sonar.server.source.index;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.DateUtils;
import org.sonar.server.es.EsTester;
import org.sonar.server.exceptions.NotFoundException;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceLineIndexTest {

  @ClassRule
  public static EsTester es = new EsTester().addDefinitions(new SourceLineIndexDefinition(new Settings()));

  private SourceLineIndex index;

  @Before
  public void setUp() {
    es.truncateIndices();
    index = new SourceLineIndex(es.client());
  }

  @Test
  public void should_retrieve_line_range() throws Exception {
    es.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      this.getClass(),
      "file1_line1.json",
      "file1_line2.json",
      "file1_line3.json",
      "file2_line1.json",
      "file2_line2.json",
      "file2_line3.json");
    assertThat(index.getLines("file1", 1, 3)).hasSize(3);
    assertThat(index.getLines("file1", 1, Integer.MAX_VALUE)).hasSize(3);
    assertThat(index.getLines("file1", 2, 2)).hasSize(1);
    assertThat(index.getLines("file1")).hasSize(3);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_reject_from_less_than_1() {
    index.getLines("polop", 0, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_reject_to_less_than_from() {
    index.getLines("polop", 2, 1);
  }

  @Test
  public void get_line() throws Exception {
    es.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      this.getClass(),
      "file1_line1.json",
      "file1_line2.json"
      );
    assertThat(index.getLine("file1", 1)).isNotNull();
    assertThat(index.getLine("file1", 2)).isNotNull();
  }

  @Test
  public void fail_to_get_line_when_line_is_not_greater_than_0() {
    try {
      index.getLine("file1", 0);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("Line should be greater than 0");
    }
  }

  @Test
  public void fail_to_get_line_on_unknown_line() throws Exception {
    es.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      this.getClass(),
      "file1_line1.json",
      "file1_line2.json"
      );
    try {
      index.getLine("file1", 1);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("No source found on line 5 for file 'file1'");
    }
  }

  @Test
  public void fail_to_get_line_on_unknown_file() {
    try {
      index.getLine("file1", 1);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(NotFoundException.class).hasMessage("No source found on line 1 for file 'file1'");
    }
  }

  @Test
  public void last_commit_of_the_whole_project() throws Exception {
    Date now = new Date();
    es.putDocuments(SourceLineIndexDefinition.INDEX, SourceLineIndexDefinition.TYPE,
      new SourceLineDoc()
        .setProjectUuid("project-uuid")
        .setScmDate(now)
        .setLine(25)
        .setFileUuid("file-uuid"),
      new SourceLineDoc()
        .setProjectUuid("project-uuid")
        .setScmDate(DateUtils.addDays(now, -1))
        .setLine(24)
        .setFileUuid("file-uuid"));

    Date returnedDate = index.lastCommitDateOnProject("project-uuid");

    assertThat(returnedDate).isEqualTo(now);
  }

  @Test
  public void last_commit_null_when_not_found() {
    Date date = index.lastCommitDateOnProject("fake-project-uuid");

    assertThat(date).isNull();
  }
}
