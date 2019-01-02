/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.scanner.protocol.output.ScannerReport;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueRelocationToRootTest {
  @Rule
  public BatchReportReaderRule reader = new BatchReportReaderRule();
  private IssueRelocationToRoot underTest = new IssueRelocationToRoot(reader);

  private ScannerReport.Component root;
  private ScannerReport.Component module;
  private ScannerReport.Component directory;

  @Before
  public void createComponents() {
    root = ScannerReport.Component.newBuilder()
      .setType(ScannerReport.Component.ComponentType.PROJECT)
      .setRef(1)
      .setStatus(ScannerReport.Component.FileStatus.UNAVAILABLE)
      .setKey("PROJECT")
      .addChildRef(2)
      .build();
    module = ScannerReport.Component.newBuilder()
      .setType(ScannerReport.Component.ComponentType.MODULE)
      .setRef(2)
      .setStatus(ScannerReport.Component.FileStatus.UNAVAILABLE)
      .setKey("PROJECT:MODULE")
      .addChildRef(3)
      .build();
    directory = ScannerReport.Component.newBuilder()
      .setType(ScannerReport.Component.ComponentType.DIRECTORY)
      .setRef(3)
      .setStatus(ScannerReport.Component.FileStatus.UNAVAILABLE)
      .setKey("PROJECT:MODULE:DIRECTORY")
      .addChildRef(4)
      .build();

    reader.putComponent(root);
    reader.putComponent(module);
    reader.putComponent(directory);
  }

  private void createIssues() {
    reader.putIssues(2, Collections.singletonList(ScannerReport.Issue.newBuilder().setRuleKey("module_issue").build()));
    reader.putIssues(3, Collections.singletonList(ScannerReport.Issue.newBuilder().setRuleKey("directory_issue").build()));
  }

  @Test
  public void move_module_and_directory_issues() {
    createComponents();
    createIssues();

    underTest.relocate(root, module);
    underTest.relocate(root, directory);

    assertThat(underTest.getMovedIssues()).hasSize(2);
    assertThat(underTest.getMovedIssues()).extracting(ScannerReport.Issue::getRuleKey).containsOnly("module_issue", "directory_issue");
  }

  @Test
  public void do_nothing_if_no_issues() {
    createComponents();
    underTest.relocate(root, module);
    underTest.relocate(root, directory);

    assertThat(underTest.getMovedIssues()).hasSize(0);
  }

}
