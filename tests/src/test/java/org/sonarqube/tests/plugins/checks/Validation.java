/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarqube.tests.plugins.checks;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.rules.ErrorCollector;
import org.sonarqube.tests.plugins.Project;
import org.sonarqube.ws.client.GetRequest;
import org.sonarqube.ws.client.WsResponse;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static util.ItUtils.getMeasuresAsDoubleByMetricKey;
import static util.ItUtils.newAdminWsClient;

/**
 *
 * TODO must have syntax highlighting
 * TODO must have duplications
 * TODO must have issues with debt
 * TODO must have tests
 * TODO must have coverage
 */
public class Validation {

  private final Orchestrator orchestrator;
  private final ErrorCollector errorCollector;

  public Validation(Orchestrator orchestrator, ErrorCollector errorCollector) {
    this.orchestrator = orchestrator;
    this.errorCollector = errorCollector;
  }

  public void mustHaveIssues(String path) {
    // TODO use the WS api/issues
    mustHaveMeasuresGreaterThanOrEquals(path, 1, "violations");
  }

  public void mustHaveComments(String path) {
    mustHaveMeasuresGreaterThanOrEquals(path, 0, "comment_lines", "comment_lines_density");
  }

  public void mustHaveComplexity(String path) {
    mustHaveMeasuresGreaterThanOrEquals(path, 0, "complexity");
  }

  public void mustHaveSize(String path) {
    mustHaveMeasuresGreaterThanOrEquals(path, 0, "ncloc", "lines");
  }

  public void mustHaveMeasuresGreaterThanOrEquals(String path, int min, String... metricKeys) {
    for (String filePath : toFiles(path)) {
      fileMustHaveMeasures(filePath, metricKeys, min);
    }
  }

  private void fileMustHaveMeasures(String filePath, String[] metricKeys, int min) {
    String componentKey = filePathToKey(filePath);
    Map<String, Double> measures = getMeasuresAsDoubleByMetricKey(orchestrator, componentKey, metricKeys);
    errorCollector.checkThat("Measures " + Joiner.on(",").join(metricKeys) + " are set on file " + filePath, componentKey, notNullValue());
    if (!measures.isEmpty()) {
      for (String metricKey : metricKeys) {
        Double measure = measures.get(metricKey);
        errorCollector.checkThat("Measure " + metricKey + " is set on file " + filePath, measure, notNullValue());
        if (measure != null) {
          errorCollector.checkThat("Measure " + metricKey + " is positive on file " + filePath, measure.intValue(), new TypeSafeMatcher<Integer>() {
            @Override
            protected boolean matchesSafely(Integer item) {
              return item >= min;
            }

            @Override
            public void describeTo(Description description) {
              description.appendText(metricKey).appendValue(min);
            }
          });
        }
      }
    }
  }

  /**
   * Checks that each source file of the given directory is uploaded to server.
   * @param path relative path to source directory or source file
   */
  public void mustHaveNonEmptySource(String path) {
    mustHaveSourceWithAtLeast(path, 1);
  }

  public void mustHaveSource(String path) {
    mustHaveSourceWithAtLeast(path, 0);
  }

  private void mustHaveSourceWithAtLeast(String path, int minLines) {
    for (String filePath : toFiles(path)) {
      WsResponse response = newAdminWsClient(orchestrator).wsConnector().call(new GetRequest("api/sources/lines").setParam("key", filePathToKey(filePath)));
      errorCollector.checkThat("Source is set on file " + filePath, response.isSuccessful(), is(true));
      Sources source = Sources.parse(response.content());
      if (source != null) {
        errorCollector.checkThat("Source is empty on file " + filePath, source.getSources().size(), new TypeSafeMatcher<Integer>() {
          @Override
          protected boolean matchesSafely(Integer item) {
            return item >= minLines;
          }

          @Override
          public void describeTo(Description description) {
            description.appendValue(minLines);
          }
        });
      }
    }
  }

  private Iterable<String> toFiles(String path) {
    File fileOrDir = new File(Project.basedir(), path);
    if (!fileOrDir.exists()) {
      throw new IllegalArgumentException("Path does not exist: " + fileOrDir);
    }
    if (fileOrDir.isDirectory()) {
      return Project.allFilesInDir(path);
    }
    return asList(path);
  }

  private String filePathToKey(String filePath) {
    return "all-langs:" + filePath;
  }

  public static class Sources {

    private List<Source> sources;

    private Sources(List<Source> sources) {
      this.sources = sources;
    }

    public List<Source> getSources() {
      return sources;
    }

    public static Sources parse(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, Sources.class);
    }

    public static class Source {
      private final String line;

      private Source(String line) {
        this.line = line;
      }

      public String getLine() {
        return line;
      }
    }
  }
}
