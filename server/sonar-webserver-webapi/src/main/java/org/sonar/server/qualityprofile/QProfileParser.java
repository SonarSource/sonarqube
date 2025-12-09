/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualityprofile;

import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.db.qualityprofile.ExportRuleDto;
import org.sonar.db.qualityprofile.ExportRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;

import static org.apache.commons.lang3.Strings.CS;
import static org.sonar.server.qualityprofile.QProfileUtils.parseImpactsToMap;

@ServerSide
public class QProfileParser {
  private static final String ATTRIBUTE_PROFILE = "profile";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ATTRIBUTE_LANGUAGE = "language";
  private static final String ATTRIBUTE_RULES = "rules";
  private static final String ATTRIBUTE_RULE = "rule";
  private static final String ATTRIBUTE_REPOSITORY_KEY = "repositoryKey";
  private static final String ATTRIBUTE_KEY = "key";
  private static final String ATTRIBUTE_PRIORITY = "priority";
  private static final String ATTRIBUTE_IMPACTS = "impacts";
  private static final String ATTRIBUTE_IMPACT = "impact";
  private static final String ATTRIBUTE_SEVERITY = "severity";
  private static final String ATTRIBUTE_SOFTWARE_QUALITY = "softwareQuality";
  private static final String ATTRIBUTE_PRIORITIZED_RULE = "prioritizedRule";
  private static final String ATTRIBUTE_TEMPLATE_KEY = "templateKey";
  private static final String ATTRIBUTE_TYPE = "type";
  private static final String ATTRIBUTE_DESCRIPTION = "description";
  private static final String ATTRIBUTE_CLEAN_CODE_ATTRIBUTE = "cleanCodeAttribute";

  private static final String ATTRIBUTE_PARAMETERS = "parameters";
  private static final String ATTRIBUTE_PARAMETER = "parameter";
  private static final String ATTRIBUTE_PARAMETER_KEY = "key";
  private static final String ATTRIBUTE_PARAMETER_VALUE = "value";

  public void writeXml(Writer writer, QProfileDto profile, Iterator<ExportRuleDto> rulesToExport) {
    XmlWriter xml = XmlWriter.of(writer).declaration();
    xml.begin(ATTRIBUTE_PROFILE);
    xml.prop(ATTRIBUTE_NAME, profile.getName());
    xml.prop(ATTRIBUTE_LANGUAGE, profile.getLanguage());
    xml.begin(ATTRIBUTE_RULES);
    while (rulesToExport.hasNext()) {
      ExportRuleDto ruleToExport = rulesToExport.next();
      xml.begin(ATTRIBUTE_RULE);
      xml.prop(ATTRIBUTE_REPOSITORY_KEY, ruleToExport.getRuleKey().repository());
      xml.prop(ATTRIBUTE_KEY, ruleToExport.getRuleKey().rule());
      xml.prop(ATTRIBUTE_TYPE, ruleToExport.getRuleType().name());
      xml.prop(ATTRIBUTE_PRIORITY, ruleToExport.getSeverityString());
      if (StringUtils.isNotEmpty(ruleToExport.getImpacts())) {
        xml.begin(ATTRIBUTE_IMPACTS);
        parseImpactsToMap(ruleToExport.getImpacts()).forEach((quality, severity) -> {
          xml.begin(ATTRIBUTE_IMPACT);
          xml.prop(ATTRIBUTE_SOFTWARE_QUALITY, quality.name());
          xml.prop(ATTRIBUTE_SEVERITY, severity.name());
          xml.end();
        });
        xml.end();
      }
      if (Boolean.TRUE.equals(ruleToExport.getPrioritizedRule())) {
        xml.prop(ATTRIBUTE_PRIORITIZED_RULE, ruleToExport.getPrioritizedRule());
      }

      if (ruleToExport.isCustomRule()) {
        xml.prop(ATTRIBUTE_NAME, ruleToExport.getName());
        xml.prop(ATTRIBUTE_TEMPLATE_KEY, ruleToExport.getTemplateRuleKey().rule());
        xml.prop(ATTRIBUTE_DESCRIPTION, ruleToExport.getDescriptionOrThrow());
        xml.prop(ATTRIBUTE_CLEAN_CODE_ATTRIBUTE, ruleToExport.getCleanCodeAttribute());
      }

      xml.begin(ATTRIBUTE_PARAMETERS);
      for (ExportRuleParamDto param : ruleToExport.getParams()) {
        xml
          .begin(ATTRIBUTE_PARAMETER)
          .prop(ATTRIBUTE_PARAMETER_KEY, param.getKey())
          .prop(ATTRIBUTE_PARAMETER_VALUE, param.getValue())
          .end();
      }
      xml.end(ATTRIBUTE_PARAMETERS);
      xml.end(ATTRIBUTE_RULE);
    }
    xml.end(ATTRIBUTE_RULES).end(ATTRIBUTE_PROFILE).close();
  }

