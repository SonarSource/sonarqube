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
package org.sonar.db.purge;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class IdUuidPairs {
  private IdUuidPairs() {
    // prevents instantiation
  }

  public static List<Long> ids(List<IdUuidPair> pairs) {
    return pairs.stream().map(IdUuidPair::getId).collect(Collectors.toCollection(() -> new ArrayList<>(pairs.size())));
  }

  public static List<Long> ids(Iterable<IdUuidPair> pairs) {
    if (pairs instanceof List) {
      return ids((List<IdUuidPair>) pairs);
    }
    return ids(Lists.newArrayList(pairs));
  }

  public static List<String> uuids(List<IdUuidPair> pairs) {
    return pairs.stream().map(IdUuidPair::getUuid).collect(Collectors.toCollection(() -> new ArrayList<>(pairs.size())));
  }

  public static List<String> uuids(Iterable<IdUuidPair> pairs) {
    if (pairs instanceof List) {
      return uuids((List<IdUuidPair>) pairs);
    }
    return uuids(Lists.newArrayList(pairs));
  }

}
