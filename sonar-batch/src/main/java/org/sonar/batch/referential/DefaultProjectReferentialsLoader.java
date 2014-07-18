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
package org.sonar.batch.referential;

import org.sonar.api.measures.Metric;
import org.sonar.batch.protocol.input.ProjectReferentials;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;

public class DefaultProjectReferentialsLoader implements ProjectReferentialsLoader {

  private static final String ENABLED = "enabled";
  private DatabaseSessionFactory sessionFactory;

  protected Collection<Metric> doFindAll() {
    return sessionFactory.getSession().getResults(Metric.class, ENABLED, true);
  }

  public DefaultProjectReferentialsLoader(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  public ProjectReferentials load(String projectKey) {
    ProjectReferentials ref = new ProjectReferentials();
    for (Metric m : sessionFactory.getSession().getResults(Metric.class, ENABLED, true)) {
      ref.metrics().add(new org.sonar.batch.protocol.input.Metric(m.getKey(), m.getType().name()));
    }
    return ref;
  }
}
