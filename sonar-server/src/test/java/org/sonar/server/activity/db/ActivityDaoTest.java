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
package org.sonar.server.activity.db;


import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.System2;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.ActivityLog;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ActivityDaoTest extends AbstractDaoTestCase {


  private ActivityDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new ActivityDao(system2);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void fail_insert_missing_type() {
    String testValue = "hello world";
    ActivityDto log = ActivityDto.createFor(testValue);
    try {
      dao.insert(session, log);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Type must be set");
    }
  }

  @Test
  public void fail_insert_missing_author() {
    String testValue = "hello world";
    ActivityDto log = ActivityDto.createFor(testValue)
      .setType(Activity.Type.QPROFILE);
    try {
      dao.insert(session, log);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("Type must be set");
    }
  }

  @Test
  public void insert_text_log() {
    String testValue = "hello world";
    ActivityDto log = ActivityDto.createFor(testValue)
      .setType(Activity.Type.QPROFILE)
      .setAuthor("jUnit");
    dao.insert(session, log);

    assertThat(dao.findAll(session)).hasSize(1);
    ActivityDto newDto = dao.getByKey(session, log.getKey());
    assertThat(newDto.getAuthor()).isEqualTo(log.getAuthor());
    assertThat(newDto.getMessage()).isEqualTo(testValue);
  }

  @Test
  public void insert_loggable_log() {
    final String testKey = "message";
    final String testValue = "hello world";
    ActivityDto log = ActivityDto.createFor(new ActivityLog() {

      @Override
      public Map<String, String> getDetails() {
        return ImmutableMap.of(testKey, testValue);
      }

      @Override
      public String getAction() {
        return "myAction";
      }
    })
      .setAuthor("jUnit")
      .setType(Activity.Type.QPROFILE);

    dao.insert(session, log);

    assertThat(dao.findAll(session)).hasSize(1);
    ActivityDto newDto = dao.getByKey(session, log.getKey());
    assertThat(newDto.getAuthor()).isEqualTo(log.getAuthor());
    assertThat(newDto.getData()).isNotNull();
    Map<String, String> details = KeyValueFormat.parse(newDto.getData());
    assertThat(details.get(testKey)).isEqualTo(testValue);
  }

}
