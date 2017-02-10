/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.permission.ws.template;

import org.junit.rules.ExternalResource;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceType;
import org.sonar.api.resources.ResourceTypeTree;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.organization.DefaultTemplates;

public class DefaultTemplatesResolverRule extends ExternalResource implements DefaultTemplatesResolver {
  private static final DefaultTemplatesResolver WITH_VIEWS = new DefaultTemplatesResolverImpl(
    new ResourceTypes(new ResourceTypeTree[] {
      ResourceTypeTree.builder()
        .addType(ResourceType.builder(Qualifiers.PROJECT).build())
        .build(),
      ResourceTypeTree.builder()
        .addType(ResourceType.builder(Qualifiers.VIEW).build())
        .build()}));
  private static final DefaultTemplatesResolver WITHOUT_VIEWS = new DefaultTemplatesResolverImpl(
    new ResourceTypes(new ResourceTypeTree[] {ResourceTypeTree.builder()
      .addType(ResourceType.builder(Qualifiers.PROJECT).build())
      .build()}));

  private final boolean governanceInitiallyInstalled;
  private boolean governanceInstalled;

  private DefaultTemplatesResolverRule(boolean governanceInitiallyInstalled) {
    this.governanceInitiallyInstalled = governanceInitiallyInstalled;
    this.governanceInstalled = governanceInitiallyInstalled;
  }

  @Override
  protected void before() throws Throwable {
    this.governanceInstalled = governanceInitiallyInstalled;
  }

  public void installGovernance() {
    this.governanceInstalled = true;
  }

  public void uninstallGovernance() {
    this.governanceInstalled = false;
  }

  public static DefaultTemplatesResolverRule withoutGovernance() {
    return new DefaultTemplatesResolverRule(false);
  }

  public static DefaultTemplatesResolverRule withGovernance() {
    return new DefaultTemplatesResolverRule(true);
  }

  @Override
  public DefaultTemplatesResolverImpl.ResolvedDefaultTemplates resolve(DefaultTemplates defaultTemplates) {
    if (governanceInstalled) {
      return WITH_VIEWS.resolve(defaultTemplates);
    }
    return WITHOUT_VIEWS.resolve(defaultTemplates);
  }
}
