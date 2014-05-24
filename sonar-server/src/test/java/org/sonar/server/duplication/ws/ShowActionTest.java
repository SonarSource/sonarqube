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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.measure.db.MeasureKey;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.measure.persistence.MeasureDao;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShowActionTest {

  @Mock
  DbSession session;

  @Mock
  DbClient dbClient;

  @Mock
  MeasureDao measureDao;

  @Mock
  DuplicationsWriter duplicationsWriter;

  WsTester tester;

  @Before
  public void setUp() throws Exception {
    when(dbClient.openSession(false)).thenReturn(session);
    tester = new WsTester(new DuplicationsWs(new ShowAction(dbClient, measureDao, duplicationsWriter)));
  }

  @Test
  public void show_duplications() throws Exception {
    String componentKey = "src/Foo.java";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    MeasureKey measureKey = MeasureKey.of(componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY);
    when(measureDao.getByKey(session, measureKey)).thenReturn(
      MeasureDto.createFor(measureKey).setTextValue("{duplications}")
    );

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();

    verify(duplicationsWriter).write(eq("{duplications}"), any(JsonWriter.class), eq(session));
  }

  @Test
  public void no_duplications_when_no_data() throws Exception {
    String componentKey = "src/Foo.java";
    MockUserSession.set().addComponentPermission(UserRole.CODEVIEWER, "org.codehaus.sonar:sonar", componentKey);

    MeasureKey measureKey = MeasureKey.of(componentKey, CoreMetrics.DUPLICATIONS_DATA_KEY);
    when(measureDao.getByKey(session, measureKey)).thenReturn(null);

    WsTester.TestRequest request = tester.newGetRequest("api/duplications", "show").setParam("key", componentKey);
    request.execute();

    verify(duplicationsWriter).write(isNull(String.class), any(JsonWriter.class), eq(session));
  }

}
