/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.source;

import java.io.File;
import java.util.NoSuchElementException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.protocol.output.FileStructure;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReportIteratorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File file;
  private ReportIterator<ScannerReport.LineCoverage> underTest;

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    FileStructure fileStructure = new FileStructure(dir);
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);

    writer.writeComponentCoverage(1, newArrayList(
      ScannerReport.LineCoverage.newBuilder()
        .setLine(1)
        .build()));

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
    underTest = new ReportIterator<>(file, ScannerReport.LineCoverage.parser());
    assertThat(underTest.next().getLine()).isOne();
  }

  @Test
  public void do_not_fail_when_calling_has_next_with_iterator_already_closed() {
    underTest = new ReportIterator<>(file, ScannerReport.LineCoverage.parser());
    assertThat(underTest.next().getLine()).isOne();
    assertThat(underTest.hasNext()).isFalse();

    underTest.close();
    assertThat(underTest.hasNext()).isFalse();
  }

  @Test
  public void test_error() {
    underTest = new ReportIterator<>(file, ScannerReport.LineCoverage.parser());
    underTest.next();

    assertThatThrownBy(() -> {
      // fail !
      underTest.next();
    })
      .isInstanceOf(NoSuchElementException.class);
  }

}
