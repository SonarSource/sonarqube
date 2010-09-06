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
package org.sonar.server.startup;

import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;
import org.sonar.api.profiles.Alert;
import org.sonar.api.utils.Logs;
import org.sonar.api.utils.TimeProfiler;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Query;

public class RegisterMetrics {

  private MeasuresDao measuresDao;
  private Metrics[] metricsRepositories = null;
  private DatabaseSession session;

  public RegisterMetrics(DatabaseSession session, MeasuresDao measuresDao, Metrics[] metricsRepositories) {
    this.session = session;
    this.measuresDao = measuresDao;
    this.metricsRepositories = metricsRepositories;
  }

  public RegisterMetrics(MeasuresDao measuresDao) {
    this.measuresDao = measuresDao;
  }

  public void start() {
    TimeProfiler profiler = new TimeProfiler().start("Load metrics");
    measuresDao.disableAutomaticMetrics();
    List<Metric> metricsToRegister = new ArrayList<Metric>();
    metricsToRegister.addAll(CoreMetrics.getMetrics());
    if (metricsRepositories != null) {
      for (Metrics metrics : metricsRepositories) {
        metricsToRegister.addAll(metrics.getMetrics());
      }
    }
    register(metricsToRegister);
    cleanAlerts();
    profiler.stop();
  }

  protected void cleanAlerts() {
    Logs.INFO.info("cleaning alert thresholds...");
    Query query = session.createQuery("delete from " + Alert.class.getSimpleName() + " a where NOT EXISTS(FROM Metric m WHERE m=a.metric))");
    query.executeUpdate();

    query = session.createQuery("delete from " + Alert.class.getSimpleName() + " a where NOT EXISTS(FROM Metric m WHERE m=a.metric and m.enabled=true))");
    query.executeUpdate();
    session.commit();
  }

  protected void register(List<Metric> metrics) {
    measuresDao.registerMetrics(metrics);
  }
}
