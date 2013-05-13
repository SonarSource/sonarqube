/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

package org.sonar.server.issue;

import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class Result<T> {

  private T object;
  private List<Message> errors;

  public Result() {
    errors = newArrayList();
  }

  public Result(T object) {
    this();
    this.object = object;
  }

  public void setObject(T object){
    this.object = object;
  }

  public T get() {
    return object;
  }

  public void addError(String text, Object... params){
    Message message = new Message(text, params);
    errors.add(message);
  }

  public List<Message> errors(){
    return errors;
  }

  public boolean ok(){
    return errors.isEmpty();
  }

  public static class Message {
    private String text;
    private Object[] params;

    public Message(String text, Object... params) {
      this.text = text;
      this.params = params;
    }

    public String text() {
      return text;
    }

    public Object[] params() {
      return params;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Message message = (Message) o;

      if (!Arrays.equals(params, message.params)) {
        return false;
      }
      if (text != null ? !text.equals(message.text) : message.text != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = text != null ? text.hashCode() : 0;
      result = 31 * result + (params != null ? Arrays.hashCode(params) : 0);
      return result;
    }
  }
}
