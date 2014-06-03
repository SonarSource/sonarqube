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
package org.sonar.server.qualityprofile;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.log.db.LogDto;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.log.db.LogDao;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ActiveRuleChangeLogTest extends AbstractDaoTestCase{


  private LogDao dao;
  private DbSession session;
  private System2 system2;

  @Before
  public void before() throws Exception {
    this.session = getMyBatis().openSession(false);
    this.system2 = mock(System2.class);
    this.dao = new LogDao(system2);
  }

  @After
  public void after(){
    session.close();
  }

  @Test
  public void insert_log(){

    ActiveRuleKey ruleKey = ActiveRuleKey.of(
      QualityProfileKey.of("name", "java"),
      RuleKey.of("repository","S001"));
    ActiveRuleChange ruleChange = new ActiveRuleChange(ActiveRuleChange.Type.ACTIVATED, ruleKey)
      .setInheritance(ActiveRule.Inheritance.INHERITED);

    LogDto log = new LogDto("SYSTEM_USER", ruleChange);

    dao.insert(session, log);

    LogDto newDto = dao.getByKey(session, log.getKey());
    assertThat(newDto.getAuthor()).isEqualTo(log.getAuthor());

    ActiveRuleChange loggedRuleChange = newDto.getActivity();
    assertThat(loggedRuleChange.getKey()).isEqualTo(ruleKey);
    assertThat(ruleChange.getInheritance()).isEqualTo(ActiveRule.Inheritance.INHERITED);

  }
}