  public ImportedQProfile readXml(Reader reader) {
    List<ImportedRule> rules = new ArrayList<>();
    String profileName = null;
    String profileLang = null;
    try {
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      if (!ATTRIBUTE_PROFILE.equals(rootC.getLocalName())) {
        throw new IllegalArgumentException("Backup XML is not valid. Root element must be <profile>.");
      }
      SMInputCursor cursor = rootC.childElementCursor();

      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (CS.equals(ATTRIBUTE_NAME, nodeName)) {
          profileName = StringUtils.trim(cursor.collectDescendantText(false));
        } else if (CS.equals(ATTRIBUTE_LANGUAGE, nodeName)) {
          profileLang = StringUtils.trim(cursor.collectDescendantText(false));
        } else if (CS.equals(ATTRIBUTE_RULES, nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          rules = parseRuleActivations(rulesCursor);
        }
      }
    } catch (XMLStreamException e) {
      throw new IllegalArgumentException("Fail to restore Quality profile backup, XML document is not well formed", e);
    }
    return new ImportedQProfile(profileName, profileLang, rules);
  }

  private static SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }

  private static List<ImportedRule> parseRuleActivations(SMInputCursor rulesCursor) throws XMLStreamException {
    List<ImportedRule> activations = new ArrayList<>();
    Set<RuleKey> activatedKeys = new HashSet<>();
    List<RuleKey> duplicatedKeys = new ArrayList<>();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();
      Map<String, String> parameters = new HashMap<>();
      ImportedRule rule = new ImportedRule();
      readRule(ruleCursor, parameters, rule);

      var ruleKey = rule.getRuleKey();
      if (activatedKeys.contains(ruleKey)) {
        duplicatedKeys.add(ruleKey);
      }
      activatedKeys.add(ruleKey);
      activations.add(rule);
    }
    if (!duplicatedKeys.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains duplicates for the following rules: " +
        duplicatedKeys.stream().map(RuleKey::toString).filter(Objects::nonNull).collect(Collectors.joining(", ")));
    }
    return activations;
  }

  private static void readRule(SMInputCursor ruleCursor, Map<String, String> parameters, ImportedRule rule) throws XMLStreamException {
    while (ruleCursor.getNext() != null) {
      String nodeName = ruleCursor.getLocalName();
      if (CS.equals(ATTRIBUTE_REPOSITORY_KEY, nodeName)) {
        rule.setRepository(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_KEY, nodeName)) {
        rule.setKey(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_TEMPLATE_KEY, nodeName)) {
        rule.setTemplate(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_NAME, nodeName)) {
        rule.setName(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_TYPE, nodeName)) {
        rule.setType(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_DESCRIPTION, nodeName)) {
        rule.setDescription(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_CLEAN_CODE_ATTRIBUTE, nodeName)) {
        rule.setCleanCodeAttribute(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_PRIORITY, nodeName)) {
        rule.setSeverity(StringUtils.trim(ruleCursor.collectDescendantText(false)));
      } else if (CS.equals(ATTRIBUTE_IMPACTS, nodeName)) {
        SMInputCursor impactsCursor = ruleCursor.childElementCursor(ATTRIBUTE_IMPACT);
        Map<SoftwareQuality, Severity> impacts = new EnumMap<>(SoftwareQuality.class);
        readImpacts(impactsCursor, impacts);
        rule.setImpacts(impacts);
      } else if (CS.equals(ATTRIBUTE_PRIORITIZED_RULE, nodeName)) {
        rule.setPrioritizedRule(Boolean.valueOf(StringUtils.trim(ruleCursor.collectDescendantText(false))));
      } else if (CS.equals(ATTRIBUTE_PARAMETERS, nodeName)) {
        SMInputCursor propsCursor = ruleCursor.childElementCursor(ATTRIBUTE_PARAMETER);
        readParameters(propsCursor, parameters);
        rule.setParameters(parameters);
      }
    }
  }

  private static void readParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
    while (propsCursor.getNext() != null) {
      SMInputCursor propCursor = propsCursor.childElementCursor();
      String key = null;
      String value = null;
      while (propCursor.getNext() != null) {
        String nodeName = propCursor.getLocalName();
        if (CS.equals(ATTRIBUTE_PARAMETER_KEY, nodeName)) {
          key = StringUtils.trim(propCursor.collectDescendantText(false));
        } else if (CS.equals(ATTRIBUTE_PARAMETER_VALUE, nodeName)) {
          value = StringUtils.trim(propCursor.collectDescendantText(false));
        }
      }
      if (key != null) {
        parameters.put(key, value);
      }
    }
  }

  private static void readImpacts(SMInputCursor impactsCursor, Map<SoftwareQuality, Severity> impacts) throws XMLStreamException {
    while (impactsCursor.getNext() != null) {
      SMInputCursor impactCursor = impactsCursor.childElementCursor();
      SoftwareQuality softwareQuality = null;
      Severity severity = null;
      while (impactCursor.getNext() != null) {
        String nodeName = impactCursor.getLocalName();
        if (CS.equals(ATTRIBUTE_SOFTWARE_QUALITY, nodeName)) {
          softwareQuality = SoftwareQuality.valueOf(StringUtils.trim(impactCursor.collectDescendantText(false)));
        } else if (CS.equals(ATTRIBUTE_SEVERITY, nodeName)) {
          severity = Severity.valueOf(StringUtils.trim(impactCursor.collectDescendantText(false)));
        }
      }
      if (softwareQuality != null && severity != null) {
        impacts.put(softwareQuality, severity);
      }
    }
  }
}
