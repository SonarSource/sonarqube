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

import com.google.common.collect.Multimap;
import org.sonar.api.ServerComponent;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.qualityprofile.db.QualityProfileKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.IndexClient;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

public class QProfileService implements ServerComponent {

  private final DbClient db;
  private final IndexClient index;
  private final RuleActivator ruleActivator;
  private final QProfileBackuper backuper;
  private final QProfileCopier copier;
  private final QProfileReset reset;

  public QProfileService(DbClient db, IndexClient index, RuleActivator ruleActivator, QProfileBackuper backuper,
                         QProfileCopier copier, QProfileReset reset) {
    this.db = db;
    this.index = index;
    this.ruleActivator = ruleActivator;
    this.backuper = backuper;
    this.copier = copier;
    this.reset = reset;
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
  public ActiveRule getActiveRule(ActiveRuleKey key) {
    return index.get(ActiveRuleIndex.class).getByKey(key);
  }

  public List<ActiveRule> findActiveRulesByRule(RuleKey key) {
    return index.get(ActiveRuleIndex.class).findByRule(key);
  }

  public List<ActiveRule> findActiveRulesByProfile(QualityProfileKey key) {
    return index.get(ActiveRuleIndex.class).findByProfile(key);
  }

  /**
   * Activate a rule on a Quality profile. Update configuration (severity/parameters) if the rule is already
   * activated.
   */
  public List<ActiveRuleChange> activate(RuleActivation activation) {
    verifyAdminPermission();
    return ruleActivator.activate(activation);
  }

  /**
   * Deactivate a rule on a Quality profile. Does nothing if the rule is not activated, but
   * fails if the rule or the profile does not exist.
   */
  public List<ActiveRuleChange> deactivate(ActiveRuleKey key) {
    verifyAdminPermission();
    return ruleActivator.deactivate(key);
  }


  public Multimap<String, String> bulkActivate(RuleQuery ruleQuery, QualityProfileKey profile, @Nullable String severity) {
    verifyAdminPermission();
    return ruleActivator.bulkActivate(ruleQuery, profile, severity);
  }

  public Multimap<String, String> bulkDeactivate(RuleQuery ruleQuery, QualityProfileKey profile) {
    verifyAdminPermission();
    return ruleActivator.bulkDeactivate(ruleQuery, profile);
  }

  public void backup(QualityProfileKey key, Writer writer) {
    // Allowed to non-admin users (see http://jira.codehaus.org/browse/SONAR-2039)
    backuper.backup(key, writer);
  }

  /**
   * @deprecated used only by Ruby on Rails. Use {@link #backup(org.sonar.core.qualityprofile.db.QualityProfileKey, java.io.Writer)}
   */
  @Deprecated
  public String backup(QualityProfileKey key) {
    StringWriter output = new StringWriter();
    backup(key, output);
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

  public void resetBuiltInProfilesForLanguage(String lang) {
    verifyAdminPermission();
    reset.resetLanguage(lang);
  }

  public Collection<String> builtInProfileNamesForLanguage(String lang) {
    return reset.builtInProfileNamesForLanguage(lang);
  }

  public void copy(QualityProfileKey from, QualityProfileKey to) {
    verifyAdminPermission();
    copier.copy(from, to);
  }

  public void delete(QualityProfileKey key) {
    verifyAdminPermission();
    // TODO
  }

  public void rename(QualityProfileKey key, String newName) {
    verifyAdminPermission();
    // TODO
  }

  //public void create(NewQualityProfile newProfile) {
  // TODO
  //verifyAdminPermission();
  //}

  /**
   * Set or unset parent profile.
   *
   * @param key       key of existing profile
   * @param parentKey key of parent profile to be inherited from. Or <code>null</code> to unset the parent.
   */
  public void setParent(QualityProfileKey key, @Nullable QualityProfileKey parentKey) {
    verifyAdminPermission();
    ruleActivator.setParent(key, parentKey);
  }

  /**
   * Set the given quality profile as default for the related language
   */
  public void setDefault(QualityProfileKey key) {
    verifyAdminPermission();
    DbSession dbSession = db.openSession(false);
    try {
      QualityProfileDto profile = db.qualityProfileDao().getNonNullByKey(dbSession, key);
      db.propertiesDao().setProperty(new PropertyDto()
        .setKey("sonar.profile." + profile.getLanguage())
        .setValue(profile.getName()));
      dbSession.commit();
    } finally {
      dbSession.close();
    }
  }

  private void verifyAdminPermission() {
    UserSession.get().checkLoggedIn();
    UserSession.get().checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }
}
