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

import org.sonar.api.config.Settings;
import org.sonar.server.organization.BillingValidationsExtension;

import static java.lang.String.format;

public class FakeBillingValidations implements BillingValidationsExtension {

  private static final String PREVENT_PROJECT_ANALYSIS_SETTING = "sonar.billing.preventProjectAnalysis";
  private static final String PREVENT_UPDATING_PROJECTS_VISIBILITY_TO_PRIVATE_SETTING = "sonar.billing.preventUpdatingProjectsVisibilityToPrivate";

  private final Settings settings;

  public FakeBillingValidations(Settings settings) {
    this.settings = settings;
  }

  @Override
  public void checkOnProjectAnalysis(Organization organization) {
    boolean preventProjectAnalysis = settings.getBoolean(PREVENT_PROJECT_ANALYSIS_SETTING);
    if (preventProjectAnalysis) {
      throw new BillingValidationsException(format("Organization %s cannot perform analysis", organization.getKey()));
    }
  }

  @Override
  public void checkCanUpdateProjectVisibility(Organization organization, boolean updateToPrivate) {
    boolean preventUpdatingProjectsToPrivate = settings.getBoolean(PREVENT_UPDATING_PROJECTS_VISIBILITY_TO_PRIVATE_SETTING);
    if (preventUpdatingProjectsToPrivate) {
      throw new BillingValidationsException(format("Organization %s cannot use private project", organization.getKey()));
    }
  }

  @Override
  public boolean canUpdateProjectVisibilityToPrivate(Organization organization) {
    if (!settings.hasKey(PREVENT_UPDATING_PROJECTS_VISIBILITY_TO_PRIVATE_SETTING)) {
      return true;
    }
    return !settings.getBoolean(PREVENT_UPDATING_PROJECTS_VISIBILITY_TO_PRIVATE_SETTING);
  }
}
