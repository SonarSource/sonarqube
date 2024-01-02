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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Date;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentImpl;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.project.CreationMethod;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

public class AnticipatedTransitionRepositoryImplTest {

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();

  private final AnticipatedTransitionRepository underTest = new AnticipatedTransitionRepositoryImpl(dbClient);

  @Before
  public void cleanUpDb() {

  }

  @Test
  public void giveAnticipatedTransitionsForFile_shouldBeReturnedCorrectly() {
    //given
    String projectKey = "projectKey1";
    String projectUuid = "projectUuid1";
    dbClient.projectDao().insert(db.getSession(), getProjectDto(projectUuid, projectKey));

    insertAnticipatedTransition(projectUuid, "file1.js");
    insertAnticipatedTransition(projectUuid, "file2.js");
    insertAnticipatedTransition(projectUuid, "file2.js");

    db.getSession().commit();

    String componentUuid = "componentUuid";
    Component file = getFileComponent(componentUuid, projectKey, "file1.js");
    var anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(1);

    file = getFileComponent(componentUuid, projectKey, "file2.js");
    anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(2);

    file = getFileComponent(componentUuid, projectKey, "file3.js");
    anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).isEmpty();
  }

  @Test
  public void giveProjectBranchAvailable_projectUuidShouldBeCalculatedFromThere() {
    //given
    String projectKey = "projectKey2";
    String projectUuid = "projectUuid2";
    String mainFile = "file1.js";
    dbClient.projectDao().insert(db.getSession(), getProjectDto(projectUuid, projectKey));

    BranchDto branchDto = getBranchDto(projectUuid, "branch");
    dbClient.branchDao().insert(db.getSession(), branchDto);

    ComponentDto fileDto = getComponentDto(projectKey + ":" + mainFile, branchDto.getUuid());
    dbClient.componentDao().insertWithAudit(db.getSession(), fileDto);

    insertAnticipatedTransition(projectUuid, mainFile);
    insertAnticipatedTransition(projectUuid, "file2.js");
    insertAnticipatedTransition(projectUuid, "file2.js");

    db.getSession().commit();

    Component file = getFileComponent(fileDto.uuid(), projectKey, mainFile);
    var anticipatedTransitions = underTest.getAnticipatedTransitionByComponent(file);
    assertThat(anticipatedTransitions).hasSize(1);
  }

  private void insertAnticipatedTransition(String projectUuid, String filename) {
    var anticipatedTransition = getAnticipatedTransition(projectUuid, filename);
    dbClient.anticipatedTransitionDao().insert(db.getSession(), anticipatedTransition);
  }

  private ComponentDto getComponentDto(String componentKey, String branchUuid) {
    ComponentDto componentDto = new ComponentDto();
    componentDto.setQualifier("FIL")
      .setUuid(Uuids.createFast())
      .setKey(componentKey)
      .setBranchUuid(branchUuid)
      .setUuidPath(Uuids.createFast());
    return componentDto;
  }


  private BranchDto getBranchDto(String projectUuid, String key) {
    BranchDto branchDto = new BranchDto();
    branchDto.setProjectUuid(projectUuid)
      .setUuid(Uuids.createFast())
      .setIsMain(true)
      .setBranchType(BranchType.BRANCH)
      .setKey(key);
    return branchDto;
  }

  private ProjectDto getProjectDto(String projectUuid, String projectKey) {
    ProjectDto projectDto = new ProjectDto();
    projectDto.setKey(projectKey);
    projectDto.setUuid(projectUuid);
    projectDto.setQualifier("TRK");
    projectDto.setName("project");
    projectDto.setCreationMethod(CreationMethod.LOCAL_API);
    return projectDto;
  }

  private Component getFileComponent(String componenUuid, String projectKey, String filename) {
    return ComponentImpl.builder(FILE)
      .setUuid(componenUuid)
      .setKey(String.format("%s:%s", projectKey, filename))
      .setName(filename)
      .setStatus(Component.Status.ADDED)
      .setShortName(filename)
      .setReportAttributes(mock(ReportAttributes.class)).build();
  }

  private AnticipatedTransitionDto getAnticipatedTransition(String projectUuid, String filename) {
    return new AnticipatedTransitionDto(Uuids.createFast(), projectUuid, "admin", "wontfix", null, null, null, null, "rule:key", filename, (new Date()).getTime());
  }

}
