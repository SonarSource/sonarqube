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
package org.sonar.db.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.audit.NoOpAuditPersister;
import org.sonar.db.source.FileSourceDto;

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.resources.Qualifiers.FILE;
import static org.sonar.api.resources.Qualifiers.UNIT_TEST_FILE;

class ScrollForFileMoveComponentDaoIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = db.getSession();
  private final ComponentDao underTest = new ComponentDao(new NoOpAuditPersister());

  @Test
  void scrollAllFilesForFileMove_has_no_effect_if_project_does_not_exist() {
    String nonExistingProjectUuid = secure().nextAlphabetic(10);

    underTest.scrollAllFilesForFileMove(dbSession, nonExistingProjectUuid, resultContext -> Assertions.fail("handler should not be " +
      "called"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_has_no_effect_if_project_has_no_file(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultContext -> Assertions.fail("handler should not be called"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_ignores_files_with_null_path(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    ComponentAndSource file = insertFileAndSource(project, FILE);
    ComponentAndSource ut = insertFileAndSource(project, UNIT_TEST_FILE);
    ComponentDto fileNoPath = db.components().insertComponent(ComponentTesting.newFileDto(project).setPath(null).setQualifier(FILE));
    db.fileSources().insertFileSource(fileNoPath);
    ComponentDto utNoPath =
      db.components().insertComponent(ComponentTesting.newFileDto(project).setPath(null).setQualifier(UNIT_TEST_FILE));
    db.fileSources().insertFileSource(utNoPath);
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(2);
    verifyFileMoveRowDto(resultHandler, file);
    verifyFileMoveRowDto(resultHandler, ut);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_ignores_files_without_source(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    ComponentAndSource file = insertFileAndSource(project, FILE);
    ComponentAndSource ut = insertFileAndSource(project, UNIT_TEST_FILE);
    ComponentDto fileNoSource = db.components().insertComponent(ComponentTesting.newFileDto(project).setPath(null).setQualifier(FILE));
    ComponentDto utNoSource =
      db.components().insertComponent(ComponentTesting.newFileDto(project).setPath(null).setQualifier(UNIT_TEST_FILE));
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(2);
    verifyFileMoveRowDto(resultHandler, file);
    verifyFileMoveRowDto(resultHandler, ut);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_scrolls_files_of_project(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto dir1 = db.components().insertComponent(ComponentTesting.newDirectory(project, "path"));
    ComponentDto dir2 = db.components().insertComponent(ComponentTesting.newDirectory(dir1, "path2"));
    ComponentAndSource file1 = insertFileAndSource(project, FILE);
    ComponentAndSource file2 = insertFileAndSource(dir1, FILE);
    ComponentAndSource file3 = insertFileAndSource(dir2, FILE);
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(3);
    verifyFileMoveRowDto(resultHandler, file1);
    verifyFileMoveRowDto(resultHandler, file2);
    verifyFileMoveRowDto(resultHandler, file3);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_scrolls_large_number_of_files_and_uts(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    List<ComponentAndSource> files = IntStream.range(0, 500)
      .mapToObj(i -> {
        String qualifier = isPrivate ? FILE : UNIT_TEST_FILE;
        ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(project).setKey("f_" + i).setQualifier(qualifier));
        FileSourceDto fileSource = db.fileSources().insertFileSource(file);
        return new ComponentAndSource(file, fileSource);
      })
      .toList();
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(files.size());
    files.forEach(f -> verifyFileMoveRowDto(resultHandler, f));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_scrolls_unit_tests_of_project(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    ComponentAndSource ut = insertFileAndSource(project, UNIT_TEST_FILE);
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(1);
    verifyFileMoveRowDto(resultHandler, ut);
  }

  @ParameterizedTest
  @MethodSource("branchTypes")
  void scrollAllFilesForFileMove_scrolls_files_and_unit_tests_of_branch(BranchType branchType, boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto branch = db.components().insertProjectBranch(project, t -> t.setBranchType(branchType));
    ComponentAndSource file = insertFileAndSource(branch, FILE);
    ComponentAndSource ut = insertFileAndSource(branch, UNIT_TEST_FILE);
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, branch.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(2);
    verifyFileMoveRowDto(resultHandler, file);
    verifyFileMoveRowDto(resultHandler, ut);
  }

  private static Stream<Arguments> branchTypes() {
    return Stream.of(
      Arguments.of(BranchType.BRANCH, true),
      Arguments.of(BranchType.BRANCH, false),
      Arguments.of(BranchType.PULL_REQUEST, true),
      Arguments.of(BranchType.PULL_REQUEST, false)
    );
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollAllFilesForFileMove_ignores_non_file_and_non_ut_component_with_source(boolean isPrivate) {
    ComponentDto project = isPrivate ? db.components().insertPrivateProject().getMainBranchComponent() :
      db.components().insertPublicProject().getMainBranchComponent();
    db.fileSources().insertFileSource(project);
    ComponentDto dir = db.components().insertComponent(ComponentTesting.newDirectory(project, "foo"));
    db.fileSources().insertFileSource(dir);
    ComponentAndSource file = insertFileAndSource(project, FILE);
    ComponentAndSource ut = insertFileAndSource(dir, UNIT_TEST_FILE);
    ComponentDto portfolio = isPrivate ? db.components().insertPublicPortfolio() : db.components().insertPrivatePortfolio();
    db.fileSources().insertFileSource(portfolio);
    ComponentDto subView = db.components().insertSubView(portfolio);
    db.fileSources().insertFileSource(subView);
    ComponentDto application = isPrivate ? db.components().insertPrivateApplication().getMainBranchComponent() :
      db.components().insertPublicApplication().getMainBranchComponent();
    db.fileSources().insertFileSource(application);
    RecordingResultHandler resultHandler = new RecordingResultHandler();

    underTest.scrollAllFilesForFileMove(dbSession, project.uuid(), resultHandler);
    underTest.scrollAllFilesForFileMove(dbSession, portfolio.uuid(), resultHandler);
    underTest.scrollAllFilesForFileMove(dbSession, application.uuid(), resultHandler);

    assertThat(resultHandler.dtos).hasSize(2);
    verifyFileMoveRowDto(resultHandler, file);
    verifyFileMoveRowDto(resultHandler, ut);
  }

  private static final class RecordingResultHandler implements ResultHandler<FileMoveRowDto> {
    List<FileMoveRowDto> dtos = new ArrayList<>();

    @Override
    public void handleResult(ResultContext<? extends FileMoveRowDto> resultContext) {
      dtos.add(resultContext.getResultObject());
    }

    private java.util.Optional<FileMoveRowDto> getByUuid(String uuid) {
      return dtos.stream().filter(t -> t.getUuid().equals(uuid)).findAny();
    }

  }

  private ComponentAndSource insertFileAndSource(ComponentDto parent, String qualifier) {
    ComponentDto file = db.components().insertComponent(ComponentTesting.newFileDto(parent).setQualifier(qualifier));
    FileSourceDto fileSource = db.fileSources().insertFileSource(file);
    return new ComponentAndSource(file, fileSource);
  }

  private record ComponentAndSource(ComponentDto component, FileSourceDto source) {
  }

  private static void verifyFileMoveRowDto(RecordingResultHandler resultHander, ComponentAndSource componentAndSource) {
    FileMoveRowDto dto = resultHander.getByUuid(componentAndSource.component.uuid()).get();
    assertThat(dto.getKey()).isEqualTo(componentAndSource.component.getKey());
    assertThat(dto.getUuid()).isEqualTo(componentAndSource.component.uuid());
    assertThat(dto.getPath()).isEqualTo(componentAndSource.component.path());
    assertThat(dto.getLineCount()).isEqualTo(componentAndSource.source.getLineCount());
  }

}
