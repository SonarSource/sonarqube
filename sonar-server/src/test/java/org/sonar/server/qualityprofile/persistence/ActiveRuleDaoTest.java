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
package org.sonar.server.qualityprofile.persistence;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.server.rule.db.RuleDao;

import static org.fest.assertions.Assertions.assertThat;

public class ActiveRuleDaoTest  extends AbstractDaoTestCase{

  ActiveRuleDao activeRuleDao;
  RuleDao ruleDao;
  QualityProfileDao qualityProfileDao;
  DbSession session;


  @Before
  public void setUp() throws Exception {
    session = getMyBatis().openSession(false);
    ruleDao = new RuleDao(System2.INSTANCE);
    qualityProfileDao = new QualityProfileDao(getMyBatis());
    activeRuleDao = new ActiveRuleDao(qualityProfileDao, ruleDao, System2.INSTANCE);
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_by_key() throws Exception {

    QualityProfileDto profile = QualityProfileDto.createFor("profile","xoo");
    qualityProfileDao.insert(session, profile);

    RuleDto rule = RuleDto.createFor(RuleKey.of("repo","rule"));
    ruleDao.insert(session, rule);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(profile, rule)
      .setSeverity("BLOCKER");
    activeRuleDao.insert(session, activeRuleDto);

    session.commit();

    ActiveRuleDto result = activeRuleDao.getByKey(session, activeRuleDto.getKey());

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isNotNull();
  }
}
