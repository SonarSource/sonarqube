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
package org.sonar.server.db;

import java.util.Date;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.MyBatis;
import org.sonar.server.db.fake.FakeDao;
import org.sonar.server.db.fake.FakeDto;
import org.sonar.server.db.fake.FakeMapper;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class BaseDaoTest {

  @Rule
  public DbTester db = DbTester.createForSchema(System2.INSTANCE, BaseDaoTest.class, "schema.sql");

  private static final String DTO_ALIAS = "fake";

  private FakeDao dao;
  private DbSession session;

  @Before
  public void setupBatis() {
    MyBatis batis = db.myBatis();
    batis.getSessionFactory().getConfiguration().getTypeAliasRegistry().registerAlias(DTO_ALIAS, FakeDto.class);
    batis.getSessionFactory().getConfiguration().addMapper(FakeMapper.class);
  }

  @Before
  public void before() {
    this.session = db.myBatis().openSession(false);
    this.dao = new FakeDao(System2.INSTANCE);
  }

  @After
  public void after() {
    this.session.close();
    db.executeUpdateSql("TRUNCATE TABLE fake");
  }

  @Test
  public void has_fake_mapper() {
    FakeMapper mapper = db.myBatis().getSessionFactory()
      .getConfiguration().getMapper(FakeMapper.class, session);
    assertThat(mapper).isNotNull();
  }

  @Test
  public void can_insert_and_select_by_key() {
    long t0 = System.currentTimeMillis() - 1000;

    String key = Uuids.create();
    FakeDto myDto = new FakeDto()
      .setKey(key);
    dao.insert(session, myDto);

    session.commit();
    assertThat(myDto.getId()).isGreaterThan(0);

    long t1 = System.currentTimeMillis() + 1000;

    FakeDto dto = dao.getByKey(session, key);
    assertThat(dto).isNotNull();

    assertThat(dto.getUpdatedAt().getTime()).isGreaterThan(t0);
    assertThat(dto.getCreatedAt().getTime()).isLessThan(t1);
  }

  @Test
  public void does_enqueue_on_insert() {
    FakeDto myDto = new FakeDto()
      .setKey(Uuids.create());
    dao.insert(session, myDto);
    session.commit();
    assertThat(session.getActionCount()).isEqualTo(1);
  }

  @Test
  public void synchronize_to_es_after_date() {
    long t0 = System.currentTimeMillis() - 1000;

    String key = Uuids.create();
    FakeDto myDto = new FakeDto()
      .setKey(key);
    dao.insert(session, myDto);

    session.commit();
    assertThat(session.getActionCount()).isEqualTo(1);

    dao.synchronizeAfter(session, new Date(t0));
    // Synchronize adds an implicit action to the queue before finishing.
    assertThat(session.getActionCount()).isEqualTo(3);
  }
}
