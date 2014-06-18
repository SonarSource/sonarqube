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
import com.google.common.collect.Multimap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.activity.index.ActivityIndex;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.FacetValue;
import org.sonar.server.search.IndexClient;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QProfileService implements ServerComponent {

  private final DbClient db;
  private final IndexClient index;
  private final RuleActivator ruleActivator;
  private final QProfileFactory factory;
  private final QProfileBackuper backuper;
  private final QProfileCopier copier;
  private final QProfileReset reset;

  public QProfileService(DbClient db, IndexClient index, RuleActivator ruleActivator, QProfileFactory factory, QProfileBackuper backuper,
                         QProfileCopier copier, QProfileReset reset) {
    this.db = db;
    this.index = index;
    this.ruleActivator = ruleActivator;
    this.factory = factory;
    this.backuper = backuper;
    this.copier = copier;
    this.reset = reset;
  }

  public QualityProfileDto create(QProfileName name) {
    verifyAdminPermission();
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = factory.create(dbSession, name);
      dbSession.commit();
      return profile;
    } finally {
      dbSession.close();
    }
  }

  /**
   * Returns all Quality profiles as DTOs. This is a temporary solution as long as
   * profiles are not indexed and declared as a business object
   */
  public List<QualityProfileDto> findAll() {
    DbSession dbSession = db.openSession(false);
    try {
      return db.qualityProfileDao().findAll(dbSession);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public QualityProfileDto getByKey(String key) {
    DbSession dbSession = db.openSession(false);
    try {
      return db.qualityProfileDao().getByKey(dbSession, key);
    } finally {
      dbSession.close();
    }
  }

  @CheckForNull
  public ActiveRule getActiveRule(ActiveRuleKey key) {
    return index.get(ActiveRuleIndex.class).getByKey(key);
  }

  public List<ActiveRule> findActiveRulesByRule(RuleKey key) {
    return index.get(ActiveRuleIndex.class).findByRule(key);
  }

  public List<ActiveRule> findActiveRulesByProfile(String key) {
    return index.get(ActiveRuleIndex.class).findByProfile(key);
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

  @CheckForNull
  public String getDefault(String language) {
    QualityProfileDto profile = factory.getDefault(language);
    return profile != null ? profile.getKey() : null;
  }

  private void verifyAdminPermission() {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  public long countActiveRulesByProfile(String key) {
    return index.get(ActiveRuleIndex.class).countByQualityProfileKey(key);
  }

  public Map<String, Long> countAllActiveRules() {
    Map<String, Long> counts = new HashMap<String, Long>();
    for (Map.Entry<String, Long> entry : index.get(ActiveRuleIndex.class).countAllByQualityProfileKey().entrySet()) {
      counts.put(entry.getKey(), entry.getValue());
    }
    return counts;
  }

  public Multimap<String, FacetValue> getStatsByProfile(String key) {
    return index.get(ActiveRuleIndex.class).getStatsByProfileKey(key);
  }

  public Map<String, Multimap<String, FacetValue>> getAllProfileStats() {
    List<String> keys = Lists.newArrayList();
    for (QualityProfileDto profile : this.findAll()) {
      keys.add(profile.getKey());
    }
    return index.get(ActiveRuleIndex.class).getStatsByProfileKeys(keys);
  }

  public long countDeprecatedActiveRulesByProfile(String key) {
    return index.get(RuleIndex.class).search(
      new RuleQuery()
        .setQProfileKey(key)
        .setActivation(true)
        .setStatuses(Lists.newArrayList(RuleStatus.DEPRECATED)),
      new QueryOptions().setLimit(0)).getTotal();
  }

  public List<QProfileActivity> findActivities(QProfileActivityQuery query, QueryOptions options) {
    List<QProfileActivity> results = Lists.newArrayList();

    OrFilterBuilder activityFilter = FilterBuilders.orFilter();
    for (String profileKey : query.getQprofileKeys()) {
      activityFilter.add(FilterBuilders.termFilter("details.profileKey", profileKey));
    }

    SearchResponse response = index.get(ActivityIndex.class).search(query, options, activityFilter);
    for (SearchHit hit : response.getHits().getHits()) {
      QProfileActivity profileActivity = new QProfileActivity(hit.getSource());
      results.add(profileActivity);
    }
    return results;
  }
}
