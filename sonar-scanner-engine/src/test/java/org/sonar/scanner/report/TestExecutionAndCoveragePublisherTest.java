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
package org.sonar.scanner.report;

import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.scanner.protocol.output.ScannerReportWriter;
import org.sonar.scanner.scan.branch.BranchConfiguration;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TestExecutionAndCoveragePublisherTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void do_nothing_for_short_living_branches() throws IOException {
    BranchConfiguration branchConfiguration = mock(BranchConfiguration.class);
    when(branchConfiguration.isShortLivingBranch()).thenReturn(true);
    InputComponentStore componentStore = mock(InputComponentStore.class);
    TestExecutionAndCoveragePublisher publisher = new TestExecutionAndCoveragePublisher(componentStore, null, branchConfiguration);
    File outputDir = temp.newFolder();
    ScannerReportWriter writer = new ScannerReportWriter(outputDir);

    publisher.publish(writer);

    verifyZeroInteractions(componentStore);
  }

}
