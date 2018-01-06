/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.component;

import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ResourceTypesRule;

public class TestComponentFinder extends ComponentFinder {
  private TestComponentFinder(DbClient dbClient, ResourceTypes resourceTypes) {
    super(dbClient, resourceTypes);
  }

  public static TestComponentFinder from(DbTester dbTester) {
    return new TestComponentFinder(dbTester.getDbClient(), new ResourceTypesRule().setRootQualifiers(Qualifiers.PROJECT));
  }
}
