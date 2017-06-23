/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.tests.scm;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.BuildResult;
import com.sonar.orchestrator.build.SonarScanner;
import org.sonarqube.tests.Category2Suite;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.wsclient.jsonsimple.JSONArray;
import org.sonar.wsclient.jsonsimple.JSONObject;
import org.sonar.wsclient.jsonsimple.JSONValue;

import static org.assertj.core.api.Assertions.assertThat;
import static util.ItUtils.projectDir;

public class ScmTest {

  @ClassRule
  public static Orchestrator orchestrator = Category2Suite.ORCHESTRATOR;

  private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

  @Before
  public void delete_data() {
    orchestrator.resetData();
  }

  @Test
  public void scm_optimization() throws Exception {
    SonarScanner build = SonarScanner.create(projectDir("scm/xoo-sample-with-scm"))
      .setProperty("sonar.scm.provider", "xoo")
      .setProperty("sonar.scm.disabled", "false");

    // First run
    BuildResult buildResult = orchestrator.executeBuild(build);

    assertThat(getScmData("sample-scm:src/main/xoo/sample/Sample.xoo"))
      .containsExactly(
        MapEntry.entry(1, new LineData("1", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(3, new LineData("2", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(4, new LineData("1", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(8, new LineData("3", "2014-01-04T00:00:00+0000", "toto")));

    assertThat(buildResult.getLogs()).containsSequence("1 files to be analyzed", "1/1 files analyzed");

    // Second run with same file should not trigger blame but SCM data are copied from previous analysis
    buildResult = orchestrator.executeBuild(build);

    assertThat(getScmData("sample-scm:src/main/xoo/sample/Sample.xoo"))
      .containsExactly(
        MapEntry.entry(1, new LineData("1", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(3, new LineData("2", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(4, new LineData("1", "2013-01-04T00:00:00+0000", "jhenry")),
        MapEntry.entry(8, new LineData("3", "2014-01-04T00:00:00+0000", "toto")));

    assertThat(buildResult.getLogs()).doesNotContain("1 files to be analyzed");
    assertThat(buildResult.getLogs()).doesNotContain("1/1 files analyzed");

    // Now if SCM is explicitely disabled it should clear SCM data on server side
    buildResult = orchestrator.executeBuild(build.setProperty("sonar.scm.disabled", "true"));

    assertThat(getScmData("sample-scm:src/main/xoo/sample/Sample.xoo")).isEmpty();

    assertThat(buildResult.getLogs()).doesNotContain("1 files to be analyzed");
    assertThat(buildResult.getLogs()).doesNotContain("1/1 files analyzed");
  }

  private class LineData {

    final String revision;
    final Date date;
    final String author;

    public LineData(String revision, String datetime, String author) throws ParseException {
      this.revision = revision;
      this.date = DATETIME_FORMAT.parse(datetime);
      this.author = author;
    }

    @Override
    public boolean equals(Object obj) {
      return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder().append(revision).append(date).append(author).toHashCode();
    }

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE);
    }
  }

  private Map<Integer, LineData> getScmData(String fileKey) throws ParseException {
    Map<Integer, LineData> result = new HashMap<>();
    String json = orchestrator.getServer().adminWsClient().get("api/sources/scm", "key", fileKey);
    JSONObject obj = (JSONObject) JSONValue.parse(json);
    JSONArray array = (JSONArray) obj.get("scm");
    for (Object anArray : array) {
      JSONArray item = (JSONArray) anArray;
      String datetime = (String) item.get(2);
      result.put(((Long) item.get(0)).intValue(), new LineData((String) item.get(3), datetime, (String) item.get(1)));
    }
    return result;
  }

}
