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
package org.sonar.ce.task.projectexport.file;

import com.sonarsource.governance.projectdump.protobuf.ProjectDump;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectexport.component.ComponentRepositoryImpl;
import org.sonar.ce.task.projectexport.component.MutableComponentRepository;
import org.sonar.ce.task.projectexport.steps.DumpElement;
import org.sonar.ce.task.projectexport.steps.FakeDumpWriter;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.MyBatis;
import org.sonar.db.source.FileSourceDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class ExportLineHashesStepIT {
  private static final String PROJECT_MASTER_UUID = "project uuid";
  private static final String PROJECT_BRANCH_UUID = "branch-uuid";
  private static final String FILE_UUID = "file uuid";
  private static final String FILE_UUID_2 = "file-2-uuid";

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private final DbSession dbSession = dbClient.openSession(false);
  private final FakeDumpWriter dumpWriter = new FakeDumpWriter();
  private final MutableComponentRepository componentRepository = new ComponentRepositoryImpl();
  private final ExportLineHashesStep underTest = new ExportLineHashesStep(dbClient, dumpWriter, componentRepository);

  @Before
  public void before() {
    logTester.setLevel(Level.DEBUG);
  }

  @After
  public void tearDown() {
    dbSession.close();
  }

  @Test
  public void getDescription_is_set() {
    assertThat(underTest.getDescription()).isEqualTo("Export line hashes");
  }

  @Test
  public void execute_does_not_create_a_session_when_there_is_no_file_in_ComponentRepository() {
    DbClient spy = spy(dbClient);
    new ExportLineHashesStep(spy, dumpWriter, componentRepository)
      .execute(new TestComputationStepContext());

    verifyNoInteractions(spy);

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINES_HASHES)).isEmpty();
  }

  @Test
  public void execute_relies_only_on_file_uuid_and_does_not_check_project_uuid() {
    componentRepository.register(1, FILE_UUID, true);

    insertFileSource(createDto(FILE_UUID, "blabla", "A"));
    insertFileSource(createDto(FILE_UUID_2, "blabla", "B"));

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINES_HASHES)).hasSize(1);
  }

  @Test
  public void execute_maps_ref_of_component_and_hashes_from_fileSources() {
    int fileRef = 984615;
    componentRepository.register(fileRef, FILE_UUID, true);
    FileSourceDto dto = createDto(FILE_UUID, PROJECT_MASTER_UUID, "B");
    insertFileSource(dto);

    underTest.execute(new TestComputationStepContext());

    List<ProjectDump.LineHashes> messages = dumpWriter.getWrittenMessagesOf(DumpElement.LINES_HASHES);
    assertThat(messages).hasSize(1);
    ProjectDump.LineHashes lineHashes = messages.iterator().next();
    assertThat(lineHashes.getHashes()).isEqualTo(dto.getRawLineHashes());
    assertThat(lineHashes.getComponentRef()).isEqualTo(fileRef);
  }

  @Test
  public void execute_does_one_SQL_request_by_1000_items_per_IN_clause() {
    for (int i = 0; i < 2500; i++) {
      componentRepository.register(i, "uuid_" + i, true);
    }

    DbClient spyDbClient = spy(dbClient);
    MyBatis spyMyBatis = spy(dbClient.getMyBatis());
    when(spyDbClient.getMyBatis()).thenReturn(spyMyBatis);
    ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
    doCallRealMethod().when(spyMyBatis).newScrollingSelectStatement(any(DbSession.class), stringCaptor.capture());

    new ExportLineHashesStep(spyDbClient, dumpWriter, componentRepository)
      .execute(new TestComputationStepContext());

    List<String> statements = stringCaptor.getAllValues();
    assertThat(statements).hasSize(3);

    assertThat(statements.get(0).split("\\?")).hasSize(1001);
    assertThat(statements.get(1).split("\\?")).hasSize(1001);
    assertThat(statements.get(2).split("\\?")).hasSize(501);
  }

  @Test
  public void execute_exports_lines_hashes_of_file_sources() {
    componentRepository.register(1, FILE_UUID, true);
    insertFileSource(FILE_UUID, "A");

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINES_HASHES))
      .extracting(ProjectDump.LineHashes::getHashes)
      .containsOnly("A");
  }

  @Test
  public void execute_logs_number_of_filesource_exported_and_export_by_order_of_id() {
    componentRepository.register(1, FILE_UUID, true);
    componentRepository.register(2, "file-2", true);
    componentRepository.register(3, "file-3", true);
    componentRepository.register(4, "file-4", true);

    insertFileSource(createDto(FILE_UUID, PROJECT_MASTER_UUID, "A"));
    insertFileSource(createDto("file-2", PROJECT_MASTER_UUID, "C"));
    insertFileSource(createDto("file-3", PROJECT_MASTER_UUID, "D"));
    insertFileSource(createDto("file-4", PROJECT_BRANCH_UUID, "E"));

    underTest.execute(new TestComputationStepContext());

    assertThat(dumpWriter.getWrittenMessagesOf(DumpElement.LINES_HASHES))
      .extracting(ProjectDump.LineHashes::getHashes)
      .containsExactly("A", "C", "D", "E");

    assertThat(logTester.logs(Level.DEBUG)).contains("Lines hashes of 4 files exported");
  }

  private FileSourceDto insertFileSource(String fileUuid, String hashes) {
    FileSourceDto dto = createDto(fileUuid, PROJECT_MASTER_UUID, hashes);
    return insertFileSource(dto);
  }

  private FileSourceDto insertFileSource(FileSourceDto dto) {
    dbClient.fileSourceDao().insert(dbSession, dto);
    dbSession.commit();
    return dto;
  }

  private FileSourceDto createDto(String fileUuid, String componentUuid, String hashes) {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setFileUuid(fileUuid)
      .setProjectUuid(componentUuid);
    fileSourceDto.setRawLineHashes(hashes);
    return fileSourceDto;
  }
}
