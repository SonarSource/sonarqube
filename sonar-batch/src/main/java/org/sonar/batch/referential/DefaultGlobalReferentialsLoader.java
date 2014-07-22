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
import org.sonar.batch.protocol.input.GlobalReferentials;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Collection;

/**
 * TODO This is currently implemented by accessing DB but should be replaced by WS call
 */
public class DefaultGlobalReferentialsLoader implements GlobalReferentialsLoader {

  private static final String ENABLED = "enabled";

  private final DatabaseSessionFactory sessionFactory;

  public DefaultGlobalReferentialsLoader(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public GlobalReferentials load() {
    GlobalReferentials ref = new GlobalReferentials();
    for (Metric m : sessionFactory.getSession().getResults(Metric.class, ENABLED, true)) {
      Boolean optimizedBestValue = m.isOptimizedBestValue();
      Boolean qualitative = m.getQualitative();
      Boolean userManaged = m.getUserManaged();
      ref.metrics().add(
        new org.sonar.batch.protocol.input.Metric(m.getId(), m.getKey(),
          m.getType().name(),
          m.getDescription(),
          m.getDirection(),
          m.getName(),
          qualitative != null ? m.getQualitative() : false,
          userManaged != null ? m.getUserManaged() : false,
          m.getWorstValue(),
          m.getBestValue(),
          optimizedBestValue != null ? optimizedBestValue : false));
    }

    return ref;
  }

  private Collection<Metric> doFindAll() {
    return sessionFactory.getSession().getResults(Metric.class, ENABLED, true);
  }
}
