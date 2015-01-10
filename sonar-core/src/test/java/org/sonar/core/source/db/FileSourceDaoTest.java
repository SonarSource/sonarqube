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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.DateUtils;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class FileSourceDaoTest extends AbstractDaoTestCase {

  private FileSourceDao dao;

  @Before
  public void setUpTestData() {
    dao = new FileSourceDao(getMyBatis());
    setupData("shared");
  }

  @Test
  public void select() throws Exception {
    FileSourceDto fileSourceDto = dao.select("ab12");

    assertThat(fileSourceDto.getData()).isEqualTo("aef12a,alice,2014-04-25T12:34:56+0100,,class Foo");
    assertThat(fileSourceDto.getDataHash()).isEqualTo("hash");
    assertThat(fileSourceDto.getProjectUuid()).isEqualTo("abcd");
    assertThat(fileSourceDto.getFileUuid()).isEqualTo("ab12");
    assertThat(new Date(fileSourceDto.getCreatedAt())).isEqualTo(DateUtils.parseDateTime("2014-10-29T16:44:02+0100"));
    assertThat(new Date(fileSourceDto.getUpdatedAt())).isEqualTo(DateUtils.parseDateTime("2014-10-30T16:44:02+0100"));
  }

  @Test
  public void selectLineHashes() throws Exception {
    DbSession session = getMyBatis().openSession(false);
    String lineHashes = null;
    try {
      lineHashes = dao.selectLineHashes("ab12", session);
    } finally {
      session.close();
    }

    assertThat(lineHashes).isEqualTo("truc");
  }

  @Test
  public void insert() throws Exception {
    dao.insert(new FileSourceDto().setProjectUuid("prj").setFileUuid("file").setData("bla bla")
      .setDataHash("hash2")
      .setLineHashes("foo\nbar")
      .setCreatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime())
      .setUpdatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime()));

    checkTable("insert", "file_sources");
  }

  @Test
  public void update() throws Exception {
    dao.update(new FileSourceDto().setId(101L).setProjectUuid("prj").setFileUuid("file")
      .setData("updated data")
      .setDataHash("hash2")
      .setLineHashes("foo2\nbar2")
      .setUpdatedAt(DateUtils.parseDateTime("2014-10-31T16:44:02+0100").getTime()));

    checkTable("update", "file_sources");
  }
}
