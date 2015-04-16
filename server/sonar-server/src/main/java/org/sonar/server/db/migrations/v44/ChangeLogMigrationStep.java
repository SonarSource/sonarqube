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
package org.sonar.server.db.migrations.v44;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.migration.v44.ChangeLog;
import org.sonar.core.persistence.migration.v44.Migration44Mapper;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.rule.SeverityUtil;
import org.sonar.server.activity.Activity;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.MigrationStep;
import org.sonar.server.qualityprofile.ActiveRuleChange;

/**
 * SONAR-5329
 * Transition ActiveRuleChanges to ActivityLog
 * <p/>
 * Used in the Active Record Migration 548.
 *
 * @since 4.4
 */
public class ChangeLogMigrationStep implements MigrationStep {

  private final ActivityDao dao;
  private final DbClient db;

  public ChangeLogMigrationStep(ActivityDao dao, DbClient db) {
    this.dao = dao;
    this.db = db;
  }

  @Override
  public void execute() {
    DbSession session = db.openSession(false);
    Migration44Mapper migrationMapper = session.getMapper(Migration44Mapper.class);
    try {
      executeUpsert(session, ActiveRuleChange.Type.ACTIVATED, migrationMapper.selectActiveRuleChange(true));
      executeUpsert(session, ActiveRuleChange.Type.UPDATED, migrationMapper.selectActiveRuleChange(null));
      executeUpsert(session, ActiveRuleChange.Type.DEACTIVATED, migrationMapper.selectActiveRuleChange(false));
      session.commit();
    } finally {
      session.close();
    }
  }

  private void executeUpsert(DbSession session, ActiveRuleChange.Type type, List<ChangeLog> changes) {

    Iterator<ChangeLog> changeLogIterator = changes.iterator();
    if (changeLogIterator.hasNext()) {
      // startCase
      ChangeLog change = changeLogIterator.next();
      int currentId = change.getId();
      Date currentTimeStamp = change.getCreatedAt();
      String currentAuthor = change.getUserLogin();
      ActiveRuleChange ruleChange = newActiveRuleChance(type, change);
      processRuleChange(ruleChange, change);

      while (changeLogIterator.hasNext()) {
        change = changeLogIterator.next();
        int id = change.getId();
        if (id != currentId) {
          saveActiveRuleChange(session, ruleChange, currentAuthor, currentTimeStamp);
          currentId = id;
          currentTimeStamp = change.getCreatedAt();
          currentAuthor = change.getUserLogin();
          ruleChange = newActiveRuleChance(type, change);
        }
        processRuleChange(ruleChange, change);
      }
      // save the last
      saveActiveRuleChange(session, ruleChange, currentAuthor, currentTimeStamp);
    }
  }

  private void saveActiveRuleChange(DbSession session, ActiveRuleChange ruleChange, String author, Date currentTimeStamp) {
    Activity activity = ruleChange.toActivity();
    ActivityDto dto = new ActivityDto();
    dto.setKey(Uuids.create());
    dto.setType(Activity.Type.QPROFILE.name());
    dto.setAction(activity.getAction());
    dto.setMessage(activity.getMessage());
    dto.setAuthor(author);
    dto.setData(KeyValueFormat.format(activity.getData()));
    dto.setCreatedAt(currentTimeStamp);
    dao.insert(session, dto);
  }

  private void processRuleChange(ActiveRuleChange ruleChange, ChangeLog change) {
    ruleChange.setSeverity(SeverityUtil.getSeverityFromOrdinal(change.getSeverity()));
    String paramName = change.getParamKey();
    String paramValue = change.getParamValue();
    if (StringUtils.isNotEmpty(paramName)) {
      ruleChange.setParameter(paramName, paramValue);
    }
  }

  private ActiveRuleChange newActiveRuleChance(ActiveRuleChange.Type type, ChangeLog change) {
    return ActiveRuleChange.createFor(type,
      ActiveRuleKey.of(change.getProfileKey(), RuleKey.of(change.getRepository(), change.getRuleKey())));
  }
}
