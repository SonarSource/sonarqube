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
package org.sonar.core.design;

import com.google.common.collect.Lists;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DependencyMapperTest {

  @ClassRule
  public static DbTester dbtester = new DbTester();

  DbSession session;

  @Before
  public void setUp() throws Exception {
    dbtester.truncateTables();
    session = dbtester.myBatis().openSession(false);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void select_all_dependencies() {
    dbtester.prepareDbUnit(getClass(), "fixture.xml");

    final List<DependencyDto> dependencies = Lists.newArrayList();

    session.getMapper(DependencyMapper.class).selectAll(new ResultHandler() {
      public void handleResult(ResultContext context) {
        dependencies.add((DependencyDto) context.getResultObject());
      }
    });

    assertThat(dependencies).hasSize(2);

    DependencyDto dep = dependencies.get(0);
    assertThat(dep.getId()).isEqualTo(1L);
    assertThat(dep.getFromSnapshotId()).isEqualTo(1000L);
    assertThat(dep.getToSnapshotId()).isEqualTo(1001L);
    assertThat(dep.getUsage()).isEqualTo("compile");
  }
}
