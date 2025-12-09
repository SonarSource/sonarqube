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
package org.sonar.db.source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.db.component.ComponentTesting.newFileDto;

class FileSourceDaoIT {

  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);

  private final DbSession dbSession = dbTester.getSession();

  private final FileSourceDao underTest = dbTester.getDbClient().fileSourceDao();

  @Test
  void select() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto expected = dbTester.fileSources().insertFileSource(file);

    FileSourceDto fileSourceDto = underTest.selectByFileUuid(dbSession, file.uuid());

    assertThat(fileSourceDto.getBinaryData()).isEqualTo(expected.getBinaryData());
    assertThat(fileSourceDto.getDataHash()).isEqualTo(expected.getDataHash());
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo(expected.getProjectUuid());
    assertThat(fileSourceDto.getFileUuid()).isEqualTo(expected.getFileUuid());
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
    assertThat(fileSourceDto.getRevision()).isEqualTo(expected.getRevision());
    assertThat(fileSourceDto.getLineHashesVersion()).isEqualTo(expected.getLineHashesVersion());
    assertThat(fileSourceDto.getLineHashes()).isEqualTo(expected.getLineHashes());
  }

  @Test
  void insert() {
    FileSourceDto expected = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes(of("LINE1_HASH", "LINE2_HASH"))
      .setSrcHash("FILE2_HASH")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setLineHashesVersion(1)
      .setRevision("123456789");
    underTest.insert(dbSession, expected);
    dbSession.commit();

    FileSourceDto fileSourceDto = underTest.selectByFileUuid(dbSession, expected.getFileUuid());

    assertThat(fileSourceDto.getProjectUuid()).isEqualTo(expected.getProjectUuid());
    assertThat(fileSourceDto.getFileUuid()).isEqualTo(expected.getFileUuid());
    assertThat(fileSourceDto.getBinaryData()).isEqualTo(expected.getBinaryData());
    assertThat(fileSourceDto.getDataHash()).isEqualTo(expected.getDataHash());
    assertThat(fileSourceDto.getRawLineHashes()).isEqualTo(expected.getRawLineHashes());
    assertThat(fileSourceDto.getLineHashes()).isEqualTo(expected.getLineHashes());
    assertThat(fileSourceDto.getLineCount()).isEqualTo(expected.getLineCount());
    assertThat(fileSourceDto.getSrcHash()).isEqualTo(expected.getSrcHash());
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(expected.getUpdatedAt());
    assertThat(fileSourceDto.getRevision()).isEqualTo(expected.getRevision());
  }

  @Test
  void insert_does_not_fail_on_FileSourceDto_with_only_non_nullable_data() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(dbSession, fileSourceDto);
    assertThatNoException().isThrownBy(dbSession::commit);
  }

  @Test
  void selectSourceByFileUuid_reads_source_without_line_hashes() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(dbSession, fileSourceDto);
    dbSession.commit();

    FileSourceDto res = underTest.selectByFileUuid(dbSession, fileSourceDto.getFileUuid());

    assertThat(res.getLineCount()).isZero();
    assertThat(res.getLineHashes()).isEmpty();
  }

  @Test
  void selectLineHashes_does_not_fail_when_lineshashes_is_null() {
    underTest.insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setSrcHash("FILE2_HASH")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    dbSession.commit();

    assertThat(underTest.selectLineHashes(dbSession, "FILE2_UUID")).isEmpty();
  }

  @Test
  void selectLineHashesVersion_returns_without_significant_code_by_default() {
    underTest.insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes(singletonList("hashes"))
      .setSrcHash("FILE2_HASH")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    dbSession.commit();

    assertThat(underTest.selectLineHashesVersion(dbSession, "FILE2_UUID")).isEqualTo(LineHashVersion.WITHOUT_SIGNIFICANT_CODE);
  }

  @Test
  void selectLineHashesVersion_succeeds() {
    underTest.insert(dbSession, new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes(singletonList("hashes"))
      .setSrcHash("FILE2_HASH")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setLineHashesVersion(1)
      .setRevision("123456789"));
    dbSession.commit();

    assertThat(underTest.selectLineHashesVersion(dbSession, "FILE2_UUID")).isEqualTo(LineHashVersion.WITH_SIGNIFICANT_CODE);
  }

  @Test
  void scrollLineHashes_has_no_effect_if_no_uuids() {
    underTest.scrollLineHashes(dbSession, emptySet(), resultContext -> fail("handler should not be called"));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollLineHashes_scrolls_hashes_of_specific_keys(boolean isPrivate) {
    ComponentDto project = isPrivate ? dbTester.components().insertPrivateProject().getMainBranchComponent() :
      dbTester.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource1 = dbTester.fileSources().insertFileSource(file1);
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource2 = dbTester.fileSources().insertFileSource(file2);
    ComponentDto file3 = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource3 = dbTester.fileSources().insertFileSource(file3);

    LineHashesWithKeyDtoHandler handler = scrollLineHashes(file1.uuid());
    assertThat(handler.dtos).hasSize(1);
    verifyLinesHashes(handler, file1, fileSource1);

    handler = scrollLineHashes(file2.uuid());
    assertThat(handler.dtos).hasSize(1);
    verifyLinesHashes(handler, file2, fileSource2);

    handler = scrollLineHashes(file2.uuid(), file1.uuid(), file3.uuid());
    assertThat(handler.dtos).hasSize(3);
    verifyLinesHashes(handler, file1, fileSource1);
    verifyLinesHashes(handler, file2, fileSource2);
    verifyLinesHashes(handler, file3, fileSource3);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollLineHashes_does_not_scroll_hashes_of_component_without_path(boolean isPrivate) {
    ComponentDto project = isPrivate ? dbTester.components().insertPrivateProject().getMainBranchComponent() :
      dbTester.components().insertPublicProject().getMainBranchComponent();
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource1 = dbTester.fileSources().insertFileSource(file1);
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project).setPath(null));
    FileSourceDto fileSource2 = dbTester.fileSources().insertFileSource(file2);

    LineHashesWithKeyDtoHandler handler = scrollLineHashes(file2.uuid(), file1.uuid());
    assertThat(handler.dtos).hasSize(1);
    verifyLinesHashes(handler, file1, fileSource1);
  }

  @Test
  void scrollFileHashes_handles_scrolling_more_than_1000_files() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    List<ComponentDto> files = IntStream.range(0, 1005)
      .mapToObj(i -> {
        ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
        dbTester.fileSources().insertFileSource(file);
        return file;
      })
      .toList();

    Map<String, FileHashesDto> fileSourcesByUuid = new HashMap<>();
    underTest.scrollFileHashesByProjectUuid(dbSession, project.branchUuid(),
      result -> fileSourcesByUuid.put(result.getResultObject().getFileUuid(), result.getResultObject()));

    assertThat(fileSourcesByUuid).hasSize(files.size());
    files.forEach(t -> assertThat(fileSourcesByUuid).containsKey(t.uuid()));
  }

  @Test
  void scrollFileHashes_returns_all_hashes() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto inserted = dbTester.fileSources().insertFileSource(file);

    List<FileHashesDto> fileSources = new ArrayList<>(1);
    underTest.scrollFileHashesByProjectUuid(dbSession, project.branchUuid(), result -> fileSources.add(result.getResultObject()));

    assertThat(fileSources).hasSize(1);
    FileHashesDto fileSource = fileSources.iterator().next();

    assertThat(fileSource.getDataHash()).isEqualTo(inserted.getDataHash());
    assertThat(fileSource.getFileUuid()).isEqualTo(inserted.getFileUuid());
    assertThat(fileSource.getRevision()).isEqualTo(inserted.getRevision());
    assertThat(fileSource.getSrcHash()).isEqualTo(inserted.getSrcHash());
    assertThat(fileSource.getLineHashesVersion()).isEqualTo(inserted.getLineHashesVersion());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void scrollLineHashes_handles_scrolling_more_than_1000_files(boolean isPrivate) {
    ComponentDto project = isPrivate ? dbTester.components().insertPrivateProject().getMainBranchComponent() :
      dbTester.components().insertPublicProject().getMainBranchComponent();
    List<ComponentDto> files = IntStream.range(0, 1005)
      .mapToObj(i -> {
        ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
        dbTester.fileSources().insertFileSource(file);
        return file;
      })
      .toList();

    LineHashesWithKeyDtoHandler handler = new LineHashesWithKeyDtoHandler();
    underTest.scrollLineHashes(dbSession, files.stream().map(ComponentDto::uuid).collect(Collectors.toSet()), handler);

    assertThat(handler.dtos).hasSize(files.size());
    files.forEach(t -> assertThat(handler.getByUuid(t.uuid())).isPresent());
  }

  private LineHashesWithKeyDtoHandler scrollLineHashes(String... uuids) {
    LineHashesWithKeyDtoHandler handler = new LineHashesWithKeyDtoHandler();
    underTest.scrollLineHashes(dbSession, ImmutableSet.copyOf(uuids), handler);
    return handler;
  }

  private static void verifyLinesHashes(LineHashesWithKeyDtoHandler handler, ComponentDto file, FileSourceDto fileSource) {
    LineHashesWithUuidDto dto = handler.getByUuid(file.uuid()).get();
    assertThat(dto.getPath()).isEqualTo(file.path());
    assertThat(dto.getRawLineHashes()).isEqualTo(fileSource.getRawLineHashes());
    assertThat(dto.getLineHashes()).isEqualTo(fileSource.getLineHashes());
  }

  private static final class LineHashesWithKeyDtoHandler implements ResultHandler<LineHashesWithUuidDto> {
    private final List<LineHashesWithUuidDto> dtos = new ArrayList<>();

    @Override
    public void handleResult(ResultContext<? extends LineHashesWithUuidDto> resultContext) {
      dtos.add(resultContext.getResultObject());
    }

    public Optional<LineHashesWithUuidDto> getByUuid(String uuid) {
      return dtos.stream()
        .filter(t -> uuid.equals(t.getUuid()))
        .findAny();
    }
  }

  @Test
  void update() {
    ComponentDto project = dbTester.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto expected = dbTester.fileSources().insertFileSource(file);

    underTest.update(dbSession, new FileSourceDto()
      .setUuid(expected.getUuid())
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE1_UUID")
      .setBinaryData("updated data".getBytes())
      .setDataHash("NEW_DATA_HASH")
      .setSrcHash("NEW_FILE_HASH")
      .setLineHashes(singletonList("NEW_LINE_HASHES"))
      .setUpdatedAt(1500000000002L)
      .setLineHashesVersion(1)
      .setRevision("987654321"));
    dbSession.commit();

    FileSourceDto fileSourceDto = underTest.selectByFileUuid(dbSession, file.uuid());

    assertThat(fileSourceDto.getProjectUuid()).isEqualTo(expected.getProjectUuid());
    assertThat(fileSourceDto.getFileUuid()).isEqualTo(expected.getFileUuid());
    assertThat(fileSourceDto.getBinaryData()).isEqualTo("updated data".getBytes());
    assertThat(fileSourceDto.getDataHash()).isEqualTo("NEW_DATA_HASH");
    assertThat(fileSourceDto.getRawLineHashes()).isEqualTo("NEW_LINE_HASHES");
    assertThat(fileSourceDto.getLineHashes()).isEqualTo(singletonList("NEW_LINE_HASHES"));
    assertThat(fileSourceDto.getLineCount()).isOne();
    assertThat(fileSourceDto.getSrcHash()).isEqualTo("NEW_FILE_HASH");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(1500000000002L);
    assertThat(fileSourceDto.getRevision()).isEqualTo("987654321");
  }

  @Test
  void update_to_no_line_hashes() {
    ImmutableList<String> lineHashes = of("a", "b", "c");
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setUuid(Uuids.createFast())
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setLineHashes(lineHashes)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(dbSession, fileSourceDto);
    dbSession.commit();

    FileSourceDto resBefore = underTest.selectByFileUuid(dbSession, fileSourceDto.getFileUuid());
    assertThat(resBefore.getLineCount()).isEqualTo(lineHashes.size());
    assertThat(resBefore.getLineHashes()).isEqualTo(lineHashes);

    fileSourceDto.setUuid(resBefore.getUuid());
    fileSourceDto.setLineHashes(emptyList());
    underTest.update(dbSession, fileSourceDto);
    dbSession.commit();

    FileSourceDto res = underTest.selectByFileUuid(dbSession, fileSourceDto.getFileUuid());
    assertThat(res.getLineHashes()).isEmpty();
    assertThat(res.getLineCount()).isOne();
  }
}
