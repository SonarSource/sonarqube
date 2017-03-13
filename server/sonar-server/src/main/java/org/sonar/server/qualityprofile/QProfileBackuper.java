/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;

@ServerSide
public class QProfileBackuper {

  private static final Joiner RULE_KEY_JOINER = Joiner.on(", ").skipNulls();

  private final QProfileReset reset;
  private final DbClient db;

  public QProfileBackuper(QProfileReset reset, DbClient db) {
    this.reset = reset;
    this.db = db;
  }

  public void backup(DbSession dbSession, QualityProfileDto profileDto, Writer writer) {
    List<ActiveRuleDto> activeRules = db.activeRuleDao().selectByProfileKey(dbSession, profileDto.getKey());
    activeRules.sort(BackupActiveRuleComparator.INSTANCE);
    writeXml(dbSession, writer, profileDto, activeRules.iterator());
  }

  private void writeXml(DbSession dbSession, Writer writer, QualityProfileDto profile, Iterator<ActiveRuleDto> activeRules) {
    XmlWriter xml = XmlWriter.of(writer).declaration();
    xml.begin("profile");
    xml.prop("name", profile.getName());
    xml.prop("language", profile.getLanguage());
    xml.begin("rules");
    while (activeRules.hasNext()) {
      ActiveRuleDto activeRule = activeRules.next();
      xml.begin("rule");
      xml.prop("repositoryKey", activeRule.getKey().ruleKey().repository());
      xml.prop("key", activeRule.getKey().ruleKey().rule());
      xml.prop("priority", activeRule.getSeverityString());
      xml.begin("parameters");
      for (ActiveRuleParamDto param : db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRule.getId())) {
        xml
          .begin("parameter")
          .prop("key", param.getKey())
          .prop("value", param.getValue())
          .end();
      }
      xml.end("parameters");
      xml.end("rule");
    }
    xml.end("rules").end("profile").close();
  }

  /**
   * @param reader     the XML backup
   * @param toProfileName the target profile. If <code>null</code>, then use the
   *                   lang and name declared in the backup
   */
  public BulkChangeResult restore(DbSession dbSession, Reader reader, @Nullable QProfileName toProfileName) {
    try {
      String profileLang = null;
      String profileName = null;
      List<RuleActivation> ruleActivations = Lists.newArrayList();
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      if (!rootC.getLocalName().equals("profile")) {
        throw new IllegalArgumentException("Backup XML is not valid. Root element must be <profile>.");
      }
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("name", nodeName)) {
          profileName = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals("language", nodeName)) {
          profileLang = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals("rules", nodeName)) {
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          ruleActivations = parseRuleActivations(rulesCursor);
        }
      }

      QProfileName target = (QProfileName) ObjectUtils.defaultIfNull(toProfileName, new QProfileName(profileLang, profileName));
      return reset.reset(dbSession, target, ruleActivations);
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Fail to restore Quality profile backup", e);
    }
  }

  private List<RuleActivation> parseRuleActivations(SMInputCursor rulesCursor) throws XMLStreamException {
    List<RuleActivation> activations = Lists.newArrayList();
    Set<RuleKey> activatedKeys = Sets.newHashSet();
    List<RuleKey> duplicatedKeys = Lists.newArrayList();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();
      String repositoryKey = null;
      String key = null;
      String severity = null;
      Map<String, String> parameters = Maps.newHashMap();
      while (ruleCursor.getNext() != null) {
        String nodeName = ruleCursor.getLocalName();
        if (StringUtils.equals("repositoryKey", nodeName)) {
          repositoryKey = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("key", nodeName)) {
          key = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("priority", nodeName)) {
          severity = StringUtils.trim(ruleCursor.collectDescendantText(false));

        } else if (StringUtils.equals("parameters", nodeName)) {
          SMInputCursor propsCursor = ruleCursor.childElementCursor("parameter");
          readParameters(propsCursor, parameters);
        }
      }
      RuleKey ruleKey = RuleKey.of(repositoryKey, key);
      if (activatedKeys.contains(ruleKey)) {
        duplicatedKeys.add(ruleKey);
      }
      activatedKeys.add(ruleKey);
      RuleActivation activation = new RuleActivation(ruleKey);
      activation.setSeverity(severity);
      activation.setParameters(parameters);
      activations.add(activation);
    }
    if (!duplicatedKeys.isEmpty()) {
      throw new IllegalArgumentException("The quality profile cannot be restored as it contains duplicates for the following rules: " +
        RULE_KEY_JOINER.join(duplicatedKeys));
    }
    return activations;
  }

  private void readParameters(SMInputCursor propsCursor, Map<String, String> parameters) throws XMLStreamException {
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
        .append(o1.getKey().ruleKey().repository(), o2.getKey().ruleKey().repository())
        .append(o1.getKey().ruleKey().rule(), o2.getKey().ruleKey().rule())
        .toComparison();
    }
  }
}
