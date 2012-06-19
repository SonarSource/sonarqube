/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.dependency;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.sonar.core.persistence.DaoTestCase;
import org.sonar.core.persistence.MyBatis;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class DependencyMapperTest extends DaoTestCase {

  @Test
  public void testDescendantProjects_do_not_include_self() {
    setupData("fixture");

    SqlSession session = getMyBatis().openSession();
    try {
      List<DependencyDto> dependencies = session.getMapper(DependencyMapper.class).selectAll();

      assertThat(dependencies).hasSize(2);

      DependencyDto dep = dependencies.get(0);
      assertThat(dep.getUsage()).isEqualTo("compile");
      assertThat(dep.getFromResourceId()).isEqualTo(100L);
      assertThat(dep.getToResourceId()).isEqualTo(101L);
      assertThat(dep.getId()).isEqualTo(1L);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }


}

