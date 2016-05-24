/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.exceptions;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.i18n.I18n;
import org.sonar.api.utils.text.JsonWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Errors {

  private final List<Message> messages = Lists.newArrayList();

  public Errors() {
  }

  public Errors add(Errors errors) {
    this.messages.addAll(errors.messages());
    return this;
  }

  public Errors add(Collection<Message> messages) {
    for (Message message : messages) {
      add(message);
    }
    return this;
  }

  public Errors add(Message message, Message... others) {
    messages.add(message);
    Collections.addAll(messages, others);
    return this;
  }

  public List<Message> messages() {
    return messages;
  }

  public boolean isEmpty() {
    return messages.isEmpty();
  }

  public boolean check(boolean expression, String l10nKey, Object... l10nParams) {
    if (!expression) {
      add(Message.of(l10nKey, l10nParams));
    }
    return expression;
  }

  public void writeJson(JsonWriter json, I18n i18n, Locale locale) {
    writeJson(json, i18n, locale, "errors");
  }

  public void writeJsonAsWarnings(JsonWriter json, I18n i18n, Locale locale) {
    writeJson(json, i18n, locale, "warnings");
  }

  private void writeJson(JsonWriter json, I18n i18n, Locale locale, String name) {
    if (!messages.isEmpty()) {
      json.name(name).beginArray();
      for (Message message : messages) {
        json.beginObject();
        String text = StringUtils.defaultString(i18n.message(locale, message.getKey(), message.getKey(), message.getParams()), message.getKey());
        json.prop("msg", text);
        json.endObject();
      }
      json.endArray();
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("messages", messages)
      .toString();
  }
}
