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
import org.sonar.server.rule2.persistence.RuleDao;

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
    qualityProfileDao.insert(profile, session);

    RuleDto rule = RuleDto.createFor(RuleKey.of("repo","rule"));
    ruleDao.insert(rule, session);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(profile, rule)
      .setSeverity("BLOCKER");
    activeRuleDao.insert(activeRuleDto, session);

    session.commit();

    ActiveRuleDto result = activeRuleDao.getByKey(activeRuleDto.getKey(), session);

    assertThat(result).isNotNull();
    assertThat(result.getKey()).isNotNull();


  }
}