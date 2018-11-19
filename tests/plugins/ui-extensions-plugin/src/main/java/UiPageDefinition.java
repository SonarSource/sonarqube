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
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;

import static org.sonar.api.web.page.Page.Scope.COMPONENT;
import static org.sonar.api.web.page.Page.Scope.ORGANIZATION;

public class UiPageDefinition implements PageDefinition {
  @Override
  public void define(Context context) {
    context
      .addPage(Page.builder("uiextensionsplugin/global_page").setName("Global Page").build())
      .addPage(Page.builder("uiextensionsplugin/global_admin_page").setName("Global Admin Page").setAdmin(true).build())
      .addPage(Page.builder("uiextensionsplugin/project_page").setName("Project Page").setScope(COMPONENT).build())
      .addPage(Page.builder("uiextensionsplugin/project_admin_page").setName("Project Admin Page").setScope(COMPONENT).setAdmin(true).build())
      .addPage(Page.builder("uiextensionsplugin/organization_page").setName("Organization Page").setScope(ORGANIZATION).build())
      .addPage(Page.builder("uiextensionsplugin/organization_admin_page").setName("Organization Admin Page").setScope(ORGANIZATION).setAdmin(true).build());
  }
}
