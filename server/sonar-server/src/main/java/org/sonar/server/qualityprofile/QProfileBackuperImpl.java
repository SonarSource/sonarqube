/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.OrgActiveRuleDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;

import static com.google.common.base.Preconditions.checkArgument;

@ServerSide
public class QProfileBackuperImpl implements QProfileBackuper {

  private static final Joiner RULE_KEY_JOINER = Joiner.on(", ").skipNulls();

  private static final String ATTRIBUTE_PROFILE = "profile";
  private static final String ATTRIBUTE_NAME = "name";
  private static final String ATTRIBUTE_LANGUAGE = "language";

  private static final String ATTRIBUTE_RULES = "rules";
  private static final String ATTRIBUTE_RULE = "rule";
  private static final String ATTRIBUTE_REPOSITORY_KEY = "repositoryKey";
  private static final String ATTRIBUTE_KEY = "key";
  private static final String ATTRIBUTE_PRIORITY = "priority";

  private static final String ATTRIBUTE_PARAMETERS = "parameters";
  private static final String ATTRIBUTE_PARAMETER = "parameter";
  private static final String ATTRIBUTE_PARAMETER_KEY = "key";
  private static final String ATTRIBUTE_PARAMETER_VALUE = "value";

  private final DbClient db;
  private final QProfileReset profileReset;
  private final QProfileFactory profileFactory;

  public QProfileBackuperImpl(DbClient db, QProfileReset profileReset, QProfileFactory profileFactory) {
    this.db = db;
    this.profileReset = profileReset;
    this.profileFactory = profileFactory;
  }

  @Override
  public void backup(DbSession dbSession, QProfileDto profile, Writer writer) {
    List<OrgActiveRuleDto> activeRules = db.activeRuleDao().selectByProfile(dbSession, profile);
    activeRules.sort(BackupActiveRuleComparator.INSTANCE);
    writeXml(dbSession, writer, profile, activeRules.iterator());
  }

  private void writeXml(DbSession dbSession, Writer writer, QProfileDto profile, Iterator<OrgActiveRuleDto> activeRules) {
    XmlWriter xml = XmlWriter.of(writer).declaration();
    xml.begin(ATTRIBUTE_PROFILE);
    xml.prop(ATTRIBUTE_NAME, profile.getName());
    xml.prop(ATTRIBUTE_LANGUAGE, profile.getLanguage());
    xml.begin(ATTRIBUTE_RULES);
    while (activeRules.hasNext()) {
      ActiveRuleDto activeRule = activeRules.next();
      xml.begin(ATTRIBUTE_RULE);
      xml.prop(ATTRIBUTE_REPOSITORY_KEY, activeRule.getRuleKey().repository());
      xml.prop(ATTRIBUTE_KEY, activeRule.getRuleKey().rule());
      xml.prop(ATTRIBUTE_PRIORITY, activeRule.getSeverityString());
      xml.begin(ATTRIBUTE_PARAMETERS);
      for (ActiveRuleParamDto param : db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRule.getId())) {
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

  @Override
  public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, OrganizationDto organization, @Nullable String overriddenProfileName) {
    return restore(dbSession, backup, nameInBackup -> {
      QProfileName targetName = nameInBackup;
      if (overriddenProfileName != null) {
        targetName = new QProfileName(nameInBackup.getLanguage(), overriddenProfileName);
      }
      return profileFactory.getOrCreateCustom(dbSession, organization, targetName);
    });
  }

  @Override
  public QProfileRestoreSummary restore(DbSession dbSession, Reader backup, QProfileDto profile) {
    return restore(dbSession, backup, nameInBackup -> {
      checkArgument(profile.getLanguage().equals(nameInBackup.getLanguage()),
        "Can't restore %s backup on %s profile with key [%s]. Languages are different.", nameInBackup.getLanguage(), profile.getLanguage(), profile.getKee());
      return profile;
    });
  }

  private QProfileRestoreSummary restore(DbSession dbSession, Reader backup, Function<QProfileName, QProfileDto> profileLoader) {
    try {
      String profileLang = null;
      String profileName = null;
      List<Rule> rules = Lists.newArrayList();
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(backup);
      rootC.advance(); // <profile>
      if (!"profile".equals(rootC.getLocalName())) {
        throw new IllegalArgumentException("Backup XML is not valid. Root element must be <profile>.");
      }
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals(ATTRIBUTE_NAME, nodeName)) {
          profileName = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals(ATTRIBUTE_LANGUAGE, nodeName)) {
          profileLang = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals(ATTRIBUTE_RULES, nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          rules = parseRuleActivations(rulesCursor);
        }
      }

      QProfileName targetName = new QProfileName(profileLang, profileName);
      QProfileDto targetProfile = profileLoader.apply(targetName);
      List<RuleActivation> ruleActivations = toRuleActivations(dbSession, rules);
      BulkChangeResult changes = profileReset.reset(dbSession, targetProfile, ruleActivations);
      return new QProfileRestoreSummary(targetProfile, changes);
    } catch (XMLStreamException e) {
      throw new IllegalArgumentException("Fail to restore Quality profile backup, XML document is not well formed", e);
    }
  }

