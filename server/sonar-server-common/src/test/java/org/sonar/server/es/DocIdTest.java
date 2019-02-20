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
package org.sonar.server.es;

import org.junit.Test;

import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

public class DocIdTest {
  @Test
  public void equals_is_based_on_index_type_and_id() {
    String index = randomAlphabetic(5);
    String type = randomAlphabetic(6);
    String id = randomAlphabetic(7);
    DocId underTest = new DocId(index, type, id);

    assertThat(underTest)
      .isEqualTo(new DocId(index, type, id))
      .isNotEqualTo(new DocId(randomAlphabetic(7), type, id))
      .isNotEqualTo(new DocId(index, type, randomAlphabetic(7)))
      .isNotEqualTo(new DocId(index, randomAlphabetic(7), id))
      .isNotEqualTo(new DocId(randomAlphabetic(7), randomAlphabetic(8), id))
      .isNotEqualTo(new DocId(randomAlphabetic(7), type, randomAlphabetic(8)))
      .isNotEqualTo(new DocId(index, randomAlphabetic(7), randomAlphabetic(8)))
      .isNotEqualTo(new DocId(randomAlphabetic(7), randomAlphabetic(8), randomAlphabetic(9)));
  }

  @Test
  public void hashcode_is_based_on_index_type_and_id() {
    String index = randomAlphabetic(5);
    String type = randomAlphabetic(6);
    String id = randomAlphabetic(7);
    DocId underTest = new DocId(index, type, id);

    assertThat(underTest.hashCode())
      .isEqualTo(new DocId(index, type, id).hashCode())
      .isNotEqualTo(new DocId(randomAlphabetic(7), type, id).hashCode())
      .isNotEqualTo(new DocId(index, type, randomAlphabetic(7)).hashCode())
      .isNotEqualTo(new DocId(index, randomAlphabetic(7), id).hashCode())
      .isNotEqualTo(new DocId(randomAlphabetic(7), randomAlphabetic(8), id).hashCode())
      .isNotEqualTo(new DocId(randomAlphabetic(7), type, randomAlphabetic(8)).hashCode())
      .isNotEqualTo(new DocId(index, randomAlphabetic(7), randomAlphabetic(8)).hashCode())
      .isNotEqualTo(new DocId(randomAlphabetic(7), randomAlphabetic(8), randomAlphabetic(9)).hashCode());
  }
}
