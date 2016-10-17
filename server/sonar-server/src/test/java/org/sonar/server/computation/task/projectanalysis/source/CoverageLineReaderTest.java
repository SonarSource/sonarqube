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

    assertThat(lineBuilder.getUtLineHits()).isEqualTo(1);
    assertThat(lineBuilder.getUtConditions()).isEqualTo(10);
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

    assertThat(lineBuilder.hasUtLineHits()).isFalse();
    assertThat(lineBuilder.getUtConditions()).isEqualTo(10);
    assertThat(lineBuilder.hasItLineHits()).isFalse();
    assertThat(lineBuilder.hasOverallLineHits()).isFalse();
    assertThat(lineBuilder.hasOverallConditions()).isTrue();
    assertThat(lineBuilder.getOverallConditions()).isEqualTo(10);
  }

  @Test
  public void set_coverage_only_ut() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setConditions(10)
      .setHits(true)
      .setCoveredConditions(2)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.getUtLineHits()).isEqualTo(1);
    assertThat(lineBuilder.getUtConditions()).isEqualTo(10);
    assertThat(lineBuilder.getUtCoveredConditions()).isEqualTo(2);
    assertThat(lineBuilder.hasItLineHits()).isFalse();
    assertThat(lineBuilder.hasItConditions()).isFalse();
    assertThat(lineBuilder.hasItCoveredConditions()).isFalse();
    assertThat(lineBuilder.getOverallLineHits()).isEqualTo(1);
    assertThat(lineBuilder.hasOverallCoveredConditions()).isTrue();
    assertThat(lineBuilder.getOverallCoveredConditions()).isEqualTo(2);
  }

  @Test
  public void set_coverage_on_uncovered_lines() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setHits(false)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasUtLineHits()).isTrue();
    assertThat(lineBuilder.getUtLineHits()).isEqualTo(0);
    assertThat(lineBuilder.hasOverallLineHits()).isTrue();
    assertThat(lineBuilder.getOverallLineHits()).isEqualTo(0);
  }

  @Test
  public void set_overall_line_hits_with_only_ut() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setHits(true)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.getOverallLineHits()).isEqualTo(1);
  }

  @Test
  public void set_overall_line_hits_with_only_it() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setHits(true)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.getOverallLineHits()).isEqualTo(1);
  }

  @Test
  public void set_overall_line_hits_with_ut_and_it() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(newArrayList(ScannerReport.LineCoverage.newBuilder()
      .setLine(1)
      .setHits(true)
      .build()).iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.getOverallLineHits()).isEqualTo(1);
  }

  @Test
  public void nothing_to_do_when_no_coverage_info() {
    CoverageLineReader computeCoverageLine = new CoverageLineReader(Collections.<ScannerReport.LineCoverage>emptyList().iterator());

    DbFileSources.Line.Builder lineBuilder = DbFileSources.Data.newBuilder().addLinesBuilder().setLine(1);
    computeCoverageLine.read(lineBuilder);

    assertThat(lineBuilder.hasUtLineHits()).isFalse();
    assertThat(lineBuilder.hasUtConditions()).isFalse();
    assertThat(lineBuilder.hasItLineHits()).isFalse();
    assertThat(lineBuilder.hasItConditions()).isFalse();
    assertThat(lineBuilder.hasItCoveredConditions()).isFalse();
    assertThat(lineBuilder.hasOverallLineHits()).isFalse();
    assertThat(lineBuilder.hasOverallConditions()).isFalse();
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

    assertThat(line2Builder.hasUtLineHits()).isFalse();
    assertThat(line2Builder.hasUtConditions()).isFalse();
    assertThat(line2Builder.hasItLineHits()).isFalse();
    assertThat(line2Builder.hasItConditions()).isFalse();
    assertThat(line2Builder.hasItCoveredConditions()).isFalse();
    assertThat(line2Builder.hasOverallLineHits()).isFalse();
    assertThat(line2Builder.hasOverallConditions()).isFalse();
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

    assertThat(line2Builder.hasUtLineHits()).isFalse();
    assertThat(line2Builder.hasUtConditions()).isFalse();
    assertThat(line2Builder.hasItLineHits()).isFalse();
    assertThat(line2Builder.hasItConditions()).isFalse();
    assertThat(line2Builder.hasItCoveredConditions()).isFalse();
    assertThat(line2Builder.hasOverallLineHits()).isFalse();
    assertThat(line2Builder.hasOverallConditions()).isFalse();
  }

}
