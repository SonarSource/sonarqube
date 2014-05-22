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

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.component.persistence.ComponentDao;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.StringReader;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

public class DuplicationsWriter implements ServerComponent {

  private final ComponentDao componentDao;

  public DuplicationsWriter(ComponentDao componentDao) {
    this.componentDao = componentDao;
  }

  @VisibleForTesting
  void write(@Nullable String duplicationsData, JsonWriter json, DbSession session) {
    Map<String, String> refByComponentKey = newHashMap();
    json.name("duplications").beginArray();
    if (duplicationsData != null) {
      writeDuplications(duplicationsData, refByComponentKey, json, session);
    }
    json.endArray();

    json.name("files").beginObject();
    writeFiles(refByComponentKey, json, session);
    json.endObject();
  }

  private void writeDuplications(String duplicationsData, Map<String, String> refByComponentKey, JsonWriter json, DbSession session) {
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor root = inputFactory.rootElementCursor(new StringReader(duplicationsData));
      root.advance(); // <duplications>
      SMInputCursor cursor = root.childElementCursor("g");
      while (cursor.getNext() != null) {
        json.beginObject().name("blocks").beginArray();
        SMInputCursor bCursor = cursor.childElementCursor("b");
        while (bCursor.getNext() != null) {
          String from = bCursor.getAttrValue("s");
          String size = bCursor.getAttrValue("l");
          String componentKey = bCursor.getAttrValue("r");
          if (from != null && size != null && componentKey != null) {
            writeDuplication(refByComponentKey, from, size, componentKey, json);
          }
        }
        json.endArray().endObject();
      }
    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
  }

  private void writeDuplication(Map<String, String> refByComponentKey, String from, String size, String componentKey, JsonWriter json) {
    String ref = refByComponentKey.get(componentKey);
    if (ref == null) {
      ref = Integer.toString(refByComponentKey.size() + 1);
      refByComponentKey.put(componentKey, ref);
    }

    json.beginObject();
    json.prop("from", Integer.valueOf(from));
    json.prop("size", Integer.valueOf(size));
    json.prop("_ref", ref);
    json.endObject();
  }

  private void writeFiles(Map<String, String> refByComponentKey, JsonWriter json, DbSession session) {
    Map<Long, ComponentDto> projectById = newHashMap();
    for (Map.Entry<String, String> entry : refByComponentKey.entrySet()) {
      String componentKey = entry.getKey();
      String ref = entry.getValue();
      ComponentDto file = componentDao.getByKey(componentKey, session);
      if (file != null) {
        json.name(ref).beginObject();
        json.prop("key", file.key());
        json.prop("name", file.longName());
        ComponentDto project = projectById.get(file.projectId());
        if (project == null) {
          project = componentDao.getById(file.projectId(), session);
          projectById.put(file.projectId(), project);
        }
        json.prop("projectName", project != null ? project.longName() : null);
        json.endObject();
      }
    }
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

}
