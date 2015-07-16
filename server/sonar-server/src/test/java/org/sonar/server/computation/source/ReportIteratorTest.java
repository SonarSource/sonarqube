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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.protocol.output.FileStructure;

import java.io.File;
import java.util.NoSuchElementException;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class ReportIteratorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File file;

  ReportIterator<BatchReport.Coverage> underTest;

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeComponentCoverage(1, newArrayList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .build()
    ));

    file = new FileStructure(dir).fileFor(FileStructure.Domain.COVERAGES, 1);
  }

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.close();
    }
  }

  @Test
  public void read_report() {
    underTest = new ReportIterator<>(file, BatchReport.Coverage.PARSER);
    assertThat(underTest.next().getLine()).isEqualTo(1);
  }

  @Test
  public void do_not_fail_when_calling_has_next_with_iterator_already_closed() {
    underTest = new ReportIterator<>(file, BatchReport.Coverage.PARSER);
    assertThat(underTest.next().getLine()).isEqualTo(1);
    assertThat(underTest.hasNext()).isFalse();

    underTest.close();
    assertThat(underTest.hasNext()).isFalse();
  }

  @Test(expected = NoSuchElementException.class)
  public void test_error() throws Exception {
    underTest = new ReportIterator<>(file, BatchReport.Coverage.PARSER);
    underTest.next();

    // fail !
    underTest.next();
  }

}
