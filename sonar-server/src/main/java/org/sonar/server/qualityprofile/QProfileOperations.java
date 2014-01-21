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

package org.sonar.server.qualityprofile;

import com.google.common.annotations.VisibleForTesting;
import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class QProfileOperations implements ServerComponent {

  public static final String PROFILE_PROPERTY_PREFIX = "sonar.profile.";

  private final MyBatis myBatis;
  private final QualityProfileDao dao;
  private final ActiveRuleDao activeRuleDao;
  private final PropertiesDao propertiesDao;
  private final QProfilePluginExporter exporter;
  private final PreviewCache dryRunCache;
  private final RuleRegistry ruleRegistry;
  private final QProfileLookup profileLookup;
  private final ProfilesManager profilesManager;

  public QProfileOperations(MyBatis myBatis, QualityProfileDao dao, ActiveRuleDao activeRuleDao, PropertiesDao propertiesDao,
                            QProfilePluginExporter exporter, PreviewCache dryRunCache, RuleRegistry ruleRegistry, QProfileLookup profileLookup, ProfilesManager profilesManager) {
    this.myBatis = myBatis;
    this.dao = dao;
    this.activeRuleDao = activeRuleDao;
    this.propertiesDao = propertiesDao;
    this.exporter = exporter;
    this.dryRunCache = dryRunCache;
    this.ruleRegistry = ruleRegistry;
    this.profileLookup = profileLookup;
    this.profilesManager = profilesManager;
  }

  public QProfileResult newProfile(String name, String language, Map<String, String> xmlProfilesByPlugin, UserSession userSession) {
    SqlSession session = myBatis.openSession();
    try {
      QProfile profile = newProfile(name, language, true, userSession, session);

      QProfileResult result = new QProfileResult();
      result.setProfile(profile);

      for (Map.Entry<String, String> entry : xmlProfilesByPlugin.entrySet()) {
        result.add(exporter.importXml(profile, entry.getKey(), entry.getValue(), session));
      }
      dryRunCache.reportGlobalModification(session);
      session.commit();
      return result;
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public QProfile newProfile(String name, String language, boolean failIfAlreadyExists, UserSession userSession, SqlSession session) {
    return newProfile(name, language, null, failIfAlreadyExists, userSession, session);
  }

  public QProfile newProfile(String name, String language, @Nullable String parent, boolean failIfAlreadyExists, UserSession userSession, SqlSession session) {
    checkPermission(userSession);
    if (failIfAlreadyExists) {
      checkNotAlreadyExists(name, language, session);
    }
    QualityProfileDto dto = new QualityProfileDto().setName(name).setLanguage(language).setParent(parent).setVersion(1).setUsed(false);
    dao.insert(dto, session);
    return QProfile.from(dto);
  }

  public void renameProfile(int profileId, String newName, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto profileDto = findNotNull(profileId, session);
      String oldName = profileDto.getName();

      QProfile profile = QProfile.from(profileDto);
      if (!oldName.equals(newName)) {
        checkNotAlreadyExists(newName, profile.language(), session);
      }
      profileDto.setName(newName);
      dao.update(profileDto, session);

      List<QProfile> children = profileLookup.children(profile, session);
      for (QProfile child : children) {
        dao.update(child.setParent(newName).toDto(), session);
      }
      propertiesDao.updateProperties(PROFILE_PROPERTY_PREFIX + profile.language(), oldName, newName, session);

      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void deleteProfile(int profileId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      deleteProfile(profileId, userSession, session);
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  private void deleteProfile(int profileId, UserSession userSession, SqlSession session) {
    checkPermission(userSession);
    QualityProfileDto profile = findNotNull(profileId, session);
    if (!profileLookup.isDeletable(QProfile.from(profile), session)) {
      throw new BadRequestException("This profile can not be deleted");
    } else {
      activeRuleDao.deleteParametersFromProfile(profile.getId(), session);
      activeRuleDao.deleteFromProfile(profile.getId(), session);
      dao.delete(profile.getId(), session);
      propertiesDao.deleteProjectProperties(PROFILE_PROPERTY_PREFIX + profile.getLanguage(), profile.getName(), session);
      ruleRegistry.deleteActiveRulesFromProfile(profile.getId());
      dryRunCache.reportGlobalModification(session);
    }
  }

  public void setDefaultProfile(int profileId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto qualityProfile = findNotNull(profileId, session);
      propertiesDao.setProperty(new PropertyDto().setKey(PROFILE_PROPERTY_PREFIX + qualityProfile.getLanguage()).setValue(qualityProfile.getName()));
      session.commit();
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void updateParentProfile(int profileId, @Nullable Integer parentId, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto profile = findNotNull(profileId, session);
      QualityProfileDto parentProfile = null;
      if (parentId != null) {
        parentProfile = findNotNull(parentId, session);
      }
      if (isCycle(profile, parentProfile, session)) {
        throw new BadRequestException("Please do not select a child profile as parent.");
      }
      String newParentName = parentProfile != null ? parentProfile.getName() : null;
      // Modification of inheritance has to be done before setting new parent name in order to be able to disable rules from old parent
      ProfilesManager.RuleInheritanceActions actions = profilesManager.profileParentChanged(profile.getId(), newParentName, userSession.name());
      profile.setParent(newParentName);
      dao.update(profile, session);
      session.commit();

      ruleRegistry.deleteActiveRules(actions.idsToDelete());
      ruleRegistry.bulkIndexActiveRuleIds(actions.idsToIndex(), session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  public void copyProfile(int profileId, String copyProfileName, UserSession userSession) {
    checkPermission(userSession);
    SqlSession session = myBatis.openSession();
    try {
      QualityProfileDto profileDto = findNotNull(profileId, session);
      checkNotAlreadyExists(copyProfileName, profileDto.getLanguage(), session);
      int copyProfileId = profilesManager.copyProfile(profileId, copyProfileName);
      session.commit();
      ruleRegistry.bulkIndexProfile(copyProfileId, session);
    } finally {
      MyBatis.closeQuietly(session);
    }
  }

  @VisibleForTesting
  boolean isCycle(QualityProfileDto childProfile, @Nullable QualityProfileDto parentProfile, SqlSession session) {
    QualityProfileDto currentParent = parentProfile;
    while (currentParent != null) {
      if (childProfile.getName().equals(currentParent.getName())) {
        return true;
      }
      currentParent = getParent(currentParent, session);
    }
    return false;
  }

  @CheckForNull
  private QualityProfileDto getParent(QualityProfileDto profile, SqlSession session) {
    if (profile.getParent() != null) {
      return dao.selectParent(profile.getId(), session);
    }
    return null;
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

  private QualityProfileDto findNotNull(int profileId, SqlSession session) {
    QualityProfileDto profile = dao.selectById(profileId, session);
    QProfileValidations.checkProfileIsNotNull(profile);
    return profile;
  }

  private void checkNotAlreadyExists(String name, String language, SqlSession session) {
    if (dao.selectByNameAndLanguage(name, language, session) != null) {
      throw BadRequestException.ofL10n("quality_profiles.already_exists");
    }
  }

}
