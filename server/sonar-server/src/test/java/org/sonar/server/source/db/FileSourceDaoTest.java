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

package org.sonar.server.source.db;

import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.core.source.db.FileSourceDto.Type;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDaoTest extends AbstractDaoTestCase {

  DbSession session;

  FileSourceDao sut;

  @Before
  public void setUpTestData() {
    session = getMyBatis().openSession(false);
    sut = new FileSourceDao(getMyBatis());
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select() throws Exception {
    setupData("shared");

    FileSourceDto fileSourceDto = sut.selectSource("FILE1_UUID");

    assertThat(fileSourceDto.getBinaryData()).isNotEmpty();
    assertThat(fileSourceDto.getDataHash()).isEqualTo("hash");
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo("PRJ_UUID");
    assertThat(fileSourceDto.getFileUuid()).isEqualTo("FILE1_UUID");
    assertThat(fileSourceDto.getCreatedAt()).isEqualTo(1500000000000L);
    assertThat(fileSourceDto.getUpdatedAt()).isEqualTo(1500000000000L);
    assertThat(fileSourceDto.getDataType()).isEqualTo(Type.SOURCE);
  }

  @Test
  public void select_data() throws Exception {
    setupData("shared");

    InputStreamToStringFunction fn = new InputStreamToStringFunction();
    sut.readDataStream("FILE1_UUID", fn);

    assertThat(fn.result).isNotEmpty();
  }

  @Test
  public void select_line_hashes() throws Exception {
    setupData("shared");

    ReaderToStringFunction fn = new ReaderToStringFunction();
    sut.readLineHashesStream(session, "FILE1_UUID", fn);

    assertThat(fn.result).isEqualTo("ABC\\nDEF\\nGHI");
  }

  @Test
  public void no_line_hashes_on_unknown_file() throws Exception {
    setupData("shared");

    ReaderToStringFunction fn = new ReaderToStringFunction();
    sut.readLineHashesStream(session, "unknown", fn);

    assertThat(fn.result).isNull();
  }

  @Test
  public void insert() throws Exception {
    setupData("shared");

    sut.insert(new FileSourceDto()
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE2_UUID")
      .setBinaryData("FILE2_BINARY_DATA".getBytes())
      .setDataHash("FILE2_DATA_HASH")
      .setLineHashes("LINE1_HASH\\nLINE2_HASH")
      .setSrcHash("FILE2_HASH")
      .setDataType(Type.SOURCE)
      .setCreatedAt(1500000000000L)
      .setUpdatedAt(1500000000001L));

    checkTable("insert", "file_sources", "project_uuid", "file_uuid", "data_hash", "line_hashes", "src_hash", "created_at", "updated_at", "data_type");
  }

  @Test
  public void update() throws Exception {
    setupData("shared");

    sut.update(new FileSourceDto()
      .setId(101L)
      .setProjectUuid("PRJ_UUID")
      .setFileUuid("FILE1_UUID")
      .setBinaryData("updated data".getBytes())
      .setDataHash("NEW_DATA_HASH")
      .setSrcHash("NEW_FILE_HASH")
      .setLineHashes("NEW_LINE_HASHES")
      .setDataType(Type.SOURCE)
      .setUpdatedAt(1500000000002L));

    checkTable("update", "file_sources", "project_uuid", "file_uuid", "data_hash", "line_hashes", "src_hash", "created_at", "updated_at", "data_type");
  }

  @Test
  public void update_date_when_updated_date_is_zero() throws Exception {
    setupData("update_date_when_updated_date_is_zero");

    sut.updateDateWhenUpdatedDateIsZero(session, "ABCD", 1500000000002L);
    session.commit();

    checkTable("update_date_when_updated_date_is_zero", "file_sources", "project_uuid", "file_uuid", "data_hash", "line_hashes", "src_hash", "created_at", "updated_at",
      "data_type");
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

  private static class InputStreamToStringFunction implements Function<InputStream, String> {

    String result = null;

    @Override
    public String apply(InputStream input) {
      try {
        result = IOUtils.toString(input);
        return IOUtils.toString(input);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
