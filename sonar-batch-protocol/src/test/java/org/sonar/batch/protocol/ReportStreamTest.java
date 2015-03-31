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

package org.sonar.batch.protocol;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.batch.protocol.output.FileStructure;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

public class ReportStreamTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File file;

  ReportStream<BatchReport.Coverage> sut;

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    BatchReportWriter writer = new BatchReportWriter(dir);

    writer.writeFileCoverage(1, Arrays.asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .build()
    ));

    file = new FileStructure(dir).fileFor(FileStructure.Domain.COVERAGE, 1);
  }

  @After
  public void tearDown() throws Exception {
    IOUtils.closeQuietly(sut);
  }

  @Test
  public void read_report() throws Exception {
    sut = new ReportStream<>(file, BatchReport.Coverage.PARSER);
    assertThat(sut).hasSize(1);
    sut.close();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_to_get_iterator_twice() throws Exception {
    sut = new ReportStream<>(file, BatchReport.Coverage.PARSER);
    sut.iterator();

    // Fail !
    sut.iterator();
  }

  @Test(expected = NoSuchElementException.class)
  public void fail_to_get_next_when_no_next() throws Exception {
    sut = new ReportStream<>(file, BatchReport.Coverage.PARSER);
    Iterator<BatchReport.Coverage> iterator = sut.iterator();
    // Get first element
    iterator.next();

    // Fail !
    iterator.next();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void fail_to_remove() throws Exception {
    sut = new ReportStream<>(file, BatchReport.Coverage.PARSER);
    Iterator<BatchReport.Coverage> iterator = sut.iterator();

    // Fail !
    iterator.remove();
  }

}
