/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.source;

import java.util.Collections;
import org.junit.Test;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class CoverageLineReaderTest {

  @Test
  public void set_coverage() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setHits(true)
      .setCoveredConditions(2)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.getLineHits()).isEqualTo(1);
    assertThat(lineBuilder.getConditions()).isEqualTo(10);
    assertThat(lineBuilder.getCoveredConditions()).isEqualTo(2);
  }

  // Some tools are only able to report condition coverage
  @Test
  public void set_coverage_only_conditions() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setCoveredConditions(2)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasLineHits()).isFalse();
    assertThat(lineBuilder.getConditions()).isEqualTo(10);
  }

  @Test
  public void set_coverage_on_uncovered_lines() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setHits(false)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasLineHits()).isTrue();
    assertThat(lineBuilder.getLineHits()).isEqualTo(0);
  }

  @Test
  public void nothing_to_do_when_no_coverage_info() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(Collections.<ScannerReport.LineCoverage>emptyList().iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasLineHits()).isFalse();
    assertThat(lineBuilder.hasConditions()).isFalse();
    assertThat(lineBuilder.hasCoveredConditions()).isFalse();
  }

  @Test
  public void nothing_to_do_when_no_coverage_info_for_current_line() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(
      ScannerReport.LineCoverage.newBuilder()
        .setLine(1)
        .setConditions(10)
        .setHits(true)
        .setCoveredConditions(2)
        .build()
    // No coverage info on line 2
    ).iterator());

    DbFileSources.Line.Builder line2Builder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(2);
    computeCoverageLine.read(line2Builder);

    assertThat(line2Builder.hasLineHits()).isFalse();
    assertThat(line2Builder.hasConditions()).isFalse();
    assertThat(line2Builder.hasCoveredConditions()).isFalse();
  }

  @Test
  public void nothing_to_do_when_no_coverage_info_for_next_line() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(
      ScannerReport.LineCoverage.newBuilder()
        .setLine(1)
        .setConditions(10)
        .setHits(true)
        .setCoveredConditions(2)
        .build()
    // No coverage info on line 2
    ).iterator());

    DbFileSources.Data.Builder fileSourceBuilder = DbFileSources.Data.newBuilder();
    DbFileSources.Line.Builder line1Builder = fileSourceBuilder.addLinesBuilder().setLine(1);
    DbFileSources.Line.Builder line2Builder = fileSourceBuilder.addLinesBuilder().setLine(2);
    computeCoverageLine.read(line1Builder);
    computeCoverageLine.read(line2Builder);

    assertThat(line2Builder.hasLineHits()).isFalse();
    assertThat(line2Builder.hasConditions()).isFalse();
    assertThat(line2Builder.hasCoveredConditions()).isFalse();
  }

  @Test
  public void does_not_set_deprecated_coverage_fields() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setHits(true)
      .setCoveredConditions(2)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasDeprecatedUtLineHits()).isFalse();
    assertThat(lineBuilder.hasDeprecatedUtConditions()).isFalse();
    assertThat(lineBuilder.hasDeprecatedUtCoveredConditions()).isFalse();
    assertThat(lineBuilder.hasDeprecatedOverallLineHits()).isFalse();
    assertThat(lineBuilder.hasDeprecatedOverallConditions()).isFalse();
    assertThat(lineBuilder.hasDeprecatedOverallCoveredConditions()).isFalse();
    assertThat(lineBuilder.hasDeprecatedItLineHits()).isFalse();
    assertThat(lineBuilder.hasDeprecatedItConditions()).isFalse();
    assertThat(lineBuilder.hasDeprecatedItCoveredConditions()).isFalse();
  }

}
