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

package org.sonar.server.batch;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class UploadReportActionMediumTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  DbClient db;
  DbSession session;

  WsTester wsTester;
  WebService.Controller controller;

  @Before
  public void setUp() throws Exception {
    tester.clearDbAndIndexes();

    db = tester.get(DbClient.class);
    session = db.openSession(false);

    wsTester = tester.get(WsTester.class);
    controller = wsTester.controller(BatchWs.API_ENDPOINT);
  }

  @After
  public void after() {
    MyBatis.closeQuietly(session);
  }

  @Test
  public void define() throws Exception {
    WebService.Action action = controller.action(UploadReportAction.UPLOAD_REPORT_ACTION);

    assertThat(action).isNotNull();
    assertThat(action.params()).hasSize(3);
  }
}
