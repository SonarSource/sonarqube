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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.output.component.ReportComponent;
import org.sonar.batch.protocol.output.component.ReportComponents;
import org.sonar.batch.protocol.output.issue.ReportIssue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class ReportHelperTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void createAndRead() throws IOException {
    ReportHelper helper = ReportHelper.create(temp.newFolder());

    helper.saveComponents(new ReportComponents().setRoot(new ReportComponent().setBatchId(1L)));

    helper.saveIssues(1L, Arrays.asList(new ReportIssue().setRuleKey("foo", "bar")));

    assertThat(new File(helper.reportRootDir(), "components.json")).exists();
    assertThat(new File(helper.reportRootDir(), "1/issues-1.json")).exists();

    assertThat(helper.getComponents().root().batchId()).isEqualTo(1L);
    assertThat(helper.getIssues(1L)).hasSize(1);
  }

}
