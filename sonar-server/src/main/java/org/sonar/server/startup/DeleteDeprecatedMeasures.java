/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.Logs;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.persistence.Query;
import java.util.List;

/**
 * This purge script can not be executed with ActiveRecord. It fails with an out of memory error.
 *
 * @since 2.5 this component could be removed after 4 or 5 releases.
 */
public final class DeleteDeprecatedMeasures {

  private ServerUpgradeStatus status;
  private DatabaseSessionFactory sessionFactory;
  private static final int MAX_IN_ELEMENTS = 500;

  public DeleteDeprecatedMeasures(DatabaseSessionFactory sessionFactory, ServerUpgradeStatus status) {
    this.sessionFactory = sessionFactory;
    this.status = status;
  }

  public void start() {
    if (mustDoPurge()) {
      doPurge();
    }
  }

  boolean mustDoPurge() {
    return status.isUpgraded() && status.getInitialDbVersion() <= 162;
  }

  void doPurge() {
    Logs.INFO.info("Deleting measures with deprecated ISO category");
    deleteRows("SELECT m.id FROM " + MeasureModel.class.getSimpleName() + " m WHERE m.ruleId IS NULL AND m.rulesCategoryId IS NOT NULL");

    Logs.INFO.info("Deleting measures with deprecated priority");
    deleteRows("SELECT m.id FROM " + MeasureModel.class.getSimpleName() + " m WHERE m.ruleId IS NULL AND m.rulePriority IS NOT NULL");
  }

  private void deleteRows(String hql) {
    DatabaseSession session = sessionFactory.getSession();
    List ids = session.getEntityManager().createQuery(hql).getResultList();
    int index = 0;
    while (index < ids.size()) {
      List paginedSids = ids.subList(index, Math.min(ids.size(), index + MAX_IN_ELEMENTS));
      Query query = session.createQuery("DELETE FROM " + MeasureModel.class.getSimpleName() + " WHERE id IN (:ids)");
      query.setParameter("ids", paginedSids);
      query.executeUpdate();
      index += MAX_IN_ELEMENTS;
      session.commit();
    }
  }
}
