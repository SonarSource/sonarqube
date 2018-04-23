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
package org.sonar.db.source;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.source.FileSourceDto.Type;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class FileSourceDaoTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private DbSession session = dbTester.getSession();

  private FileSourceDao underTest = dbTester.getDbClient().fileSourceDao();

  @Test
  public void select() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    FileSourceDto fileSourceDto = underTest.selectSourceByFileUuid(session, "FILE1_UUID");

    assertThat(fileSourceDto.getBinaryData()).isNotEmpty();
    assertThat(fileSourceDto.getDataHash()).isEqualTo("hash");
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo("PRJ_UUID");
    assertThat(fileSourceDto.getFileUuid()).isEqualTo("FILE1_UUID");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(1500000000000L);
    assertThat(fileSourceDto.getDataType()).isEqualTo(Type.SOURCE);
    assertThat(fileSourceDto.getRevision()).isEqualTo("123456789");
    assertThat(fileSourceDto.getLineHashesVersion()).isEqualTo(0);

  }

  @Test
  public void select_line_hashes() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    ReaderToStringConsumer fn = new ReaderToStringConsumer();
    underTest.readLineHashesStream(dbTester.getSession(), "FILE1_UUID", fn);

    assertThat(fn.result).isEqualTo("ABC\\nDEF\\nGHI");
  }

  @Test
  public void no_line_hashes_on_unknown_file() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    ReaderToStringConsumer fn = new ReaderToStringConsumer();
    underTest.readLineHashesStream(dbTester.getSession(), "unknown", fn);

    assertThat(fn.result).isNull();
  }

  @Test
  public void no_line_hashes_when_only_test_data() {
    dbTester.prepareDbUnit(getClass(), "no_line_hashes_when_only_test_data.xml");

    ReaderToStringConsumer fn = new ReaderToStringConsumer();
    underTest.readLineHashesStream(dbTester.getSession(), "FILE1_UUID", fn);

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
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setLineHashesVersion(1)
      .setRevision("123456789");
    underTest.insert(session, expected);
    session.commit();

    FileSourceDto fileSourceDto = underTest.selectSourceByFileUuid(session, expected.getFileUuid());

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
    underTest.insert(session, fileSourceDto);
    session.commit();
  }

  @Test
  public void selectSourceByFileUuid_reads_source_without_line_hashes() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(session, fileSourceDto);
    session.commit();

    FileSourceDto res = underTest.selectSourceByFileUuid(session, fileSourceDto.getFileUuid());

    assertThat(res.getLineCount()).isEqualTo(0);
    assertThat(res.getLineHashes()).isEmpty();
  }

  @Test
  public void selectTest_reads_test_without_line_hashes() {
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setDataType(Type.TEST)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(session, fileSourceDto);
    session.commit();

    FileSourceDto res = underTest.selectTestByFileUuid(session, fileSourceDto.getFileUuid());

    assertThat(res.getLineCount()).isEqualTo(0);
    assertThat(res.getLineHashes()).isEmpty();
  }

  @Test
  public void selectLineHashes_does_not_fail_when_lineshashes_is_null() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.insert(session, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    session.commit();

    assertThat(underTest.selectLineHashes(dbTester.getSession(), "FILE2_UUID")).isEmpty();
  }

  @Test
  public void selectLineHashesVersion_returns_without_significant_code_by_default() {
    underTest.insert(session, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes(singletonList("hashes"))
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    session.commit();

    assertThat(underTest.selectLineHashesVersion(dbTester.getSession(), "FILE2_UUID")).isEqualTo(LineHashVersion.WITHOUT_SIGNIFICANT_CODE);
  }

  @Test
  public void selectLineHashesVersion_succeeds() {
    underTest.insert(session, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes(singletonList("hashes"))
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setLineHashesVersion(1)
      .setRevision("123456789"));
    session.commit();

    assertThat(underTest.selectLineHashesVersion(dbTester.getSession(), "FILE2_UUID")).isEqualTo(LineHashVersion.WITH_SIGNIFICANT_CODE);
  }

  @Test
  public void readLineHashesStream_does_not_fail_when_lineshashes_is_null() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.insert(session, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    session.commit();

    boolean[] flag = {false};
    underTest.readLineHashesStream(dbTester.getSession(), "FILE2_UUID", new Consumer<Reader>() {
      @Override
      public void accept(@Nullable Reader input) {
        fail("function must never been called since there is no data to read");
        flag[0] = true;
      }
    });
    assertThat(flag[0]).isFalse();
  }

  @Test
  public void update() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.update(session, new FileSourceDto()
      .setId(101L)
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE1_UUID")
      .setBinaryData("updated data".getBytes())
      .setDataHash("NEW_DATA_HASH")
      .setSrcHash("NEW_FILE_HASH")
      .setLineHashes(singletonList("NEW_LINE_HASHES"))
      .setDataType(Type.SOURCE)
      .setUpdatedAt(1500000000002L)
      .setLineHashesVersion(1)
      .setRevision("987654321"));
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "update-result.xml", "file_sources", "project_uuid", "file_uuid",
      "data_hash", "line_hashes", "src_hash", "created_at", "updated_at", "data_type", "revision", "line_hashes_version");
  }

  @Test
  public void update_to_no_line_hashes() {
    ImmutableList<String> lineHashes = of("a", "b", "c");
    FileSourceDto fileSourceDto = new FileSourceDto()
      .setProjectUuid("Foo")
      .setFileUuid("Bar")
      .setDataType(Type.SOURCE)
      .setLineHashes(lineHashes)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L);
    underTest.insert(session, fileSourceDto);
    session.commit();

    FileSourceDto resBefore = underTest.selectSourceByFileUuid(session, fileSourceDto.getFileUuid());
    assertThat(resBefore.getLineCount()).isEqualTo(lineHashes.size());
    assertThat(resBefore.getLineHashes()).isEqualTo(lineHashes);

    fileSourceDto.setId(resBefore.getId());
    fileSourceDto.setLineHashes(emptyList());
    underTest.update(session, fileSourceDto);
    session.commit();

    FileSourceDto res = underTest.selectSourceByFileUuid(session, fileSourceDto.getFileUuid());
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
