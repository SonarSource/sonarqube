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
package org.sonar.server.source.ws;

import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.utils.text.JsonWriter;

import java.io.StringWriter;

public class ScmWriterTest {

  ScmWriter writer = new ScmWriter();

  @Test
  public void write_authors_and_dates() throws Exception {
    // same commit on lines 1 and 2
    test("1=julien;2=julien;3=simon;", "1=2013-03-13T16:22:31+0100;2=2013-03-13T16:22:31+0100;3=2014-01-01T16:22:31+0100;", 1, Integer.MAX_VALUE, true,
      "{\"scm\": [[1, \"julien\", \"2013-03-13\"], [2, \"julien\", \"2013-03-13\"], [3, \"simon\", \"2014-01-01\"]]}");
  }

  @Test
  public void filter_by_range_of_lines() throws Exception {
    String authors = "1=julien;2=simon;";
    String dates = "1=2013-03-13T16:22:31+0100;2=2014-01-01T16:22:31+0100;";

    test(authors, dates, 2, 200, true, "{\"scm\": [[2, \"simon\", \"2014-01-01\"]]}");
    test(authors, dates, 3, 5, true, "{\"scm\": []}");
    test(authors, dates, -2, 1, true, "{\"scm\": [[1, \"julien\", \"2013-03-13\"]]}");
  }

  @Test
  public void group_by_commits() throws Exception {
    // lines 1 and 2 are the same commit, but not 3 (different date)
    String authors = "1=julien;2=julien;3=julien;4=simon;";
    String dates = "1=2013-03-13T16:22:31+0100;2=2013-03-13T16:22:31+0100;3=2013-03-14T16:22:31+0100;4=2014-01-01T16:22:31+0100";

    test(authors, dates, 1, Integer.MAX_VALUE, false,
      "{\"scm\": [[1, \"julien\", \"2013-03-13\"], [3, \"julien\", \"2013-03-14\"], [4, \"simon\", \"2014-01-01\"]]}");

    test(authors, dates, 2, 4, false,
      "{\"scm\": [[2, \"julien\", \"2013-03-13\"], [3, \"julien\", \"2013-03-14\"], [4, \"simon\", \"2014-01-01\"]]}");

    test(authors, dates, 1, 2, false, "{\"scm\": [[1, \"julien\", \"2013-03-13\"]]}");
    test(authors, dates, 10, 20, false, "{\"scm\": []}");
  }


  private void test(String authors, String dates, int from, int to, boolean group, String expected) throws JSONException {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    jsonWriter.beginObject();
    writer.write(authors, dates, from, to, group, jsonWriter);
    jsonWriter.endObject();
    JSONAssert.assertEquals(output.toString(), expected, true);
  }
}
