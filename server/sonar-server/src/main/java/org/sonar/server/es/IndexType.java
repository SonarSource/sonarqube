/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.es;

import java.util.Arrays;
import java.util.function.Function;
import org.sonar.core.util.stream.MoreCollectors;

import static java.util.Objects.requireNonNull;

public class IndexType {

  private final String index;
  private final String type;

  public IndexType(String index, String type) {
    this.index = requireNonNull(index);
    this.type = requireNonNull(type);
  }

  public String getIndex() {
    return index;
  }

  public String getType() {
    return type;
  }

  public static String[] getIndices(IndexType... indexTypes) {
    return getDetails(IndexType::getIndex, indexTypes);
  }

  public static String[] getTypes(IndexType... indexTypes) {
    return getDetails(IndexType::getType, indexTypes);
  }

  private static String[] getDetails(Function<? super IndexType, ? extends String> function, IndexType... indexTypes) {
    return Arrays.stream(indexTypes).map(function).collect(MoreCollectors.toSet(indexTypes.length)).toArray(new String[0]);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IndexType that = (IndexType) o;
    if (!index.equals(that.index)) {
      return false;
    }
    return type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = index.hashCode();
    result = 31 * result + type.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "[" + index + "/" + type + "]";
  }
}
