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

import org.apache.ibatis.session.SqlSession;
import org.sonar.api.ServerComponent;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.utils.ValidationMessages;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.preview.PreviewCache;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.rule.RuleRegistry;
import org.sonar.server.user.UserSession;

import java.io.StringReader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class QProfileBackup implements ServerComponent {

  private final DatabaseSessionFactory sessionFactory;
  private final XMLProfileParser xmlProfileParser;

  private final MyBatis myBatis;
  private final QProfileOperations qProfileOperations;
  private final QProfileLookup qProfileLookup;
  private final RuleRegistry ruleRegistry;
  private final PreviewCache dryRunCache;

  public QProfileBackup(DatabaseSessionFactory sessionFactory, XMLProfileParser xmlProfileParser, MyBatis myBatis,
                        QProfileOperations qProfileOperations, QProfileLookup qProfileLookup, RuleRegistry ruleRegistry, PreviewCache dryRunCache) {

    this.sessionFactory = sessionFactory;
    this.xmlProfileParser = xmlProfileParser;
    this.myBatis = myBatis;
    this.qProfileOperations = qProfileOperations;
    this.qProfileLookup = qProfileLookup;
    this.ruleRegistry = ruleRegistry;
    this.dryRunCache = dryRunCache;
  }

  public QProfileResult restore(String xmlBackup, boolean deleteExisting, UserSession userSession) {
    checkPermission(userSession);

    QProfileResult result = new QProfileResult();
    ValidationMessages messages = ValidationMessages.create();
    RulesProfile importedProfile = xmlProfileParser.parse(new StringReader(xmlBackup), messages);
    processValidationMessages(messages, result);
    if (importedProfile != null) {
      DatabaseSession hibernateSession = sessionFactory.getSession();
      checkProfileDoesNotExists(importedProfile, deleteExisting, hibernateSession);
      hibernateSession.saveWithoutFlush(importedProfile);
      hibernateSession.commit();

      SqlSession session = myBatis.openSession();
      try {
        QProfile profile = qProfileLookup.profile(importedProfile.getId());
        ruleRegistry.bulkIndexProfile(profile.id(), session);
        result.setProfile(profile);
      } finally {
        MyBatis.closeQuietly(session);
      }
      dryRunCache.reportGlobalModification();
    }
    return result;
  }

  private void checkProfileDoesNotExists(RulesProfile importedProfile, boolean deleteExisting, DatabaseSession hibernateSession) {
    RulesProfile existingProfile = hibernateSession.getSingleResult(RulesProfile.class, "name", importedProfile.getName(), "language", importedProfile.getLanguage());
    if (existingProfile != null && !deleteExisting) {
      throw BadRequestException.of("The profile " + existingProfile + " already exists. Please delete it before restoring.");
    }
    if (existingProfile != null) {
      // TODO use QProfileOperations to do that (For ES unindexing)
      hibernateSession.removeWithoutFlush(existingProfile);
      hibernateSession.commit();
    }
  }

  private void processValidationMessages(ValidationMessages messages, QProfileResult result) {
    if (!messages.getErrors().isEmpty()) {
      List<BadRequestException.Message> errors = newArrayList();
      for (String error : messages.getErrors()) {
        errors.add(BadRequestException.Message.of(error));
      }
      throw BadRequestException.of("Fail to restore profile", errors);
    }
    result.setWarnings(messages.getWarnings());
    result.setInfos(messages.getInfos());
  }

  private void checkPermission(UserSession userSession) {
    userSession.checkLoggedIn();
    userSession.checkGlobalPermission(GlobalPermissions.QUALITY_PROFILE_ADMIN);
  }

}
