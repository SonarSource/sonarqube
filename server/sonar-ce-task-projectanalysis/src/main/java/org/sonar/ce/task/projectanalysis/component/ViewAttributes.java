/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.ce.task.projectanalysis.component;

import java.util.Arrays;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.resources.Qualifiers;

import static java.util.Objects.requireNonNull;

@Immutable
public class ViewAttributes {

  public enum Type {
    PORTFOLIO(Qualifiers.VIEW), APPLICATION(Qualifiers.APP);

    private final String qualifier;

    Type(String qualifier) {
      this.qualifier = qualifier;
    }

    public String getQualifier() {
      return qualifier;
    }

    public static Type fromQualifier(String qualifier) {
      return Arrays.stream(values())
        .filter(type -> type.getQualifier().equals(qualifier))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(String.format("Qualifier '%s' is not supported", qualifier)));
    }
  }

  private final Type type;

  public ViewAttributes(Type type) {
    this.type = requireNonNull(type, "Type cannot be null");
  }

  public Type getType() {
    return type;
  }

  @Override
  public String toString() {
    return "viewAttributes{" +
      "type='" + type + '\'' +
      '}';
  }
}
