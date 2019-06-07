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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component.ComponentType;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultBranchImplTest {
  private static final String PROJECT_KEY = "P";
  private static final ScannerReport.Component FILE = ScannerReport.Component.newBuilder().setType(ComponentType.FILE).setProjectRelativePath("src/Foo.js").build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void default_branch_represents_the_project() {
    DefaultBranchImpl branch = new DefaultBranchImpl();

    assertThat(branch.isMain()).isTrue();
    assertThat(branch.getType()).isEqualTo(BranchType.LONG);
    assertThat(branch.getName()).isEqualTo(BranchDto.DEFAULT_MAIN_BRANCH_NAME);
    assertThat(branch.supportsCrossProjectCpd()).isTrue();

    assertThat(branch.generateKey(PROJECT_KEY, null)).isEqualTo("P");
    assertThat(branch.generateKey(PROJECT_KEY, FILE.getProjectRelativePath())).isEqualTo("P:src/Foo.js");
  }
}
