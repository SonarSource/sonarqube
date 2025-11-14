/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.common.permission;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.server.component.ComponentTypesRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentQualifiers.VIEW;

public class DefaultTemplatesResolverImplIT {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private ComponentTypesRule resourceTypesWithPortfoliosInstalled = new ComponentTypesRule().setRootQualifiers(PROJECT, APP, VIEW);
  private ComponentTypesRule resourceTypesWithApplicationInstalled = new ComponentTypesRule().setRootQualifiers(PROJECT, APP);
  private ComponentTypesRule resourceTypes = new ComponentTypesRule().setRootQualifiers(PROJECT);

  private DefaultTemplatesResolverImpl underTestWithPortfoliosInstalled = new DefaultTemplatesResolverImpl(db.getDbClient(), resourceTypesWithPortfoliosInstalled);
  private DefaultTemplatesResolverImpl underTestWithApplicationInstalled = new DefaultTemplatesResolverImpl(db.getDbClient(), resourceTypesWithApplicationInstalled);
  private DefaultTemplatesResolverImpl underTest = new DefaultTemplatesResolverImpl(db.getDbClient(), resourceTypes);

  @Test
  public void get_default_templates_when_portfolio_not_installed() {
    db.permissionTemplates().setDefaultTemplates("prj", null, null);

    assertThat(underTest.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTest.resolve(db.getSession()).getApplication()).isEmpty();
    assertThat(underTest.resolve(db.getSession()).getPortfolio()).isEmpty();
  }

  @Test
  public void get_default_templates_always_return_project_template_even_when_all_templates_are_defined_but_portfolio_not_installed() {
    db.permissionTemplates().setDefaultTemplates("prj", "app", "port");

    assertThat(underTest.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTest.resolve(db.getSession()).getApplication()).isEmpty();
    assertThat(underTest.resolve(db.getSession()).getPortfolio()).isEmpty();
  }

  @Test
  public void get_default_templates_always_return_project_template_when_only_project_template_and_portfolio_is_installed_() {
    db.permissionTemplates().setDefaultTemplates("prj", null, null);

    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getApplication()).contains("prj");
    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getPortfolio()).contains("prj");
  }

  @Test
  public void get_default_templates_for_all_components_when_portfolio_is_installed() {
    db.permissionTemplates().setDefaultTemplates("prj", "app", "port");

    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getApplication()).contains("app");
    assertThat(underTestWithPortfoliosInstalled.resolve(db.getSession()).getPortfolio()).contains("port");
  }

  @Test
  public void get_default_templates_always_return_project_template_when_only_project_template_and_application_is_installed_() {
    db.permissionTemplates().setDefaultTemplates("prj", null, null);

    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getApplication()).contains("prj");
    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getPortfolio()).isEmpty();
  }

  @Test
  public void get_default_templates_for_all_components_when_application_is_installed() {
    db.permissionTemplates().setDefaultTemplates("prj", "app", null);

    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getProject()).contains("prj");
    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getApplication()).contains("app");
    assertThat(underTestWithApplicationInstalled.resolve(db.getSession()).getPortfolio()).isEmpty();
  }

  @Test
  public void fail_when_default_template_for_project_is_missing() {
    DbSession session = db.getSession();
    assertThatThrownBy(() -> underTestWithPortfoliosInstalled.resolve(session))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Default template for project is missing");
  }

}
