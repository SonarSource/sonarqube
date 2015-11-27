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
package org.sonar.search.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.script.AbstractExecutableScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.sonar.process.ProcessProperties;

public class ListUpdate extends AbstractExecutableScript {

  public static class UpdateListScriptFactory implements NativeScriptFactory {
    @Override
    public ExecutableScript newScript(@Nullable Map<String, Object> params) {
      String idField = XContentMapValues.nodeStringValue(params.get(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD), null);
      String idValue = XContentMapValues.nodeStringValue(params.get(ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE), null);
      String field = XContentMapValues.nodeStringValue(params.get(ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD), null);
      Map value = null;
      if (idField == null) {
        throw new IllegalStateException(String.format("Missing '%s' parameter", ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_FIELD));
      }
      if (idValue == null) {
        throw new IllegalStateException(String.format("Missing '%s' parameter", ProcessProperties.ES_PLUGIN_LISTUPDATE_ID_VALUE));
      }
      if (field == null) {
        throw new IllegalStateException(String.format("Missing '%s' parameter", ProcessProperties.ES_PLUGIN_LISTUPDATE_FIELD));
      }

      // NULL case is deletion of nested item
      if (params.containsKey(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE)) {
        Object obj = params.get(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE);
        if (obj != null) {
          value = XContentMapValues.nodeMapValue(params.get(ProcessProperties.ES_PLUGIN_LISTUPDATE_VALUE), "Update item");
        }
      }

      return new ListUpdate(idField, idValue, field, value);
    }
  }

  private final String idField;
  private final String idValue;
  private final String field;
  private final Map<String, Object> value;
  private Map<String, Object> ctx;

  public ListUpdate(String idField, String idValue, String field, @Nullable Map<String, Object> value) {
    this.idField = idField;
    this.idValue = idValue;
    this.field = field;
    this.value = value;
  }

  @Override
  public void setNextVar(String name, Object value) {
    if ("ctx".equals(name)) {
      ctx = (Map<String, Object>) value;
    }
  }

  @Override
  public Object unwrap(Object value) {
    return value;
  }

  @Override
  public Object run() {
    try {
      // Get the Document's source from ctx
      Map<String, Object> source = XContentMapValues.nodeMapValue(ctx.get("_source"), "source from context");

      // Get the Object for list update
      Object fieldValue = source.get(field);

      if (fieldValue == null && value != null) {
        // 0. The field does not exist (this is a upsert then)
        List values = new ArrayList<>(1);
        values.add(value);
        source.put(field, values);
      } else if (!XContentMapValues.isArray(fieldValue) && value != null) {
        // 1. The field is not yet a list
        Map currentFieldValue = XContentMapValues.nodeMapValue(fieldValue, "current FieldValue");
        if (XContentMapValues.nodeStringValue(currentFieldValue.get(idField), null).equals(idValue)) {
          source.put(field, value);
        } else {
          List values = new ArrayList<>(2);
          values.add(fieldValue);
          values.add(value);
          source.put(field, values);
        }
      } else {
        // 3. field is a list
        Collection items = (Collection) fieldValue;
        Object target = null;
        for (Object item : items) {
          Map<String, Object> fields = (Map<String, Object>) item;
          String itemIdValue = XContentMapValues.nodeStringValue(fields.get(idField), null);
          if (itemIdValue != null && itemIdValue.equals(idValue)) {
            target = item;
            break;
          }
        }
        if (target != null) {
          items.remove(target);
        }

        // Supporting the update by NULL = deletion case
        if (value != null) {
          items.add(value);
        }
        source.put(field, items);
      }
    } catch (Exception e) {
      throw new IllegalStateException("failed to execute listUpdate script", e);
    }
    return null;

  }
}
