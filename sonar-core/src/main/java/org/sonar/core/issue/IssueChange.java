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
package org.sonar.core.issue;

import com.google.common.collect.Maps;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Map;

public class IssueChange implements Serializable {

  public static class Diff<T extends Serializable> implements Serializable {
    private T before, after;

    public Diff(@Nullable T before, @Nullable T after) {
      this.before = before;
      this.after = after;
    }

    public T before() {
      return before;
    }

    public T after() {
      return after;
    }

    void setAfter(T t) {
      this.after = t;
    }
  }

  private Map<String, Diff> diffs = Maps.newLinkedHashMap();

  public Map<String, Diff> diffs() {
    return diffs;
  }

  @SuppressWarnings("unchecked")
  public void setDiff(String field, @Nullable Serializable oldValue, @Nullable Serializable newValue) {
    Diff diff = diffs.get(field);
    if (diff == null) {
      diff = new Diff(oldValue, newValue);
      diffs.put(field, diff);
    } else {
      diff.setAfter(newValue);
    }
  }
}
