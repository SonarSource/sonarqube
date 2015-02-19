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
package org.sonar.process.monitor;

/**
 * Created by eric on 20/02/15.
 */
public enum KnownJavaCommand {
  APP("app", 0), WEB("web", 1), ELASTIC_SEARCH("search", 2), UNKNOWN("unknown", -1);

  private String key;
  private int index;

  KnownJavaCommand(String key, int index) {
    this.key = key;
    this.index = index;
  }

  public String getKey() {
    return key;
  }

  public int getIndex() {
    return index;
  }

  public static KnownJavaCommand lookFor(String key) {
    for (KnownJavaCommand knownJavaCommand : KnownJavaCommand.values()) {
      if (knownJavaCommand.getKey().equals(key)) {
        return knownJavaCommand;
      }
    }
    return KnownJavaCommand.UNKNOWN;
  }

  public static int lookIndexFor(String key) {
    return lookFor(key).getIndex();
  }

  public static int getFirstIndexAvailable() {
    int result = 0;
    for (KnownJavaCommand knownJavaCommand : KnownJavaCommand.values()) {
      result = knownJavaCommand.getIndex() >= result ? knownJavaCommand.getIndex() + 1 : result;
    }
    return result;
  }
}
