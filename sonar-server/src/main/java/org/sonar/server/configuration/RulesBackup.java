/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.configuration;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.jpa.dao.RulesDao;

import java.util.*;

public class RulesBackup implements Backupable {

  private static final String PARENT_REPOSITORY_KEY = "parentRepositoryKey";
  private static final String PARENT_KEY = "parentKey";
  private static final String REPOSITORY_KEY = "repositoryKey";
  private static final String KEY = "key";
  private static final String CONFIG_KEY = "configKey";
  private static final String LEVEL = "level";
  private static final String NAME = "name";
  private static final String DESCRIPTION = "description";
  private static final String PARAMS = "params";
  private static final String VALUE = "value";

  private Collection<Rule> rules;
  private RulesDao rulesDao;
  private DatabaseSession session;

  public RulesBackup(DatabaseSession session) {
    this.rulesDao = new RulesDao(session);
    this.session = session;
  }

  /**
   * For tests.
   */
  RulesBackup(Collection<Rule> rules) {
    this.rules = rules;
  }

  public void exportXml(SonarConfig sonarConfig) {
    if (rules == null) {
      rules = getUserRules();
    }
    sonarConfig.setRules(rules);
  }

  public void importXml(SonarConfig sonarConfig) {
    disableUserRules();
    if (sonarConfig.getRules() != null && !sonarConfig.getRules().isEmpty()) {
      registerUserRules(sonarConfig.getRules());
    }
  }

  private List<Rule> getUserRules() {
    List<Rule> userRules = Lists.newArrayList();
    for (Rule rule : rulesDao.getRules()) {
      if (rule.getParent() != null) {
        userRules.add(rule);
      }
    }
    return userRules;
  }

  private void disableUserRules() {
    LoggerFactory.getLogger(getClass()).info("Disable rules created by user");
    for (Rule rule : getUserRules()) {
      rule.setStatus(Rule.STATUS_REMOVED);
      session.save(rule);
    }
  }

  private void registerUserRules(Collection<Rule> rules) {
    LoggerFactory.getLogger(getClass()).info("Restore rules");
    for (Rule rule : rules) {
      Rule parent = rule.getParent();
      Rule matchingParentRuleInDb = rulesDao.getRuleByKey(parent.getRepositoryKey(), parent.getKey());
      if (matchingParentRuleInDb == null) {
        LoggerFactory.getLogger(getClass()).error("Unable to find parent rule " + parent.getRepositoryKey() + ":" + parent.getKey());
        continue;
      }

      for (Iterator<RuleParam> irp = rule.getParams().iterator(); irp.hasNext(); ) {
        RuleParam param = irp.next();
        RuleParam matchingRPInDb = rulesDao.getRuleParam(matchingParentRuleInDb, param.getKey());
        if (matchingRPInDb == null) {
          LoggerFactory.getLogger(getClass()).error("Unable to find rule parameter in parent " + param.getKey());
          irp.remove();
        }
      }

      rule.setParent(matchingParentRuleInDb);
      rule.setLanguage(matchingParentRuleInDb.getLanguage());
      Rule matchingRuleInDb = session.getSingleResult(Rule.class,
        "pluginName", rule.getRepositoryKey(),
        KEY, rule.getKey());
      if (matchingRuleInDb != null) {
        // merge
        matchingRuleInDb.setParent(matchingParentRuleInDb);
        matchingRuleInDb.setConfigKey(rule.getConfigKey());
        matchingRuleInDb.setName(rule.getName());
        matchingRuleInDb.setDescription(rule.getDescription());
        matchingRuleInDb.setSeverity(rule.getSeverity());
        matchingRuleInDb.setParams(rule.getParams());
        matchingRuleInDb.setStatus(Rule.STATUS_READY);
        matchingRuleInDb.setLanguage(rule.getLanguage());
        matchingRuleInDb.setUpdatedAt(new Date());
        session.save(matchingRuleInDb);
      } else {
        rule.setStatus(Rule.STATUS_READY);
        rule.setCreatedAt(new Date());
        session.save(rule);
      }
    }
  }

  public void configure(XStream xStream) {
    xStream.alias("rule", Rule.class);
    xStream.registerConverter(new Converter() {
      public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Rule rule = (Rule) source;
        writeNode(writer, PARENT_REPOSITORY_KEY, rule.getParent().getRepositoryKey());
        writeNode(writer, PARENT_KEY, rule.getParent().getKey());
        writeNode(writer, REPOSITORY_KEY, rule.getRepositoryKey());
        writeNode(writer, KEY, rule.getKey());
        writeNode(writer, CONFIG_KEY, rule.getConfigKey());
        writeNode(writer, LEVEL, rule.getSeverity().name());
        writeNode(writer, NAME, rule.getName());
        writeNode(writer, DESCRIPTION, rule.getDescription());

        if (!rule.getParams().isEmpty()) {
          writer.startNode(PARAMS);
          for (RuleParam ruleParam : rule.getParams()) {
            writer.startNode("param");
            writeNode(writer, KEY, ruleParam.getKey());
            writeNode(writer, VALUE, ruleParam.getDefaultValue());
            writer.endNode();
          }
          writer.endNode();
        }
      }

      public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Rule rule = Rule.create();

        Map<String, String> valuesRule = new HashMap<String, String>();
        while (reader.hasMoreChildren()) {
          reader.moveDown();
          valuesRule.put(reader.getNodeName(), reader.getValue());
          if (PARAMS.equals(reader.getNodeName())) {
            while (reader.hasMoreChildren()) {
              reader.moveDown();
              Map<String, String> valuesParam = readNode(reader);
              rule.createParameter()
                .setKey(valuesParam.get(KEY))
                .setDefaultValue(valuesParam.get(VALUE));
              reader.moveUp();
            }
          }
          reader.moveUp();
        }

        Rule parent = Rule.create()
          .setRepositoryKey(valuesRule.get(PARENT_REPOSITORY_KEY))
          .setKey(valuesRule.get(PARENT_KEY));
        rule.setParent(parent)
          .setRepositoryKey(valuesRule.get(REPOSITORY_KEY))
          .setKey(valuesRule.get(KEY))
          .setConfigKey(valuesRule.get(CONFIG_KEY))
          .setName(valuesRule.get(NAME))
          .setDescription(valuesRule.get(DESCRIPTION))
          .setSeverity(RulePriority.valueOf(valuesRule.get(LEVEL)));
        return rule;
      }

      public boolean canConvert(Class type) {
        return Rule.class.equals(type);
      }
    });
  }

  private void writeNode(HierarchicalStreamWriter writer, String name, String value) {
    writer.startNode(name);
    writer.setValue(value);
    writer.endNode();
  }

  private Map<String, String> readNode(HierarchicalStreamReader reader) {
    Map<String, String> values = new HashMap<String, String>();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      values.put(reader.getNodeName(), reader.getValue());
      reader.moveUp();
    }
    return values;
  }
}
