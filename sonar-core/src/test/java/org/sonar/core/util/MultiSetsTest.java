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

package org.sonar.core.util;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiSetsTest {
  @Test
  public void order_with_highest_count_first() throws Exception {
    Multiset<String> multiset = HashMultiset.create();
    add(multiset, "seneca", 10);
    add(multiset, "plato", 5);
    add(multiset, "confucius", 3);

    List<Multiset.Entry<String>> orderedEntries = MultiSets.listOrderedByHighestCounts(multiset);

    assertThat(orderedEntries).extracting("element").containsExactly("seneca", "plato", "confucius");
  }

  private void add(Multiset<String> multiset, String element, int times) {
    for (int i = 0; i < times; i++) {
      multiset.add(element);
    }
  }
}
