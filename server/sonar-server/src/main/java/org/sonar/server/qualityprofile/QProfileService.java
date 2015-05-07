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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.ServerSide;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.user.UserDto;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.Result;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@ServerSide
public class QProfileService {

  private final DbClient db;
  private final ActivityIndex activityIndex;
  private final RuleActivator ruleActivator;
  private final QProfileFactory factory;
  private final QProfileBackuper backuper;
  private final QProfileCopier copier;
  private final QProfileReset reset;
  private final QProfileExporters exporters;

  public QProfileService(DbClient db, ActivityIndex activityIndex, RuleActivator ruleActivator, QProfileFactory factory,
    QProfileBackuper backuper, QProfileCopier copier, QProfileReset reset, QProfileExporters exporters) {
    this.db = db;
    this.activityIndex = activityIndex;
    this.ruleActivator = ruleActivator;
    this.factory = factory;
    this.backuper = backuper;
    this.copier = copier;
    this.reset = reset;
    this.exporters = exporters;
  }

  public QProfileResult create(QProfileName name, @Nullable Map<String, String> xmlQProfilesByPlugin) {
    verifyAdminPermission();
    DbSession dbSession = db.openSession(false);
    try {
      QProfileResult result = new QProfileResult();
      QualityProfileDto profile = factory.create(dbSession, name);
      result.setProfile(profile);
      if (xmlQProfilesByPlugin != null) {
        for (Map.Entry<String, String> entry : xmlQProfilesByPlugin.entrySet()) {
          result.add(exporters.importXml(profile, entry.getKey(), entry.getValue(), dbSession));
        }
      }
      dbSession.commit();
      return result;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(String profileKey, RuleActivation activation) {
    verifyAdminPermission();
    DbSession dbSession = db.openSession(false);
    try {
      List<ActiveRuleChange> changes = ruleActivator.activate(dbSession, activation, profileKey);
      dbSession.commit();
      return changes;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails if the rule or the profile does not exist.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    verifyAdminPermission();
    return ruleActivator.deactivate(key);
  }

  public BulkChangeResult bulkActivate(RuleQuery ruleQuery, String profile, @Nullable String severity) {
    verifyAdminPermission();
    return ruleActivator.bulkActivate(ruleQuery, profile, severity);
  }

  public BulkChangeResult bulkDeactivate(RuleQuery ruleQuery, String profile) {
    verifyAdminPermission();
    return ruleActivator.bulkDeactivate(ruleQuery, profile);
  }

  public void backup(String profileKey, Writer writer) {
    // Allowed to non-admin users (see http://jira.codehaus.org/browse/SONAR-2039)
    backuper.backup(profileKey, writer);
  }

  /**
   * @deprecated used only by Ruby on Rails. Use {@link #backup(String, java.io.Writer)}
   */
  @Deprecated
  public String backup(String profileKey) {
    StringWriter output = new StringWriter();
    backup(profileKey, output);
    return output.toString();
  }

  public void restore(Reader backup) {
    verifyAdminPermission();
    backuper.restore(backup, null);
  }

  /**
   * @deprecated used only by Ruby on Rails. Use {@link #restore(java.io.Reader)}
   */
  @Deprecated
  public void restore(String backup) {
    restore(new StringReader(backup));
  }

  public void restoreBuiltInProfilesForLanguage(String lang) {
    verifyAdminPermission();
    reset.resetLanguage(lang);
  }

  /**
   * Currently used by Ruby on Rails
   */
  public Collection<String> builtInProfileNamesForLanguage(String lang) {
    return reset.builtInProfileNamesForLanguage(lang);
  }

  public void copyToName(String fromKey, String toName) {
    verifyAdminPermission();
    copier.copyToName(fromKey, toName);
  }

  public void delete(String key) {
    verifyAdminPermission();
    factory.delete(key);
  }

  public void rename(String key, String newName) {
    verifyAdminPermission();
    factory.rename(key, newName);
  }

  /**
   * Set or unset parent profile.
   *
   * @param key       key of existing profile
   * @param parentKey key of parent profile to be inherited from. Or <code>null</code> to unset the parent.
   */
  public void setParent(String key, @Nullable String parentKey) {
    verifyAdminPermission();
    ruleActivator.setParent(key, parentKey);
  }

  /**
   * Set the given quality profile as default for the related language
   */
  public void setDefault(String key) {
    verifyAdminPermission();
    factory.setDefault(key);
  }

  /**
   * Used in /api/profiles and in /profiles/export
   */
  @CheckForNull
  public QualityProfileDto getDefault(String language) {
    return factory.getDefault(language);
  }

  private void verifyAdminPermission() {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public Result<QProfileActivity> searchActivities(QProfileActivityQuery query, SearchOptions options) {
    DbSession session = db.openSession(false);
    try {
      SearchResponse response = activityIndex.doSearch(query, options);
      Result<QProfileActivity> result = new Result<QProfileActivity>(response);
      for (SearchHit hit : response.getHits().getHits()) {
        QProfileActivity profileActivity = new QProfileActivity(hit.getSource());
        RuleDto ruleDto = db.ruleDao().getNullableByKey(session, profileActivity.ruleKey());
        profileActivity.ruleName(ruleDto != null ? ruleDto.getName() : null);

        String login = profileActivity.getLogin();
        if (login != null) {
          UserDto user = db.userDao().selectActiveUserByLogin(session, login);
          profileActivity.authorName(user != null ? user.getName() : null);
        }
        result.getHits().add(profileActivity);
      }
      return result;
    } finally {
      session.close();
    }
  }
}
