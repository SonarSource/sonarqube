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

package org.sonar.server.test.ws;

import com.google.common.io.Resources;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.test.MutableTestPlan;
import org.sonar.api.test.TestCase;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.SnapshotPerspectives;
import org.sonar.core.measure.db.MeasureDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.StringReader;

public class TestsShowAction implements RequestHandler {

  private static final String KEY = "key";

  private final DbClient dbClient;
  private final SnapshotPerspectives snapshotPerspectives;

  public TestsShowAction(DbClient dbClient, SnapshotPerspectives snapshotPerspectives) {
    this.dbClient = dbClient;
    this.snapshotPerspectives = snapshotPerspectives;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Get the list of test cases of a test plan. Require Browse permission on file's project")
      .setSince("4.4")
      .setResponseExample(Resources.getResource(getClass(), "tests-example-show.json"))
      .setHandler(this);

    action
      .createParam(KEY)
      .setRequired(true)
      .setDescription("Test plan key")
      .setExampleValue("my_project:/src/test/BarTest.java");
  }

  @Override
  public void handle(Request request, Response response) {
    String fileKey = request.mandatoryParam(KEY);
    UserSession.get().checkComponentPermission(UserRole.CODEVIEWER, fileKey);

    String testData = findTestData(fileKey);
    JsonWriter json = response.newJsonWriter().beginObject();
    if (testData != null) {
      writeFromTestData(testData, json);
    } else {
      MutableTestPlan testPlan = snapshotPerspectives.as(MutableTestPlan.class, fileKey);
      if (testPlan != null) {
        writeFromTestable(testPlan, json);
      }
    }
    json.endObject().close();
  }

  private void writeFromTestable(MutableTestPlan testPlan, JsonWriter json) {
    json.name("tests").beginArray();
    for (TestCase testCase : testPlan.testCases()) {
      json.beginObject();
      json.prop("name", testCase.name());
      json.prop("status", testCase.status().name());
      json.prop("durationInMs", testCase.durationInMs());
      json.prop("coveredLines", testCase.countCoveredLines());
      json.prop("message", testCase.message());
      json.prop("stackTrace", testCase.stackTrace());
      json.endObject();
    }
    json.endArray();
  }

  private void writeFromTestData(String data, JsonWriter json) {
    SMInputFactory inputFactory = initStax();
    try {
      SMHierarchicCursor root = inputFactory.rootElementCursor(new StringReader(data));
      root.advance(); // tests-details
      SMInputCursor cursor = root.childElementCursor();
      json.name("tests").beginArray();
      while (cursor.getNext() != null) {
        json.beginObject();

        json.prop("name", cursor.getAttrValue("name"));
        json.prop("status", cursor.getAttrValue("status").toUpperCase());
        // time can contain float value, we have to truncate it
        json.prop("durationInMs", ((Double) Double.parseDouble(cursor.getAttrValue("time"))).longValue());

        SMInputCursor errorCursor = cursor.childElementCursor();
        if (errorCursor.getNext() != null) {
          json.prop("message", errorCursor.getAttrValue("message"));
          json.prop("stackTrace", errorCursor.getElemStringValue());
        }

        json.endObject();
      }
      json.endArray();
    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid: " + e.getMessage(), e);
    }
  }

  @CheckForNull
  private String findTestData(String fileKey) {
    DbSession session = dbClient.openSession(false);
    try {
      MeasureDto testData = dbClient.measureDao().findByComponentKeyAndMetricKey(session, fileKey, CoreMetrics.TEST_DATA_KEY);
      if (testData != null) {
        return testData.getData();
      }
    } finally {
      MyBatis.closeQuietly(session);
    }
    return null;
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

}
