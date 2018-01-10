/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component.index;

import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.server.es.EsTester;

public class NewTest {

  @Rule
  public EsTester es = new EsTester(new ComponentIndexDefinition(new MapSettings().asConfig()));

  @Test
  public void name() {
    IndicesExistsResponse x = es.client().prepareIndicesExist("components").get();
    System.out.println(x.isExists());
    IndicesExistsResponse x2 = es.client().prepareIndicesExist("components").get();
    System.out.println(x2.isExists());
  }
}
