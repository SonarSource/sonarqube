/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.steps;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.platform.SonarQubeVersion;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class WriteMetadataStepTest {

  private static final String PROJECT_KEY = "project_key";
  private static final String PROJECT_UUID = "project_uuid";
  private static final long NOW = 123L;

  System2 system2 = spy(System2.INSTANCE);
  FakeDumpWriter dumpWriter = new FakeDumpWriter();
  MutableProjectHolderImpl projectHolder = new MutableProjectHolderImpl();
  Version sqVersion = Version.create(6, 0);
  WriteMetadataStep underTest = new WriteMetadataStep(system2, dumpWriter, projectHolder,
      new SonarQubeVersion(sqVersion));

  @Test
  public void write_metadata() {
    when(system2.now()).thenReturn(NOW);
    ProjectDto dto = new ProjectDto().setKey(PROJECT_KEY).setUuid(PROJECT_UUID);
    projectHolder.setProjectDto(dto);
    underTest.execute(new TestComputationStepContext());

    ProjectDump.Metadata metadata = dumpWriter.getMetadata();
    assertThat(metadata.getProjectKey()).isEqualTo(PROJECT_KEY);
    assertThat(metadata.getProjectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(metadata.getSonarqubeVersion()).isEqualTo(sqVersion.toString());
    assertThat(metadata.getDumpDate()).isEqualTo(NOW);
  }

  @Test
  public void getDescription_is_defined() {
    assertThat(underTest.getDescription()).isNotEmpty();
  }

}
