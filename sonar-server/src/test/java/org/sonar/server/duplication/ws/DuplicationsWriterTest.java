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

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.persistence.ComponentDao;

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DuplicationsWriterTest {

  @Mock
  ComponentDao componentDao;

  @Mock
  DbSession session;

  DuplicationsWriter writer;

  @Before
  public void setUp() throws Exception {
    writer = new DuplicationsWriter(componentDao);
  }

  @Test
  public void write_duplications() throws Exception {
    String key1 = "org.codehaus.sonar:sonar-ws-client:src/main/java/org/sonar/wsclient/services/PropertyDeleteQuery.java";
    ComponentDto file1 = new ComponentDto().setId(10L).setQualifier("FIL").setKey(key1).setLongName("PropertyDeleteQuery").setProjectId(1L);
    String key2 = "org.codehaus.sonar:sonar-ws-client:src/main/java/org/sonar/wsclient/services/PropertyUpdateQuery.java";
    ComponentDto file2 = new ComponentDto().setId(11L).setQualifier("FIL").setKey(key2).setLongName("PropertyUpdateQuery").setProjectId(1L);

    when(componentDao.getByKey(session, key1)).thenReturn(file1);
    when(componentDao.getByKey(session, key2)).thenReturn(file2);
    when(componentDao.getById(1L, session)).thenReturn(new ComponentDto().setId(1L).setLongName("SonarQube"));

    List<DuplicationsParser.Block> blocks = newArrayList();
    blocks.add(new DuplicationsParser.Block(newArrayList(
      new DuplicationsParser.Duplication(file1, 57, 12),
      new DuplicationsParser.Duplication(file2, 73, 12)
    )));

    test(blocks,
      "{\n" +
        "  \"duplications\": [\n" +
        "    {\n" +
        "      \"blocks\": [\n" +
        "        {\n" +
        "          \"from\": 57, \"size\": 12, \"_ref\": \"1\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"from\": 73, \"size\": 12, \"_ref\": \"2\"\n" +
        "        }\n" +
        "      ]\n" +
        "    }," +
        "  ],\n" +
        "  \"files\": {\n" +
        "    \"1\": {\n" +
        "      \"key\": \"org.codehaus.sonar:sonar-ws-client:src/main/java/org/sonar/wsclient/services/PropertyDeleteQuery.java\",\n" +
        "      \"name\": \"PropertyDeleteQuery\",\n" +
        "      \"projectName\": \"SonarQube\"\n" +
        "    },\n" +
        "    \"2\": {\n" +
        "      \"key\": \"org.codehaus.sonar:sonar-ws-client:src/main/java/org/sonar/wsclient/services/PropertyUpdateQuery.java\",\n" +
        "      \"name\": \"PropertyUpdateQuery\",\n" +
        "      \"projectName\": \"SonarQube\"\n" +
        "    }\n" +
        "  }" +
        "}"
    );

    verify(componentDao, times(2)).getByKey(eq(session), anyString());
    verify(componentDao, times(1)).getById(anyLong(), eq(session));
  }

  @Test
  public void write_nothing_when_no_data() throws Exception {
    test(Collections.<DuplicationsParser.Block>emptyList(), "{\"duplications\": [], \"files\": {}}");
  }

  private void test(List<DuplicationsParser.Block> blocks, String expected) throws JSONException {
    StringWriter output = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(output);
    jsonWriter.beginObject();
    writer.write(blocks, jsonWriter, session);
    jsonWriter.endObject();
    JSONAssert.assertEquals(output.toString(), expected, true);
  }

}
