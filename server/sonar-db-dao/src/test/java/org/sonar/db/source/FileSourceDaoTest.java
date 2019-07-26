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
package org.sonar.db.source;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class FileSourceDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession dbSession = dbTester.getSession();

  private FileSourceDao underTest = dbTester.getDbClient().fileSourceDao();

  @Test
  public void select() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
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
  public void select_line_hashes() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto expected = dbTester.fileSources().insertFileSource(file);

    ReaderToStringConsumer fn = new ReaderToStringConsumer();
    underTest.readLineHashesStream(dbSession, expected.getFileUuid(), fn);

    assertThat(fn.result).isEqualTo(expected.getLineHashes().isEmpty() ? null : String.join("\n", expected.getLineHashes()));
  }

  @Test
  public void no_line_hashes_on_unknown_file() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    dbTester.fileSources().insertFileSource(file);

    ReaderToStringConsumer fn = new ReaderToStringConsumer();
    underTest.readLineHashesStream(dbSession, "unknown", fn);

    assertThat(fn.result).isNull();
  }

  @Test
  public void insert() {
    FileSourceDto expected = new FileSourceDto()
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
  public void insert_does_not_fail_on_FileSourceDto_with_only_non_nullable_data() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(dbSession, fileSourceDto);
    dbSession.commit();
  }

  @Test
  public void selectSourceByFileUuid_reads_source_without_line_hashes() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(dbSession, fileSourceDto);
    dbSession.commit();

    FileSourceDto res = underTest.selectByFileUuid(dbSession, fileSourceDto.getFileUuid());

    assertThat(res.getLineCount()).isEqualTo(0);
    assertThat(res.getLineHashes()).isEmpty();
  }

  @Test
  public void selectLineHashes_does_not_fail_when_lineshashes_is_null() {
    underTest.insert(dbSession, new FileSourceDto()
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
  public void selectLineHashesVersion_returns_without_significant_code_by_default() {
    underTest.insert(dbSession, new FileSourceDto()
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
  public void selectLineHashesVersion_succeeds() {
    underTest.insert(dbSession, new FileSourceDto()
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
  public void readLineHashesStream_does_not_fail_when_lineshashes_is_null() {
    underTest.insert(dbSession, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setSrcHash("FILE2_HASH")
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    dbSession.commit();

    boolean[] flag = {false};
    underTest.readLineHashesStream(dbSession, "FILE2_UUID", new Consumer<Reader>() {
      @Override
      public void accept(@Nullable Reader input) {
        fail("function must never been called since there is no data to read");
        flag[0] = true;
      }
    });
    assertThat(flag[0]).isFalse();
  }

  @Test
  public void scrollLineHashes_has_no_effect_if_no_uuids() {
    underTest.scrollLineHashes(dbSession, emptySet(), resultContext -> fail("handler should not be called"));
  }

  @Test
  public void scrollLineHashes_scrolls_hashes_of_specific_keys() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = new Random().nextBoolean() ? dbTester.components().insertPrivateProject(organization) : dbTester.components().insertPublicProject(organization);
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

  @Test
  public void scrollLineHashes_does_not_scroll_hashes_of_component_without_path() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = new Random().nextBoolean() ? dbTester.components().insertPrivateProject(organization) : dbTester.components().insertPublicProject(organization);
    ComponentDto file1 = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto fileSource1 = dbTester.fileSources().insertFileSource(file1);
    ComponentDto file2 = dbTester.components().insertComponent(newFileDto(project).setPath(null));
    FileSourceDto fileSource2 = dbTester.fileSources().insertFileSource(file2);

    LineHashesWithKeyDtoHandler handler = scrollLineHashes(file2.uuid(), file1.uuid());
    assertThat(handler.dtos).hasSize(1);
    verifyLinesHashes(handler, file1, fileSource1);
  }

  @Test
  public void scrollLineHashes_handles_scrolling_more_than_1000_files() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = new Random().nextBoolean() ? dbTester.components().insertPrivateProject(organization) : dbTester.components().insertPublicProject(organization);
    List<ComponentDto> files = IntStream.range(0, 1001 + new Random().nextInt(5))
      .mapToObj(i -> {
        ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
        dbTester.fileSources().insertFileSource(file);
        return file;
      })
      .collect(Collectors.toList());

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
  public void update() {
    OrganizationDto organization = dbTester.organizations().insert();
    ComponentDto project = dbTester.components().insertPrivateProject(organization);
    ComponentDto file = dbTester.components().insertComponent(newFileDto(project));
    FileSourceDto expected = dbTester.fileSources().insertFileSource(file);

    underTest.update(dbSession, new FileSourceDto()
      .setId(expected.getId())
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
    assertThat(fileSourceDto.getLineCount()).isEqualTo(1);
    assertThat(fileSourceDto.getSrcHash()).isEqualTo("NEW_FILE_HASH");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(1500000000002L);
    assertThat(fileSourceDto.getRevision()).isEqualTo("987654321");
  }

  @Test
  public void update_to_no_line_hashes() {
    ImmutableList<String> lineHashes = of("a", "b", "c");
    FileSourceDto fileSourceDto = new FileSourceDto()
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

    fileSourceDto.setId(resBefore.getId());
    fileSourceDto.setLineHashes(emptyList());
    underTest.update(dbSession, fileSourceDto);
    dbSession.commit();

    FileSourceDto res = underTest.selectByFileUuid(dbSession, fileSourceDto.getFileUuid());
    assertThat(res.getLineHashes()).isEmpty();
    assertThat(res.getLineCount()).isEqualTo(1);
  }

  private static class ReaderToStringConsumer implements Consumer<Reader> {

    String result = null;

    @Override
    public void accept(Reader input) {
      try {
        result = IOUtils.toString(input);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
