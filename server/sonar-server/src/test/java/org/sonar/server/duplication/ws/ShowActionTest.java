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

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowActionTest {

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
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(session);
    tester = new WsTester(new DuplicationsWs(new ShowAction(dbClient, componentDao, measureDao, parser, duplicationsJsonWriter)));
  }

  @Test
  public void show_duplications() throws Exception {
    String componentKey = "src/Foo.java";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    ComponentDto componentDto = new ComponentDto().setId(10L);
    when(componentDao.getNullableByKey(session, componentKey)).thenReturn(componentDto);

    String data = "{duplications}";
    when(measureDao.findByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(
      new MeasureDto().setComponentKey(componentKey).setMetricKey(CoreMetrics.DUPLICATIONS_DATA_KEY).setData("{duplications}")
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
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    when(componentDao.getByUuid(session, uuid)).thenReturn(new ComponentDto().setKey(componentKey));

    ComponentDto componentDto = new ComponentDto().setId(10L);
    when(componentDao.getNullableByKey(session, componentKey)).thenReturn(componentDto);

    String data = "{duplications}";
    when(measureDao.findByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(
      new MeasureDto().setComponentKey(componentKey).setMetricKey(CoreMetrics.DUPLICATIONS_DATA_KEY).setData("{duplications}")
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
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    ComponentDto componentDto = new ComponentDto().setId(10L);
    when(componentDao.getNullableByKey(session, componentKey)).thenReturn(componentDto);

    when(measureDao.findByComponentKeyAndMetricKey(session, componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY)).thenReturn(null);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();

    verify(duplicationsJsonWriter).write(eq(Lists.<DuplicationsParser.Block>newArrayList()), any(JsonWriter.class), eq(session));
  }

  @Test(expected = NotFoundException.class)
  public void fail_when_file_not_found() throws Exception {
    String componentKey = "src/Foo.java";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();
  }

}
