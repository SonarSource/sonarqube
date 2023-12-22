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
package org.sonar.alm.client.gitlab;

import java.util.Collection;
import org.junit.Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class GsonIdTest {

  @Test
  public void parseOne_whenSingleId_deserializesCorrectly() {
    GsonId gsonId = GsonId.parseOne("""
      {
        "blu": "blue",
        "id": "123",
        "bla": "bli"
      }
      """);

    assertThat(gsonId.getId()).isEqualTo(123);
  }

  @Test
  public void parseCollection_whenMultipleIds_deserializeCorrectly() {
    Collection<GsonId> gsonId = GsonId.parseCollection("""
      [
      {
        "blu": "blue",
        "id": "123",
        "bla": "bli"
      },
              
      {
        "id": "456",
        "bla": "bli"
      }
      ]
      """);

    assertThat(gsonId.stream().map(GsonId::getId).collect(toSet())).containsExactlyInAnyOrder(123L, 456L);
  }

}
