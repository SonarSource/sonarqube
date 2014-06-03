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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.text.XmlWriter;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.search.IndexClient;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QProfileBackuper implements ServerComponent {

  private final RuleActivator activator;
  private final DbClient db;
  private final IndexClient index;

  public QProfileBackuper(RuleActivator activator, DbClient db, IndexClient index) {
    this.activator = activator;
    this.db = db;
    this.index = index;
  }

  public void backup(QualityProfileKey key, Writer writer) {
    // TODO check permission
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = db.qualityProfileDao().getByKey(key, dbSession);
      if (profile == null) {
        throw new IllegalArgumentException("Quality profile does not exist: " + key);
      }
      List<ActiveRule> activeRules = index.get(ActiveRuleIndex.class).findByProfile(key);
      writeXml(writer, profile, activeRules);

    } finally {
      dbSession.close();
    }
  }

  private void writeXml(Writer writer, QualityProfileDto profile, List<ActiveRule> activeRules) {
    XmlWriter xml = XmlWriter.of(writer).declaration();
    xml.begin("profile");
    xml.prop("name", profile.getName());
    xml.prop("language", profile.getLanguage());
    xml.begin("rules");
    for (ActiveRule activeRule : activeRules) {
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
    xml.end("rules");
    xml.end("profile").close();
  }

  public void restore(Reader reader) {
    try {
      String profileLang = null, profileName = null;
      SMInputFactory inputFactory = initStax();
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <profile>
      SMInputCursor cursor = rootC.childElementCursor();
      while (cursor.getNext() != null) {
        String nodeName = cursor.getLocalName();
        if (StringUtils.equals("name", nodeName)) {
          profileName = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals("language", nodeName)) {
          profileLang = StringUtils.trim(cursor.collectDescendantText(false));

        } else if (StringUtils.equals("rules", nodeName)) {
          QualityProfileKey profileKey = QualityProfileKey.of(profileName, profileLang);
          SMInputCursor rulesCursor = cursor.childElementCursor("rule");
          doRestore(rulesCursor, profileKey);
        }
      }
    } catch (XMLStreamException e) {
      throw new IllegalStateException("Fail to restore Quality profile backup", e);
    }
  }

  private void doRestore(SMInputCursor rulesCursor, QualityProfileKey profileKey) throws XMLStreamException {
    Set<RuleKey> rulesToDeactivate = Sets.newHashSet();
    DbSession dbSession = db.openSession(false);
    try {
      // find or create profile
      if (db.qualityProfileDao().getByKey(profileKey, dbSession) == null) {
        // create new profile
        db.qualityProfileDao().insert(dbSession, QualityProfileDto.createFor(profileKey));
      } else {
        // already exists. Keep reference to all the activated rules before backup restore
        for (ActiveRuleDto activeRuleDto : db.activeRuleDao().findByProfileKey(dbSession, profileKey)) {
          rulesToDeactivate.add(activeRuleDto.getKey().ruleKey());
        }
      }

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
        RuleActivation activation = new RuleActivation(ActiveRuleKey.of(profileKey, ruleKey));
        activation.setSeverity(severity);
        activation.setParameters(parameters);
        activator.activate(dbSession, activation);
        rulesToDeactivate.remove(ruleKey);
      }
      for (RuleKey ruleKey : rulesToDeactivate) {
        // Will fail to deactivate a rule which is activated in a parent profile
        activator.deactivate(ActiveRuleKey.of(profileKey, ruleKey), dbSession);
      }
      dbSession.commit();

    } finally {
      dbSession.close();
    }
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

  public void resetForLang(String language) {
    // TODO
  }
}
