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
package org.sonar.db;

import org.apache.ibatis.session.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.db.rule.RuleMapper;

import static org.assertj.core.api.Assertions.assertThat;


public class MyBatisTest {
  private static SQDatabase database;

  @BeforeClass
  public static void start() {
    database = SQDatabase.newH2Database("sonar2", true);
    database.start();
  }

  @AfterClass
  public static void stop() {
    database.stop();
  }

  private MyBatis underTest = new MyBatis(database);

  @Test
  public void shouldConfigureMyBatis() {
    underTest.start();

    Configuration conf = underTest.getSessionFactory().getConfiguration();

    assertThat(conf.isUseGeneratedKeys()).isFalse();
    assertThat(conf.hasMapper(RuleMapper.class)).isTrue();
    assertThat(conf.isLazyLoadingEnabled()).isFalse();
  }

  @Test
  public void shouldOpenBatchSession() {
    underTest.start();

    try (DbSession session = underTest.openSession(false)) {
      assertThat(session.getConnection()).isNotNull();
      assertThat(session.getMapper(RuleMapper.class)).isNotNull();
    }
  }
}
