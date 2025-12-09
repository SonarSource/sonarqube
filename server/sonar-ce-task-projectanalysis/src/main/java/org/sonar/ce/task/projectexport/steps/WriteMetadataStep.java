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
import org.sonar.api.utils.System2;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.platform.SonarQubeVersion;

public class WriteMetadataStep implements ComputationStep {

  private final System2 system2;
  private final DumpWriter dumpWriter;
  private final ProjectHolder projectHolder;
  private final SonarQubeVersion sonarQubeVersion;

  public WriteMetadataStep(System2 system2, DumpWriter dumpWriter, ProjectHolder projectHolder, SonarQubeVersion sonarQubeVersion) {
    this.system2 = system2;
    this.dumpWriter = dumpWriter;
    this.projectHolder = projectHolder;
    this.sonarQubeVersion = sonarQubeVersion;
  }

  @Override
  public void execute(Context context) {
    dumpWriter.write(ProjectDump.Metadata.newBuilder()
      .setProjectKey(projectHolder.projectDto().getKey())
      .setProjectUuid(projectHolder.projectDto().getUuid())
      .setSonarqubeVersion(sonarQubeVersion.get().toString())
      .setDumpDate(system2.now())
      .build());
  }

  @Override
  public String getDescription() {
    return "Write metadata";
  }
}
