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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.measures.Metric;
import org.sonar.api.profiles.Alert;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.core.dryrun.DryRunCache;
import org.sonar.jpa.dao.RulesDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProfilesBackup implements Backupable {

  private static final String OPERATOR = "operator";
  private static final String VALUE_ERROR = "value-error";
  private static final String VALUE_WARNING = "value-warning";
  private static final String PERIOD = "period";
  private static final String METRIC_KEY = "metric-key";
  private static final String KEY = "key";
  private static final String PLUGIN = "plugin";
  private static final String LEVEL = "level";
  private static final String INHERITANCE = "inheritance";
  private static final String PARAMS = "params";
  private static final String VALUE = "value";

  private Collection<RulesProfile> profiles;
  private DatabaseSession session;
  private DryRunCache dryRunCache;

  public ProfilesBackup(DatabaseSession session, DryRunCache dryRunCache) {
    this.session = session;
    this.dryRunCache = dryRunCache;
  }

  /**
   * for unit tests
   */
  ProfilesBackup(Collection<RulesProfile> profiles) {
    this.profiles = profiles;
  }

  public void configure(XStream xStream) {
    String defaultProfile = "defaultProfile";

    xStream.alias("profile", RulesProfile.class);
    xStream.alias("alert", Alert.class);
    xStream.alias("active-rule", ActiveRule.class);
    xStream.aliasField("active-rules", RulesProfile.class, "activeRules");
    xStream.aliasField("default-profile", RulesProfile.class, defaultProfile);
    xStream.omitField(RulesProfile.class, "id");
    xStream.omitField(RulesProfile.class, "projects");
    xStream.omitField(RulesProfile.class, "provided");
    xStream.omitField(RulesProfile.class, defaultProfile);
    xStream.omitField(RulesProfile.class, "enabled");
    xStream.registerConverter(getActiveRuleConverter());
    xStream.registerConverter(getAlertsConverter());
  }

  public void exportXml(SonarConfig sonarConfig) {
    this.profiles = (this.profiles == null ? session.getResults(RulesProfile.class) : this.profiles);
    // the profiles objects must be cloned to avoid issues CGLib
    List<RulesProfile> cloned = new ArrayList<RulesProfile>();
    for (RulesProfile profile : this.profiles) {
      cloned.add((RulesProfile) profile.clone());
    }

    sonarConfig.setProfiles(cloned);
  }

  public void importXml(SonarConfig sonarConfig) {
    if (sonarConfig.getProfiles() != null && !sonarConfig.getProfiles().isEmpty()) {
      LoggerFactory.getLogger(getClass()).info("Delete profiles");
      ProfilesManager profilesManager = new ProfilesManager(session, null, dryRunCache);
      profilesManager.deleteAllProfiles();

      RulesDao rulesDao = new RulesDao(session);
      for (RulesProfile profile : sonarConfig.getProfiles()) {
        LoggerFactory.getLogger(getClass()).info("Restore profile " + profile.getName());
        importProfile(rulesDao, profile);
      }
    }
  }

  public void importProfile(RulesDao rulesDao, RulesProfile toImport) {
    if (toImport.getVersion() == 0) {
      // backward-compatibility with versions < 2.9. The field "version" did not exist. Default value is 1.
      toImport.setVersion(1);
    }
    if (toImport.getUsed() == null) {
      // backward-compatibility with versions < 2.9. The field "used_profile" did not exist. Default value is false.
      toImport.setUsed(false);
    }
    importActiveRules(rulesDao, toImport);
    importAlerts(toImport);
    session.save(toImport);
  }

  private void importAlerts(RulesProfile profile) {
    if (profile.getAlerts() != null) {
      for (Iterator<Alert> ia = profile.getAlerts().iterator(); ia.hasNext();) {
        Alert alert = ia.next();
        Metric unMarshalledMetric = alert.getMetric();
        String validKey = unMarshalledMetric.getKey();
        Metric matchingMetricInDb = session.getSingleResult(Metric.class, KEY, validKey);
        if (matchingMetricInDb == null) {
          LoggerFactory.getLogger(getClass()).error("Unable to find metric " + validKey);
          ia.remove();
          continue;
        }
        alert.setMetric(matchingMetricInDb);
        alert.setRulesProfile(profile);
      }
    }
  }

  private void importActiveRules(RulesDao rulesDao, RulesProfile profile) {
    for (Iterator<ActiveRule> iar = profile.getActiveRules(true).iterator(); iar.hasNext();) {
      ActiveRule activeRule = iar.next();
      Rule unMarshalledRule = activeRule.getRule();
      Rule matchingRuleInDb = rulesDao.getRuleByKey(unMarshalledRule.getRepositoryKey(), unMarshalledRule.getKey());
      if (matchingRuleInDb == null) {
        LoggerFactory.getLogger(getClass()).error(
          "Unable to find active rule " + unMarshalledRule.getRepositoryKey() + ":" + unMarshalledRule.getKey());
        iar.remove();
      } else {
        activeRule.setRule(matchingRuleInDb);
        activeRule.setRulesProfile(profile);
        activeRule.getActiveRuleParams();
        for (Iterator<ActiveRuleParam> irp = activeRule.getActiveRuleParams().iterator(); irp.hasNext();) {
          ActiveRuleParam activeRuleParam = irp.next();
          RuleParam unMarshalledRP = activeRuleParam.getRuleParam();
          RuleParam matchingRPInDb = rulesDao.getRuleParam(matchingRuleInDb, unMarshalledRP.getKey());
          if (matchingRPInDb == null) {
            LoggerFactory.getLogger(getClass()).error("Unable to find active rule parameter " + unMarshalledRP.getKey());
            irp.remove();
            continue;
          }
          activeRuleParam.setActiveRule(activeRule);
          activeRuleParam.setRuleParam(matchingRPInDb);
        }
      }
    }
  }

  private Converter getAlertsConverter() {
    return new Converter() {

      public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Alert alert = (Alert) source;
        writeNode(writer, OPERATOR, alert.getOperator());
        writeNode(writer, VALUE_ERROR, alert.getValueError());
        writeNode(writer, VALUE_WARNING, alert.getValueWarning());
        if (alert.getPeriod() != null) {
          writeNode(writer, PERIOD, Integer.toString(alert.getPeriod()));
        }
        writeNode(writer, METRIC_KEY, alert.getMetric().getKey());
      }

      public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> values = readNode(reader);
        Alert alert = new Alert(null, new Metric(values.get(METRIC_KEY)), values.get(OPERATOR), values.get(VALUE_ERROR),
          values.get(VALUE_WARNING));
        String periodText = values.get(PERIOD);
        if (StringUtils.isNotEmpty(periodText)) {
          alert.setPeriod(Integer.parseInt(periodText));
        }
        return alert;
      }

      public boolean canConvert(Class type) {
        return type.equals(Alert.class);
      }
    };
  }

  private Converter getActiveRuleConverter() {
    return new Converter() {

      public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        ActiveRule rule = (ActiveRule) source;
        writeNode(writer, KEY, rule.getRule().getKey());
        writeNode(writer, PLUGIN, rule.getRule().getRepositoryKey());
        writeNode(writer, LEVEL, rule.getSeverity().name());
        writeNode(writer, INHERITANCE, rule.getInheritance());

        if (!rule.getActiveRuleParams().isEmpty()) {
          writer.startNode(PARAMS);
          for (ActiveRuleParam activeRuleParam : rule.getActiveRuleParams()) {
            writer.startNode("param");
            writeNode(writer, KEY, activeRuleParam.getRuleParam().getKey());
            writeNode(writer, VALUE, activeRuleParam.getValue());
            writer.endNode();
          }
          writer.endNode();
        }
      }

      public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> valuesRule = new HashMap<String, String>();
        List<ActiveRuleParam> params = new ArrayList<ActiveRuleParam>();
        while (reader.hasMoreChildren()) {
          reader.moveDown();
          valuesRule.put(reader.getNodeName(), reader.getValue());
          if (PARAMS.equals(reader.getNodeName())) {
            while (reader.hasMoreChildren()) {
              reader.moveDown();
              Map<String, String> valuesParam = readNode(reader);
              ActiveRuleParam activeRuleParam = new ActiveRuleParam(null, new RuleParam(null, valuesParam.get(KEY), null, null),
                valuesParam.get(VALUE));
              params.add(activeRuleParam);
              reader.moveUp();
            }
          }
          reader.moveUp();
        }

        ActiveRule activeRule = new ActiveRule(null, new Rule(valuesRule.get(PLUGIN), valuesRule.get(KEY)), RulePriority
          .valueOf(valuesRule.get(LEVEL)));
        activeRule.setActiveRuleParams(params);
        if (valuesRule.containsKey(INHERITANCE)) {
          activeRule.setInheritance(valuesRule.get(INHERITANCE));
        }
        return activeRule;
      }

      public boolean canConvert(Class type) {
        return type.equals(ActiveRule.class);
      }
    };
  }

  private void writeNode(HierarchicalStreamWriter writer, String name, String value) {
    if (value != null) {
      writer.startNode(name);
      writer.setValue(value);
      writer.endNode();
    }
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
