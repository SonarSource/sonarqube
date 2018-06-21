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
package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.db.component.BranchDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class BranchLoaderTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public AnalysisMetadataHolderRule metadataHolder = new AnalysisMetadataHolderRule();

  @Test
  public void throw_ME_if_both_branch_properties_are_set() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .setDeprecatedBranch("foo")
      .setBranchName("bar")
      .build();

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Properties sonar.branch and sonar.branch.name can't be set together");

    new BranchLoader(metadataHolder).load(metadata);
  }

  @Test
  public void regular_analysis_of_project_is_enabled_if_delegate_is_absent() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .build();

    new BranchLoader(metadataHolder).load(metadata);

    assertThat(metadataHolder.getBranch()).isNotNull();

    Branch branch = metadataHolder.getBranch();
    assertThat(branch.isMain()).isTrue();
    assertThat(branch.getName()).isEqualTo(BranchDto.DEFAULT_MAIN_BRANCH_NAME);
  }

  @Test
  public void default_support_of_branches_is_enabled_if_delegate_is_absent() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .setDeprecatedBranch("foo")
      .build();

    new BranchLoader(metadataHolder).load(metadata);

    assertThat(metadataHolder.getBranch()).isNotNull();

    Branch branch = metadataHolder.getBranch();
    assertThat(branch.isMain()).isTrue();
    assertThat(branch.getName()).isEqualTo("foo");
  }

  @Test
  public void default_support_of_branches_is_enabled_if_delegate_is_present() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder()
      .setDeprecatedBranch("foo")
      .build();

    FakeDelegate delegate = new FakeDelegate();
    new BranchLoader(metadataHolder, delegate).load(metadata);

    assertThat(metadataHolder.getBranch()).isNotNull();

    Branch branch = metadataHolder.getBranch();
    assertThat(branch.isMain()).isTrue();
    assertThat(branch.getName()).isEqualTo("foo");
  }

  private class FakeDelegate implements BranchLoaderDelegate {
    Branch branch = mock(Branch.class);

    @Override
    public void load(ScannerReport.Metadata metadata) {
      metadataHolder.setBranch(branch);
    }
  }
}
