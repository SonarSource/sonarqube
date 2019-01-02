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
package org.sonar.db.purge;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.sonar.test.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class IdUuidPairsTest {
  @Test
  public void extract_ids() {
    List<IdUuidPair> idUuidPairList = Lists.newArrayList(new IdUuidPair(1L, "ABCD"), new IdUuidPair(2L, "EFGH"));

    List<Long> ids = IdUuidPairs.ids(idUuidPairList);

    assertThat(ids).containsOnly(1L, 2L);
  }

  @Test
  public void is_non_instantiable() {
    assertThat(TestUtils.hasOnlyPrivateConstructors(IdUuidPairs.class)).isTrue();
  }

  @Test
  public void extract_uuids() {
    List<IdUuidPair> idUuidPairList = Lists.newArrayList(new IdUuidPair(1L, "ABCD"), new IdUuidPair(2L, "EFGH"));

    List<String> uuids = IdUuidPairs.uuids(idUuidPairList);

    assertThat(uuids).containsOnly("ABCD", "EFGH");
  }
}
