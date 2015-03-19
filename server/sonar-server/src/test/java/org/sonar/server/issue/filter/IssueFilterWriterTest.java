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

package org.sonar.server.issue.filter;

import org.junit.Test;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.db.IssueFilterDto;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import java.io.StringWriter;

import static org.sonar.test.JsonAssert.assertJson;

public class IssueFilterWriterTest {

  IssueFilterWriter writer = new IssueFilterWriter();

  @Test
  public void write_filter() throws Exception {
    UserSession userSession = MockUserSession.set();
    test(userSession,
      new IssueFilterDto()
        .setId(13L)
        .setName("Blocker issues")
        .setDescription("All Blocker Issues")
        .setShared(true)
        .setUserLogin("simon")
        .setData("severity=BLOCKER"),
      "{\"filter\":{\n" +
        "      \"id\":13,\n" +
        "        \"name\":\"Blocker issues\",\n" +
        "        \"description\":\"All Blocker Issues\",\n" +
        "        \"shared\":true,\n" +
        "        \"query\":\"severity=BLOCKER\",\n" +
        "        \"user\":\"simon\",\n" +
        "        \"canModify\":false\n" +
        "    }}");
  }

  @Test
  public void can_modify_if_logged_user_own_filter() throws Exception {
    UserSession userSession = MockUserSession.set().setLogin("simon");
    test(userSession,
      new IssueFilterDto()
        .setId(13L)
        .setName("Blocker issues")
        .setDescription("All Blocker Issues")
        .setShared(true)
        .setUserLogin("simon")
        .setData("severity=BLOCKER"),
      "{\"filter\":{\n" +
        "      \"id\":13,\n" +
        "        \"name\":\"Blocker issues\",\n" +
        "        \"description\":\"All Blocker Issues\",\n" +
        "        \"shared\":true,\n" +
        "        \"query\":\"severity=BLOCKER\",\n" +
        "        \"user\":\"simon\",\n" +
        "        \"canModify\":true\n" +
        "    }}");
  }

  @Test
  public void can_modify_if_logged_user_has_permission() throws Exception {
    UserSession userSession = MockUserSession.set().setLogin("simon").setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    test(userSession,
      new IssueFilterDto()
        .setId(13L)
        .setName("Blocker issues")
        .setDescription("All Blocker Issues")
        .setShared(true)
        .setUserLogin("julien")
        .setData("severity=BLOCKER"),
      "{\"filter\":{\n" +
        "      \"id\":13,\n" +
        "        \"name\":\"Blocker issues\",\n" +
        "        \"description\":\"All Blocker Issues\",\n" +
        "        \"shared\":true,\n" +
        "        \"query\":\"severity=BLOCKER\",\n" +
        "        \"user\":\"julien\",\n" +
        "        \"canModify\":true\n" +
        "    }}");
  }

  private void test(UserSession userSession, IssueFilterDto filter, String expected) {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    jsonWriter.beginObject();
    writer.write(userSession, filter, jsonWriter);
    jsonWriter.endObject();
    assertJson(output.toString()).isSimilarTo(expected);
  }
}
