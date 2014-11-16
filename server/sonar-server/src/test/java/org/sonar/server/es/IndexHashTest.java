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
package org.sonar.server.es;

import org.junit.Test;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;

public class IndexHashTest {

  @Test
  public void of() throws Exception {
    NewIndex indexV1 = createIndex();
    String hashV1 = new IndexHash().of(indexV1);
    assertThat(hashV1).isNotEmpty();
    // always the same
    assertThat(hashV1).isEqualTo(new IndexHash().of(indexV1));

    NewIndex indexV2 = createIndex();
    indexV2.getMappings().get("fake").createIntegerField("max");
    String hashV2 = new IndexHash().of(indexV2);
    assertThat(hashV2).isNotEmpty().isNotEqualTo(hashV1);
  }

  private NewIndex createIndex() {
    NewIndex index = new NewIndex("fakes");
    NewIndex.NewMapping mapping = index.createMapping("fake");
    mapping.setAttribute("list_attr", Arrays.asList("foo", "bar"));
    mapping.stringFieldBuilder("key").build();
    mapping.createDateTimeField("updatedAt");
    return index;
  }

}
