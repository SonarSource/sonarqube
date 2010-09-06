/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.jpa.dao;

import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.AsyncMeasureSnapshot;
import org.sonar.api.database.model.MeasureModel;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

public class AsyncMeasuresDao extends BaseDao {

  public AsyncMeasuresDao(DatabaseSession session) {
    super(session);
  }

  public MeasureModel getAsyncMeasure(Long asyncMeasureId) {
    return getSession().getEntityManager().find(MeasureModel.class, asyncMeasureId);
  }

  public void deleteAsyncMeasure(MeasureModel asyncMeasure) {
    deleteAsyncMeasureSnapshots(asyncMeasure.getId());
    getSession().remove(asyncMeasure);
  }

  public Snapshot getPreviousSnapshot(Snapshot s) {
    try {
      return (Snapshot) getSession().createQuery(
          "SELECT s FROM Snapshot s " +
              "WHERE s.createdAt<:date " +
              "AND s.scope=:scope " +
              "AND s.resourceId=:resourceId " +
              "ORDER BY s.createdAt DESC")
          .setParameter("date", s.getCreatedAt())
          .setParameter("scope", s.getScope())
          .setParameter("resourceId", s.getResourceId())
          .setMaxResults(1)
          .getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public List<Snapshot> getNextSnapshotsUntilDate(MeasureModel measure, Date date) {
    Query query = getSession().createQuery(
        "SELECT s FROM Snapshot s " +
            "WHERE s.resourceId=:projectId " +
            "AND s.createdAt>=:beginDate " +
            (date != null ? "AND s.createdAt<:endDate " : "") +
            "AND s.scope=:scope " +
            "ORDER BY s.createdAt ASC ")
        .setParameter("projectId", measure.getProjectId())
        .setParameter("beginDate", measure.getMeasureDate())
        .setParameter("scope", ResourceModel.SCOPE_PROJECT);
    if (date != null) {
      query.setParameter("endDate", date);
    }
    return query.getResultList();
  }

  public AsyncMeasureSnapshot createAsyncMeasureSnapshot(Long asyncMeasureId, Integer snapshotId, Date AsyncMeasureDate, Date snapshotDate, Integer metricId, Integer projectId) {
    AsyncMeasureSnapshot asyncMeasureSnapshot = new AsyncMeasureSnapshot(asyncMeasureId, snapshotId, AsyncMeasureDate, snapshotDate, metricId, projectId);
    getSession().save(asyncMeasureSnapshot);
    return asyncMeasureSnapshot;
  }

  public void updateAsyncMeasureSnapshot(AsyncMeasureSnapshot asyncMeasureSnapshot, Snapshot snapshot) {
    if (snapshot != null) {
      asyncMeasureSnapshot.setSnapshotId(snapshot.getId());
      asyncMeasureSnapshot.setSnapshotDate(snapshot.getCreatedAt());
    } else {
      asyncMeasureSnapshot.setSnapshotId(null);
      asyncMeasureSnapshot.setSnapshotDate(null);
    }
    getSession().merge(asyncMeasureSnapshot);
  }

  public void removeSnapshotFromAsyncMeasureSnapshot(AsyncMeasureSnapshot asyncMeasureSnapshot) {
    asyncMeasureSnapshot.setSnapshotId(null);
    asyncMeasureSnapshot.setSnapshotDate(null);
    getSession().merge(asyncMeasureSnapshot);
  }


  public AsyncMeasureSnapshot getNextAsyncMeasureSnapshot(Integer projetcId, Integer metricId, Date date) {
    try {
      return (AsyncMeasureSnapshot) getSession().createQuery(
          "SELECT ams FROM AsyncMeasureSnapshot ams " +
              "WHERE ams.projectId=:projectId " +
              "AND ams.metricId=:metricId " +
              "AND ams.measureDate>:date " +
              "ORDER BY ams.measureDate ASC")
          .setParameter("projectId", projetcId)
          .setParameter("metricId", metricId)
          .setParameter("date", date)
          .setMaxResults(1)
          .getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public List<AsyncMeasureSnapshot> getNextAsyncMeasureSnapshotsUntilDate(MeasureModel asyncMeasure, Date endDate) {
    Query query = getSession().createQuery(
        "SELECT ams FROM AsyncMeasureSnapshot ams " +
            "WHERE ams.projectId=:projectId " +
            "AND ams.metricId=:metricId " +
            (endDate != null ? "AND ams.measureDate<:endDate " : "") +
            "AND ams.snapshotDate>=:measureDate " +
            "ORDER BY ams.snapshotDate ASC ")
        .setParameter("projectId", asyncMeasure.getProjectId())
        .setParameter("metricId", asyncMeasure.getMetric().getId())
        .setParameter("measureDate", asyncMeasure.getMeasureDate());
    if (endDate != null) {
      query.setParameter("endDate", endDate);
    }
    return query.getResultList();
  }

  public List<AsyncMeasureSnapshot> getPreviousAsyncMeasureSnapshots(Integer projectId, Date beginDate, Date endDate) {
    Query query = getSession().createQuery(
        "SELECT ams FROM AsyncMeasureSnapshot ams " +
            "WHERE ams.projectId=:projectId " +
            "AND ams.measureDate<=:endDate " +
            (beginDate != null ? "AND ams.measureDate>:beginDate " : "") +
            "AND ams.snapshotId IS NULL " +
            "ORDER BY ams.measureDate ASC")
        .setParameter("projectId", projectId)
        .setParameter("endDate", endDate);
    if (beginDate != null) {
      query.setParameter("beginDate", beginDate);
    }
    return query.getResultList();
  }

  public List<AsyncMeasureSnapshot> getAsyncMeasureSnapshotsFromSnapshotId(Integer snapshotId, List<Integer> metricIdsToExclude) {
    Query query = getSession().createQuery(
        "SELECT ams FROM AsyncMeasureSnapshot ams " +
            "WHERE ams.snapshotId=:snapshotId " +
            (!metricIdsToExclude.isEmpty() ? "AND ams.metricId NOT IN (:metricIdsToExclude) " : "") +
            "ORDER BY ams.measureDate ASC")
        .setParameter("snapshotId", snapshotId);
    if (!metricIdsToExclude.isEmpty()) {
      query.setParameter("metricIdsToExclude", metricIdsToExclude);
    }
    return query.getResultList();
  }

  public AsyncMeasureSnapshot getLastAsyncMeasureSnapshot(Integer projetcId, Integer metricId, Date date) {
    try {
      return (AsyncMeasureSnapshot) getSession().createQuery(
          "SELECT ams FROM AsyncMeasureSnapshot ams " +
              "WHERE ams.projectId=:projectId " +
              "AND ams.metricId=:metricId " +
              "AND ams.measureDate<:date " +
              "ORDER BY ams.measureDate DESC")
          .setParameter("projectId", projetcId)
          .setParameter("metricId", metricId)
          .setParameter("date", date)
          .setMaxResults(1)
          .getSingleResult();
    } catch (NoResultException ex) {
      return null;
    }
  }

  public void deleteAsyncMeasureSnapshots(Long asyncMeasureId) {
    getSession().createQuery(
        "DELETE FROM AsyncMeasureSnapshot ams WHERE ams.measureId=:measureId")
        .setParameter("measureId", asyncMeasureId)
        .executeUpdate();
  }

}
