package org.sonar.server.configuration;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RulesBackupTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldExportRules() {
    SonarConfig sonarConfig = new SonarConfig();

    Rule rule = Rule.create("repo", "key", "name").setDescription("description");

    Rule userRule = Rule.create("repo", "key2", "name2").setDescription("description2");
    userRule.setParent(rule);
    userRule.createParameter("param").setDefaultValue("value");

    RulesBackup rulesBackup = new RulesBackup(Arrays.asList(userRule));
    rulesBackup.exportXml(sonarConfig);

    assertThat(sonarConfig.getRules().size(), is(1));
    assertTrue(sonarConfig.getRules().iterator().next() == userRule);
  }

  @Test
  public void shouldImportRules() {
    RulesDao rulesDao = getDao().getRulesDao();

    RulesBackup rulesBackup = new RulesBackup(getSession());
    SonarConfig sonarConfig = new SonarConfig();

    Rule rule = Rule.create("repo", "key", "name").setDescription("description");

    Rule userRule = Rule.create("repo", "key2", "name2").setDescription("description2");
    userRule.setParent(rule);
    userRule.createParameter("param").setDefaultValue("value");

    getSession().save(rule);

    sonarConfig.setRules(Arrays.asList(userRule));
    rulesBackup.importXml(sonarConfig);

    List<Rule> rules = rulesDao.getRules();
    assertThat(rules.size(), is(2));
  }
}
