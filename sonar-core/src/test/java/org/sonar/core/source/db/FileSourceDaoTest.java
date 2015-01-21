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

package org.sonar.core.source.db;

import com.google.common.base.Function;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDaoTest extends AbstractDaoTestCase {

  DbSession session;

  FileSourceDao dao;

  @Before
  public void setUpTestData() {
    session = getMyBatis().openSession(false);
    dao = new FileSourceDao(getMyBatis());
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select() throws Exception {
    setupData("shared");

    FileSourceDto fileSourceDto = dao.select("ab12");

    assertThat(fileSourceDto.getData()).isEqualTo("aef12a,alice,2014-04-25T12:34:56+0100,,class Foo");
    assertThat(fileSourceDto.getDataHash()).isEqualTo("hash");
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo("abcd");
    assertThat(fileSourceDto.getFileUuid()).isEqualTo("ab12");
    assertThat(new Date(fileSourceDto.getCreatedAt())).isEqualTo(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));
    assertThat(new Date(fileSourceDto.getUpdatedAt())).isEqualTo(DateUtils.parseDateTime("2014-10-30T16:44:02+0100"));
  }

  @Test
  public void select_data() throws Exception {
    setupData("shared");

    StringParser stringParser = new StringParser();
    dao.readDataStream("ab12", stringParser);

    assertThat(stringParser.getResult()).isEqualTo("aef12a,alice,2014-04-25T12:34:56+0100,,class Foo");
  }

  @Test
  public void select_line_hashes() throws Exception {
    setupData("shared");

    StringParser stringParser = new StringParser();
    dao.readLineHashesStream(session, "ab12", stringParser);

    assertThat(stringParser.getResult()).isEqualTo("truc");
  }

  @Test
  public void no_line_hashes_on_unknown_file() throws Exception {
    setupData("shared");

    StringParser stringParser = new StringParser();
    dao.readLineHashesStream(session, "unknown", stringParser);

    assertThat(stringParser.getResult()).isEmpty();
  }

  @Test
  public void insert() throws Exception {
    setupData("shared");

    dao.insert(new FileSourceDto().setProjectUuid("prj").setFileUuid("file").setData("bla bla")
      .setDataHash("hash2")
      .setLineHashes("foo\nbar")
      .setSrcHash("hache")
      .setCreatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime())
      .setUpdatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime()));

    checkTable("insert", "file_sources");
  }

  @Test
  public void update() throws Exception {
    setupData("shared");

    dao.update(new FileSourceDto().setId(101L).setProjectUuid("prj").setFileUuid("file")
      .setData("updated data")
      .setDataHash("hash2")
      .setSrcHash("123456")
      .setLineHashes("foo2\nbar2")
      .setUpdatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime()));

    checkTable("update", "file_sources");
  }

  class StringParser implements Function<Reader, String> {

    String result = "";

    @Override
    public String apply(Reader input) {
      try {
        result = IOUtils.toString(input);
        return IOUtils.toString(input);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    public String getResult() {
      return result;
    }
  }
}
