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

package org.sonar.wsclient.test.internal;

import org.json.simple.JSONValue;
import org.sonar.wsclient.internal.EncodingUtils;
import org.sonar.wsclient.internal.HttpRequestFactory;
import org.sonar.wsclient.test.CoveredFile;
import org.sonar.wsclient.test.TestCase;
import org.sonar.wsclient.test.TestClient;
import org.sonar.wsclient.test.TestableTestCases;
import org.sonar.wsclient.unmarshallers.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultTestClient implements TestClient {

  private final HttpRequestFactory requestFactory;

  public DefaultTestClient(HttpRequestFactory requestFactory) {
    this.requestFactory = requestFactory;
  }

  @Override
  public List<TestCase> show(String testPlanKey) {
    Map<String, Object> params = EncodingUtils.toMap("key", testPlanKey);
    String jsonResult = requestFactory.get("/api/tests/show", params);

    List<TestCase> testCases = new ArrayList<TestCase>();
    Map jRoot = (Map) JSONValue.parse(jsonResult);
    List<Map> maps = (List) jRoot.get("tests");
    if (maps != null) {
      for (final Map json : maps) {
        testCases.add(new TestCase() {
          @Override
          public String name() {
            return JsonUtils.getString(json, "name");
          }

          @Override
          public String status() {
            return JsonUtils.getString(json, "status");
          }

          @Override
          public Long durationInMs() {
            return JsonUtils.getLong(json, "durationInMs");
          }

          @Override
          public Integer coveredLines() {
            return JsonUtils.getInteger(json, "coveredLines");
          }
        });
      }
    }
    return testCases;
  }

  @Override
  public List<CoveredFile> plan(String testPlanKey, String testCase) {
    Map<String, Object> params = EncodingUtils.toMap("key", testPlanKey, "test", testCase);
    String jsonResult = requestFactory.get("/api/tests/plan", params);

    List<CoveredFile> files = new ArrayList<CoveredFile>();
    Map jRoot = (Map) JSONValue.parse(jsonResult);
    List<Map> maps = (List) jRoot.get("files");
    if (maps != null) {
      for (final Map json : maps) {
        files.add(new CoveredFile() {
          @Override
          public String key() {
            return JsonUtils.getString(json, "key");
          }

          @Override
          public String longName() {
            return JsonUtils.getString(json, "longName");
          }

          @Override
          public Integer coveredLines() {
            return JsonUtils.getInteger(json, "coveredLines");
          }
        });
      }
    }
    return files;
  }

  @Override
  public TestableTestCases testable(String fileKey, Integer line) {
    Map<String, Object> params = EncodingUtils.toMap("key", fileKey);
    params.put("line", line);
    String jsonResult = requestFactory.get("/api/tests/testable", params);

    Map jRoot = (Map) JSONValue.parse(jsonResult);
    DefaultTestableTestCases coveringTestCases = new DefaultTestableTestCases();

    List<Map> tests = (List) jRoot.get("tests");
    if (tests != null) {
      for (final Map json : tests) {
        String fileRef = JsonUtils.getString(json, "_ref");
        coveringTestCases.addTest(fileRef, new TestableTestCases.TestCase() {
          @Override
          public String name() {
            return JsonUtils.getString(json, "name");
          }

          @Override
          public String status() {
            return JsonUtils.getString(json, "status");
          }

          @Override
          public Long durationInMs() {
            return JsonUtils.getLong(json, "durationInMs");
          }

        });
      }
    }

    Map<String, Map> jsonFiles = (Map) jRoot.get("files");
    if (jsonFiles != null) {
      for (Map.Entry<String, Map> entry : jsonFiles.entrySet()) {
        String ref = entry.getKey();
        final Map file = entry.getValue();
        if (ref != null && file != null) {
          coveringTestCases.addFile(ref, new TestableTestCases.File() {
            @Override
            public String key() {
              return JsonUtils.getString(file, "key");
            }

            @Override
            public String longName() {
              return JsonUtils.getString(file, "longName");
            }

          });
        }
      }
    }
    return coveringTestCases;
  }
}
