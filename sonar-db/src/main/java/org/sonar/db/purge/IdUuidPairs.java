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

package org.sonar.db.purge;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nonnull;

public class IdUuidPairs {
  private IdUuidPairs() {
  }

  public static List<Long> ids(List<IdUuidPair> pairs) {
    return Lists.transform(pairs, new IdUuidPairToIdFunction());
  }

  public static List<String> uuids(List<IdUuidPair> pairs) {
    return Lists.transform(pairs, new IdUuidPairToUuidFunction());
  }

  private static class IdUuidPairToIdFunction implements Function<IdUuidPair, Long> {
    @Override
    public Long apply(@Nonnull IdUuidPair pair) {
      return pair.getId();
    }
  }

  private static class IdUuidPairToUuidFunction implements Function<IdUuidPair, String> {
    @Override
    public String apply(@Nonnull IdUuidPair pair) {
      return pair.getUuid();
    }
  }
}
