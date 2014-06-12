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
package org.sonar.server.exceptions;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.sonar.api.utils.text.JsonWriter;

import java.util.Collection;
import java.util.List;

public class Messages {

  private ListMultimap<Message.Level, Message> messagesByLevel = ArrayListMultimap.create();

  public Messages add(Message message) {
    messagesByLevel.put(message.getLevel(), message);
    return this;
  }

  public Collection<Message> all() {
    return messagesByLevel.values();
  }

  public List<Message> getByLevel(Message.Level level) {
    return messagesByLevel.get(level);
  }

  public boolean hasLevel(Message.Level level) {
    return messagesByLevel.containsKey(level);
  }

  public void writeJson(JsonWriter json) {
    writeJson(json, "errors", Message.Level.ERROR);
    writeJson(json, "warnings", Message.Level.WARNING);
    writeJson(json, "infos", Message.Level.INFO);
  }

  private void writeJson(JsonWriter json, String label, Message.Level level) {
    List<Message> messages = messagesByLevel.get(level);
    if (!messages.isEmpty()) {
      json.name(label).beginArray();
      for (Message message : messages) {
        json.beginObject().prop("msg", message.getLabel()).endObject();
      }
      json.endArray();
    }
  }
}
