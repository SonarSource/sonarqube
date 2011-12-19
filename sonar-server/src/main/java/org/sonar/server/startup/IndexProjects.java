/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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

import org.sonar.api.ServerComponent;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.TimeProfiler;
import org.sonar.jpa.entity.SchemaMigration;
import org.sonar.persistence.resource.ResourceIndexerDao;
import org.sonar.persistence.resource.ResourceIndexerFilter;

/**
 * Index existing projects during migration to 2.13. Since this latter version, resources are automatically indexed
 * during project analysis.
 *
 * @since 2.13
 */
public class IndexProjects implements ServerComponent {

  private ServerUpgradeStatus upgradeStatus;
  private ResourceIndexerDao indexerDao;

  public IndexProjects(ServerUpgradeStatus upgradeStatus, ResourceIndexerDao indexerDao) {
    this.upgradeStatus = upgradeStatus;
    this.indexerDao = indexerDao;
  }

  public void start() {
    if (shouldIndex()) {
      index();
    }
  }

  private boolean shouldIndex() {
    return upgradeStatus.isUpgraded() && upgradeStatus.getInitialDbVersion() < SchemaMigration.VERSION_2_13;
  }

  private void index() {
    TimeProfiler profiler = new TimeProfiler().start("Index projects");
    indexerDao.index(newFilter());
    profiler.stop();
  }

  private static ResourceIndexerFilter newFilter() {
    return ResourceIndexerFilter.create()
      .setQualifiers(new String[]{Qualifiers.PROJECT, Qualifiers.VIEW, Qualifiers.SUBVIEW})
      .setScopes(new String[]{Scopes.PROJECT});
  }

}
