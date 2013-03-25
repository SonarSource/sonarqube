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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class RulesBackup implements Backupable {
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
        "key", rule.getKey());
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
        writeNode(writer, "parentRepositoryKey", rule.getParent().getRepositoryKey());
        writeNode(writer, "parentKey", rule.getParent().getKey());
        writeNode(writer, "repositoryKey", rule.getRepositoryKey());
        writeNode(writer, "key", rule.getKey());
        writeNode(writer, "configKey", rule.getConfigKey());
        writeNode(writer, "level", rule.getSeverity().name());
        writeNode(writer, "name", rule.getName());
        writeNode(writer, "description", rule.getDescription());

        if (!rule.getParams().isEmpty()) {
          writer.startNode("params");
          for (RuleParam ruleParam : rule.getParams()) {
            writer.startNode("param");
            writeNode(writer, "key", ruleParam.getKey());
            writeNode(writer, "value", ruleParam.getDefaultValue());
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
          if ("params".equals(reader.getNodeName())) {
            while (reader.hasMoreChildren()) {
              reader.moveDown();
              Map<String, String> valuesParam = readNode(reader);
              rule.createParameter()
                .setKey(valuesParam.get("key"))
                .setDefaultValue(valuesParam.get("value"));
              reader.moveUp();
            }
          }
          reader.moveUp();
        }

        Rule parent = Rule.create()
          .setRepositoryKey(valuesRule.get("parentRepositoryKey"))
          .setKey(valuesRule.get("parentKey"));
        rule.setParent(parent)
          .setRepositoryKey(valuesRule.get("repositoryKey"))
          .setKey(valuesRule.get("key"))
          .setConfigKey(valuesRule.get("configKey"))
          .setName(valuesRule.get("name"))
          .setDescription(valuesRule.get("description"))
          .setSeverity(RulePriority.valueOf(valuesRule.get("level")));
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
