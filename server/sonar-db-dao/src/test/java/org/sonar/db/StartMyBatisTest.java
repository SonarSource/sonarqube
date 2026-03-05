/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class StartMyBatisTest {

  @Test
  void should_start_mybatis_instance() {
    var myBatis = mock(DefaultMyBatis.class);
    var cluster = mock(DatabaseCluster.class);
    var writer = mock(Database.class);
    when(myBatis.getCluster()).thenReturn(cluster);
    when(cluster.getWriter()).thenReturn(writer);
    var startMyBatis = new StartMyBatis(myBatis);
    startMyBatis.start();
    verify(myBatis).start();
    verify(myBatis).getCluster();
    verify(cluster).getWriter();
    verify(writer).start();
    verifyNoMoreInteractions(myBatis);
  }


}
