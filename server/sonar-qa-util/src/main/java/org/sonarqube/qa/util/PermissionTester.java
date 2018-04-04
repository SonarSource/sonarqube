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
package org.sonarqube.qa.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.sonarqube.ws.Organizations;
import org.sonarqube.ws.Permissions.PermissionTemplate;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.permissions.AddUserToTemplateRequest;
import org.sonarqube.ws.client.permissions.ApplyTemplateRequest;
import org.sonarqube.ws.client.permissions.CreateTemplateRequest;
import org.sonarqube.ws.client.permissions.PermissionsService;

import static com.sonar.orchestrator.container.Server.ADMIN_LOGIN;
import static java.util.Arrays.stream;

public class PermissionTester {

  private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

  private final TesterSession session;

  PermissionTester(TesterSession session) {
    this.session = session;
  }

  @SafeVarargs
  public final PermissionTemplate generateTemplate(Consumer<CreateTemplateRequest>... populators) {
    return generateTemplate(null, populators);
  }

  @SafeVarargs
  public final PermissionTemplate generateTemplate(@Nullable Organizations.Organization organization, Consumer<CreateTemplateRequest>... populators) {
    int id = ID_GENERATOR.getAndIncrement();
    String name = "template" + id;
    CreateTemplateRequest request = new CreateTemplateRequest()
      .setName(name)
      .setOrganization(organization != null ? organization.getKey() : null);
    stream(populators).forEach(p -> p.accept(request));
    PermissionTemplate template = service().createTemplate(request).getPermissionTemplate();
    // Give browse and admin permissions to admin in order to allow admin wsclient to perform any operation on created projects
    addUserToTemplate(organization, ADMIN_LOGIN, template, "user");
    addUserToTemplate(organization, ADMIN_LOGIN, template, "admin");
    return template;
  }

  public void addUserToTemplate(Users.CreateWsResponse.User user, PermissionTemplate template, String permission) {
    addUserToTemplate(null, user, template, permission);
  }

  public void addUserToTemplate(@Nullable Organizations.Organization organization, Users.CreateWsResponse.User user, PermissionTemplate template, String permission) {
    addUserToTemplate(organization, user.getLogin(), template, permission);
  }

  private void addUserToTemplate(@Nullable Organizations.Organization organization, String userLogin, PermissionTemplate template, String permission) {
    service().addUserToTemplate(new AddUserToTemplateRequest()
      .setOrganization(organization != null ? organization.getKey() : null)
      .setLogin(userLogin)
      .setTemplateName(template.getName())
      .setPermission(permission));
  }

  public void applyTemplate(PermissionTemplate template, Project project) {
    applyTemplate(null, template, project);
  }

  public void applyTemplate(@Nullable Organizations.Organization organization, PermissionTemplate template, Project project) {
    service().applyTemplate(
      new ApplyTemplateRequest()
        .setOrganization(organization != null ? organization.getKey() : null)
        .setTemplateName(template.getName())
        .setProjectKey(project.getKey()));
  }

  public PermissionsService service() {
    return session.wsClient().permissions();
  }

}