  private List<RuleActivation> toRuleActivations(DbSession dbSession, List<Rule> rules) {
    List<RuleKey> ruleKeys = rules.stream()
      .map(r -> r.ruleKey)
      .collect(MoreCollectors.toList());
    Map<RuleKey, RuleDefinitionDto> ruleDefinitionsByKey = db.ruleDao().selectDefinitionByKeys(dbSession, ruleKeys)
      .stream()
      .collect(MoreCollectors.uniqueIndex(RuleDefinitionDto::getKey));

    List<RuleDefinitionDto> externalRules = ruleDefinitionsByKey.values().stream()
      .filter(RuleDefinitionDto::isExternal)
      .collect(Collectors.toList());

    if (!externalRules.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains rules from external rule engines: "
        + externalRules.stream().map(r -> r.getKey().toString()).collect(Collectors.joining(", ")));
    }

    return rules.stream()
      .map(r -> {
        RuleDefinitionDto ruleDefinition = ruleDefinitionsByKey.get(r.ruleKey);
        if (ruleDefinition == null) {
          return null;
        }
        return RuleActivation.create(ruleDefinition.getId(), r.severity, r.parameters);
      })
      .filter(Objects::nonNull)
      .collect(MoreCollectors.toList(rules.size()));
  }

  private static final class Rule {
    private final RuleKey ruleKey;
    private final String severity;
    private final Map<String, String> parameters;

    private Rule(RuleKey ruleKey, String severity, Map<String, String> parameters) {
      this.ruleKey = ruleKey;
      this.severity = severity;
      this.parameters = parameters;
    }
  }

  private static List<Rule> parseRuleActivations(SMInputCursor rulesCursor) throws XMLStreamException {
    List<Rule> activations = new ArrayList<>();
    Set<RuleKey> activatedKeys = new HashSet<>();
    List<RuleKey> duplicatedKeys = new ArrayList<>();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();
      String repositoryKey = null;
      String key = null;
      String severity = null;
      Map<String, String> parameters = new HashMap<>();
      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();
        if (StringUtils.equals(ATTRIBUTE_REPOSITORY_KEY, nodeName)) {
          repositoryKey = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals(ATTRIBUTE_KEY, nodeName)) {
          key = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals(ATTRIBUTE_PRIORITY, nodeName)) {
          severity = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals(ATTRIBUTE_PARAMETERS, nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor(ATTRIBUTE_PARAMETER);
          readParameters(propsCursor, parameters);
        }
      }
      RuleKey ruleKey = RuleKey.of(repositoryKey, key);
      if (activatedKeys.contains(ruleKey)) {
        duplicatedKeys.add(ruleKey);
      }
      activatedKeys.add(ruleKey);
      activations.add(new Rule(ruleKey, severity, parameters));
    }
    if (!duplicatedKeys.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains duplicates for the following rules: " +
        RULE_KEY_JOINER.join(duplicatedKeys));
    }
    return activations;
  }

  private static void readParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
    while (propsCursor.getNext() != null) {
      SMInputCursor propCursor = propsCursor.childElementCursor();
      String key = null;
      String value = null;
      while (propCursor.getNext() != null) {
        String nodeName = propCursor.getLocalName();
        if (StringUtils.equals(ATTRIBUTE_PARAMETER_KEY, nodeName)) {
          key = StringUtils.trim(propCursor.collectDescendantText(false));
        } else if (StringUtils.equals(ATTRIBUTE_PARAMETER_VALUE, nodeName)) {
          value = StringUtils.trim(propCursor.collectDescendantText(false));
        }
      }
      if (key != null) {
        parameters.put(key, value);
      }
    }
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

  private enum BackupActiveRuleComparator implements Comparator<ActiveRuleDto> {
    INSTANCE;

    @Override
    public int compare(ActiveRuleDto o1, ActiveRuleDto o2) {
      return new CompareToBuilder()
        .append(o1.getRuleKey().repository(), o2.getRuleKey().repository())
        .append(o1.getRuleKey().rule(), o2.getRuleKey().rule())
        .toComparison();
    }
  }
}
