/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;

public class DocIdTest {
  @Test
  public void equals_is_based_on_index_type_and_id() {
    String index = secure().nextAlphabetic(5);
    String type = secure().nextAlphabetic(6);
    String id = secure().nextAlphabetic(7);
    DocId underTest = new DocId(index, type, id);

    assertThat(underTest)
      .isEqualTo(new DocId(index, type, id))
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), type, id))
      .isNotEqualTo(new DocId(index, type, secure().nextAlphabetic(7)))
      .isNotEqualTo(new DocId(index, secure().nextAlphabetic(7), id))
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), secure().nextAlphabetic(8), id))
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), type, secure().nextAlphabetic(8)))
      .isNotEqualTo(new DocId(index, secure().nextAlphabetic(7), secure().nextAlphabetic(8)))
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), secure().nextAlphabetic(8), secure().nextAlphabetic(9)));
  }

  @Test
  public void hashcode_is_based_on_index_type_and_id() {
    String index = secure().nextAlphabetic(5);
    String type = secure().nextAlphabetic(6);
    String id = secure().nextAlphabetic(7);
    DocId underTest = new DocId(index, type, id);

    assertThat(underTest.hashCode())
      .isEqualTo(new DocId(index, type, id).hashCode())
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), type, id).hashCode())
      .isNotEqualTo(new DocId(index, type, secure().nextAlphabetic(7)).hashCode())
      .isNotEqualTo(new DocId(index, secure().nextAlphabetic(7), id).hashCode())
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), secure().nextAlphabetic(8), id).hashCode())
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), type, secure().nextAlphabetic(8)).hashCode())
      .isNotEqualTo(new DocId(index, secure().nextAlphabetic(7), secure().nextAlphabetic(8)).hashCode())
      .isNotEqualTo(new DocId(secure().nextAlphabetic(7), secure().nextAlphabetic(8), secure().nextAlphabetic(9)).hashCode());
  }
}
