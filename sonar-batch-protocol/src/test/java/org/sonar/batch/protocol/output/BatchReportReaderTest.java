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
package org.sonar.batch.protocol.output;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  BatchReportReader sut;

  @Before
  public void setUp() throws Exception {
    sut = new BatchReportReader(temp.newFolder());
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() throws Exception {
    sut.readMetadata();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_deleted_component() throws Exception {
    sut.readDeletedComponentIssues(666);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() throws Exception {
    sut.readComponent(666);
  }

  @Test
  public void empty_list_if_no_measure_found() throws Exception {
    assertThat(sut.readComponentMeasures(666)).isEmpty();
  }

  @Test
  public void null_if_no_scm_found() throws Exception {
    assertThat(sut.readComponentScm(666)).isNull();
  }

  @Test
  public void empty_list_if_no_duplication_found() throws Exception {
    assertThat(sut.readComponentDuplications(123)).isEmpty();
  }

  @Test
  public void empty_list_if_no_symbol_found() throws Exception {
    assertThat(sut.readComponentSymbols(123)).isEmpty();
  }

  @Test
  public void empty_list_if_no_highlighting_found() throws Exception {
    assertThat(sut.readComponentSyntaxHighlighting(123)).isEmpty();
  }

  @Test
  public void empty_list_if_no_coverage_found() throws Exception {
    assertThat(sut.readFileCoverage(123)).isEmpty();
  }

  /**
   * no file if no issues
   */
  @Test
  public void empty_list_if_no_issue_found() throws Exception {
    assertThat(sut.readComponentIssues(666)).isEmpty();
  }

}
