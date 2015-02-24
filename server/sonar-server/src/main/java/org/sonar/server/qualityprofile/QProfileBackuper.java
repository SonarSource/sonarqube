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
package org.sonar.server.qualityprofile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.search.IndexClient;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import java.io.Reader;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QProfileBackuper implements ServerComponent {

  private final QProfileReset reset;
  private final DbClient db;
  private final IndexClient index;

  public QProfileBackuper(QProfileReset reset, DbClient db, IndexClient index) {
    this.reset = reset;
    this.db = db;
    this.index = index;
  }

  void backup(String key, Writer writer) {
    QualityProfileDto profile;
    DbSession dbSession = db.openSession(false);
    try {
      profile = db.qualityProfileDao().getNonNullByKey(dbSession, key);
    } finally {
      dbSession.close();
    }
    Iterator<ActiveRule> activeRules = index.get(ActiveRuleIndex.class).findByProfile(profile.getKey());
    writeXml(writer, profile, activeRules);
  }

  private void writeXml(Writer writer, QualityProfileDto profile, Iterator<ActiveRule> activeRules) {
    XmlWriter xml = XmlWriter.of(writer).declaration();
    xml.begin("profile");
    xml.prop("name", profile.getName());
    xml.prop("language", profile.getLanguage());
    xml.begin("rules");
    while (activeRules.hasNext()) {
      ActiveRule activeRule = activeRules.next();
      xml.begin("rule");
      xml.prop("repositoryKey", activeRule.key().ruleKey().repository());
      xml.prop("key", activeRule.key().ruleKey().rule());
      xml.prop("priority", activeRule.severity());
      xml.begin("parameters");
      for (Map.Entry<String, String> param : activeRule.params().entrySet()) {
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
  BulkChangeResult restore(Reader reader, @Nullable QProfileName toProfileName) {
    try {
      String profileLang = null, profileName = null;
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
      return reset.reset(target, ruleActivations);
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Fail to restore Quality profile backup", e);
    }
  }

  private List<RuleActivation> parseRuleActivations(SMInputCursor rulesCursor) throws XMLStreamException {
    List<RuleActivation> activations = Lists.newArrayList();
    while (rulesCursor.getNext() != null) {
      SMInputCursor ruleCursor = rulesCursor.childElementCursor();
      String repositoryKey = null, key = null, severity = null;
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
      RuleActivation activation = new RuleActivation(ruleKey);
      activation.setSeverity(severity);
      activation.setParameters(parameters);
      activations.add(activation);
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

  private SMInputFactory initStax() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    return new SMInputFactory(xmlFactory);
  }
}
