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
package org.sonar.api.profiles;

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.ValidationMessages;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO should be an interface
 * 
 * @since 2.3
 */
public final class XMLProfileParser implements ServerComponent {

  private final RuleFinder ruleFinder;
  private MetricFinder metricFinder;

  /**
   * For backward compatibility.
   * 
   * @deprecated since 2.5. Plugins shouldn't directly instantiate this class,
   *             because it should be retrieved as an IoC dependency.
   */
  @Deprecated
  public XMLProfileParser(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  /**
   * @deprecated since 2.5. Plugins shouldn't directly instantiate this class,
   *             because it should be retrieved as an IoC dependency.
   */
  @Deprecated
  public XMLProfileParser(RuleFinder ruleFinder, MetricFinder metricFinder) {
    this.ruleFinder = ruleFinder;
    this.metricFinder = metricFinder;
  }

  public RulesProfile parseResource(ClassLoader classloader, String xmlClassPath, ValidationMessages messages) {
    Reader reader = new InputStreamReader(classloader.getResourceAsStream(xmlClassPath), Charsets.UTF_8);
    try {
      return parse(reader, messages);

    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public RulesProfile parse(Reader reader, ValidationMessages messages) {
    RulesProfile profile = RulesProfile.create();
    SMInputFactory inputFactory = initStax();
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("rules", nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          processRules(rulesCursor, profile, messages);

        } else if (StringUtils.equals("alerts", nodeName)) {
          SMInputCursor alertsCursor = cursor.childElementCursor("alert");
          processAlerts(alertsCursor, profile, messages);

        } else if (StringUtils.equals("name", nodeName)) {
          profile.setName(StringUtils.trim(cursor.collectDescendantText(false)));

        } else if (StringUtils.equals("language", nodeName)) {
          profile.setLanguage(StringUtils.trim(cursor.collectDescendantText(false)));
        }
      }
    } catch (XMLStreamException e) {
      messages.addErrorText("XML is not valid: " + e.getMessage());
    }
    checkProfile(profile, messages);
    return profile;
  }

  private void checkProfile(RulesProfile profile, ValidationMessages messages) {
    if (StringUtils.isBlank(profile.getName())) {
      messages.addErrorText("The mandatory node <name> is missing.");
    }
    if (StringUtils.isBlank(profile.getLanguage())) {
      messages.addErrorText("The mandatory node <language> is missing.");
    }
  }

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  private void processRules(SMInputCursor rulesCursor, RulesProfile profile, ValidationMessages messages) throws XMLStreamException {
    Map<String, String> parameters = new HashMap<String, String>();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();

      String repositoryKey = null, key = null;
      RulePriority priority = null;
      parameters.clear();

      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();

        if (StringUtils.equals("repositoryKey", nodeName)) {
          repositoryKey = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("priority", nodeName)) {
          priority = RulePriority.valueOf(StringUtils.trim(ruleCursor.collectDescendantText(false)));

        } else if (StringUtils.equals("parameters", nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor("parameter");
          processParameters(propsCursor, parameters);
        }
      }

      Rule rule = ruleFinder.findByKey(repositoryKey, key);
      if (rule == null) {
        messages.addWarningText("Rule not found: [repository=" + repositoryKey + ", key=" + key + "]");

      } else {
        ActiveRule activeRule = profile.activateRule(rule, priority);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
          if (rule.getParam(entry.getKey()) == null) {
            messages.addWarningText("The parameter '" + entry.getKey() + "' does not exist in the rule: [repository=" + repositoryKey
              + ", key=" + key + "]");
          } else {
            activeRule.setParameter(entry.getKey(), entry.getValue());
          }
        }
      }
    }
  }

  private void processParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
    while (propsCursor.getNext() != null) {
      SMInputCursor propCursor = propsCursor.childElementCursor();
      String key = null;
      String value = null;
      while (propCursor.getNext() != null) {
        String nodeName = propCursor.getLocalName();
        if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(propCursor.collectDescendantText(false));

        } else if (StringUtils.equals("value", nodeName)) {
          value = StringUtils.trim(propCursor.collectDescendantText(false));
        }
      }
      if (key != null) {
        parameters.put(key, value);
      }
    }
  }

  private void processAlerts(SMInputCursor alertsCursor, RulesProfile profile, ValidationMessages messages) throws XMLStreamException {
    if (metricFinder == null) {
      // TODO remove when constructor without MetricFinder would be removed
      Logs.INFO.error("Unable to parse alerts, because MetricFinder not available.");
      return;
    }
    while (alertsCursor.getNext() != null) {
      SMInputCursor alertCursor = alertsCursor.childElementCursor();

      String metricKey = null, operator = "", valueError = "", valueWarning = "";
      Integer period = null;

      while (alertCursor.getNext() != null) {
        String nodeName = alertCursor.getLocalName();

        if (StringUtils.equals("metric", nodeName)) {
          metricKey = StringUtils.trim(alertCursor.collectDescendantText(false));

        } else if (StringUtils.equals("period", nodeName)) {
          String periodParameter = StringUtils.trim(alertCursor.collectDescendantText(false));
          if (StringUtils.isNotBlank(periodParameter)) {
            period = Integer.parseInt(periodParameter);
          }
        }else if (StringUtils.equals("operator", nodeName)) {
          operator = StringUtils.trim(alertCursor.collectDescendantText(false));

        } else if (StringUtils.equals("warning", nodeName)) {
          valueWarning = StringUtils.trim(alertCursor.collectDescendantText(false));

        } else if (StringUtils.equals("error", nodeName)) {
          valueError = StringUtils.trim(alertCursor.collectDescendantText(false));
        }
      }

      Metric metric = metricFinder.findByKey(metricKey);
      if (metric == null) {
        messages.addWarningText("Metric '" + metricKey + "' does not exist");
      } else {
        Alert alert = new Alert(profile, metric, operator, valueError, valueWarning, period);
        profile.getAlerts().add(alert);
      }
    }
  }

}
