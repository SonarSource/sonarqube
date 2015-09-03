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

package org.sonar.server.duplication.ws;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDao;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Mock
  DbSession session;

  @Mock
  DbClient dbClient;

  @Mock
  ComponentDao componentDao;

  @Mock
  MeasureDao measureDao;

  @Mock
  DuplicationsParser parser;

  @Mock
  DuplicationsJsonWriter duplicationsJsonWriter;

  WsTester tester;

  @Before
  public void setUp() {
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.componentDao()).thenReturn(componentDao);
    tester = new WsTester(new DuplicationsWs(new ShowAction(dbClient, measureDao, parser, duplicationsJsonWriter, userSessionRule, new ComponentFinder(dbClient))));
  }

  @Test
  public void show_duplications() throws Exception {
    String componentKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    ComponentDto componentDto = new ComponentDto().setId(10L).setKey(componentKey);
    when(componentDao.selectByKey(session, componentKey)).thenReturn(Optional.of(componentDto));

    String data = "{duplications}";
    when(measureDao.selectByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(
      new MeasureDto().setMetricKey(CoreMetrics.DUPLICATIONS_DATA_KEY).setData("{duplications}")
    );

    List<DuplicationsParser.Block> blocks = newArrayList(new DuplicationsParser.Block(newArrayList(new DuplicationsParser.Duplication(componentDto, 1, 2))));
    when(parser.parse(componentDto, data, session)).thenReturn(blocks);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();

    verify(duplicationsJsonWriter).write(eq(blocks), any(JsonWriter.class), eq(session));
  }

  @Test
  public void show_duplications_by_uuid() throws Exception {
    String uuid = "ABCD";
    String componentKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    ComponentDto componentDto = new ComponentDto().setId(10L).setKey(componentKey);
    when(componentDao.selectByUuid(session, uuid)).thenReturn(Optional.of(componentDto));

    String data = "{duplications}";
    when(measureDao.selectByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(
      new MeasureDto().setMetricKey(CoreMetrics.DUPLICATIONS_DATA_KEY).setData("{duplications}")
    );

    List<DuplicationsParser.Block> blocks = newArrayList(new DuplicationsParser.Block(newArrayList(new DuplicationsParser.Duplication(componentDto, 1, 2))));
    when(parser.parse(componentDto, data, session)).thenReturn(blocks);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("uuid", uuid);
    request.execute();

    verify(duplicationsJsonWriter).write(eq(blocks), any(JsonWriter.class), eq(session));
  }

  @Test
  public void no_duplications_when_no_data() throws Exception {
    String componentKey = "src/Foo.java";
    userSessionRule.addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    ComponentDto componentDto = new ComponentDto().setId(10L).setKey(componentKey);
    when(componentDao.selectByKey(session, componentKey)).thenReturn(Optional.of(componentDto));

    when(measureDao.selectByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(null);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();

    verify(duplicationsJsonWriter).write(eq(Lists.<DuplicationsParser.Block>newArrayList()), any(JsonWriter.class), eq(session));
  }

  @Test(expected = NotFoundException.class)
  public void fail_when_file_not_found() throws Exception {
    String componentKey = "src/Foo.java";

    when(componentDao.selectByKey(session, componentKey)).thenReturn(Optional.<ComponentDto>absent());

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();
  }

}
