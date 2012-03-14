/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.diff;

import com.google.common.base.Objects;

public class Edit {

  public static enum Type {
    INSERT,
    MOVE
  }

  int beginA;
  int endA;
  int beginB;
  int endB;
  final Type type;

  public Edit(Type type, int beginA, int endA, int beginB, int endB) {
    this.beginA = beginA;
    this.endA = endA;
    this.beginB = beginB;
    this.endB = endB;
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Edit)) {
      return false;
    }
    Edit e = (Edit) obj;
    return type == e.type
      && beginA == e.beginA
      && endA == e.endA
      && beginB == e.beginB
      && endB == e.endB;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("type", type)
        .add("beginA", beginA)
        .add("endA", endA)
        .add("beginB", beginB)
        .add("endB", endB)
        .toString();
  }

}
