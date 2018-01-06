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

import java.io.IOException;
import java.io.Reader;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.source.FileSourceDto.Type;

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
  }

  @Test
  public void select_line_hashes() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    ReaderToStringFunction fn = new ReaderToStringFunction();
    underTest.readLineHashesStream(dbTester.getSession(), "FILE1_UUID", fn);

    assertThat(fn.result).isEqualTo("ABC\\nDEF\\nGHI");
  }

  @Test
  public void no_line_hashes_on_unknown_file() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    ReaderToStringFunction fn = new ReaderToStringFunction();
    underTest.readLineHashesStream(dbTester.getSession(), "unknown", fn);

    assertThat(fn.result).isNull();
  }

  @Test
  public void no_line_hashes_when_only_test_data() {
    dbTester.prepareDbUnit(getClass(), "no_line_hashes_when_only_test_data.xml");

    ReaderToStringFunction fn = new ReaderToStringFunction();
    underTest.readLineHashesStream(dbTester.getSession(), "FILE1_UUID", fn);

    assertThat(fn.result).isNull();
  }

  @Test
  public void insert() {
    dbTester.prepareDbUnit(getClass(), "shared.xml");

    underTest.insert(session, new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes("LINE1_HASH\\nLINE2_HASH")
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L)
      .setRevision("123456789"));
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "insert-result.xml", "file_sources",
      "project_uuid", "file_uuid", "data_hash", "line_hashes", "src_hash", "created_at", "updated_at", "data_type", "revision");
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
    underTest.readLineHashesStream(dbTester.getSession(), "FILE2_UUID", new Function<Reader, Void>() {
      @Nullable
      @Override
      public Void apply(@Nullable Reader input) {
        fail("function must never been called since there is no data to read");
        flag[0] = true;
        return null;
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
      .setLineHashes("NEW_LINE_HASHES")
      .setDataType(Type.SOURCE)
      .setUpdatedAt(1500000000002L)
      .setRevision("987654321"));
    session.commit();

    dbTester.assertDbUnitTable(getClass(), "update-result.xml", "file_sources",
      "project_uuid", "file_uuid", "data_hash", "line_hashes", "src_hash", "created_at", "updated_at", "data_type", "revision");
  }

  private static class ReaderToStringFunction implements Function<Reader, String> {

    String result = null;

    @Override
    public String apply(Reader input) {
      try {
        result = IOUtils.toString(input);
        return IOUtils.toString(input);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
