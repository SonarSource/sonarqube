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
package org.sonarqube.ws.tester;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.sonarqube.ws.Permissions.PermissionTemplate;
import org.sonarqube.ws.Permissions.SearchTemplatesWsResponse.TemplateIdQualifier;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.client.permissions.AddGroupRequest;
import org.sonarqube.ws.client.permissions.AddGroupToTemplateRequest;
import org.sonarqube.ws.client.permissions.AddProjectCreatorToTemplateRequest;
import org.sonarqube.ws.client.permissions.AddUserRequest;
import org.sonarqube.ws.client.permissions.AddUserToTemplateRequest;
import org.sonarqube.ws.client.permissions.ApplyTemplateRequest;
import org.sonarqube.ws.client.permissions.CreateTemplateRequest;
import org.sonarqube.ws.client.permissions.PermissionsService;
import org.sonarqube.ws.client.permissions.SearchTemplatesRequest;
import org.sonarqube.ws.client.permissions.SetDefaultTemplateRequest;

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
    int id = ID_GENERATOR.getAndIncrement();
    String name = "template" + id;
    CreateTemplateRequest request = new CreateTemplateRequest()
      .setName(name);
    stream(populators).forEach(p -> p.accept(request));
    PermissionTemplate template = service().createTemplate(request).getPermissionTemplate();
    // Give browse and admin permissions to admin in order to allow admin wsclient to perform any operation on created projects
    addUserToTemplate(ADMIN_LOGIN, template, "user");
    addUserToTemplate(ADMIN_LOGIN, template, "admin");
    return template;
  }

  public void addUserToTemplate(Users.CreateWsResponse.User user, PermissionTemplate template, String permission) {
    addUserToTemplate(user.getLogin(), template, permission);
  }

  public void addUserToTemplate(String login, PermissionTemplate template, String permission) {
    service().addUserToTemplate(new AddUserToTemplateRequest()
      .setLogin(login)
      .setTemplateName(template.getName())
      .setPermission(permission));
  }

  public void addGroup(String groupName, String permission) {
    service().addGroup(new AddGroupRequest().setGroupName(groupName).setPermission(permission));
  }

  public void addUser(String login, String permission) {
    service().addUser(new AddUserRequest().setLogin(login).setPermission(permission));
  }

  public void addGroupToTemplate(String groupName, PermissionTemplate template, String permission) {
    service().addGroupToTemplate(new AddGroupToTemplateRequest()
      .setGroupName(groupName)
      .setTemplateName(template.getName())
      .setPermission(permission));
  }

  public void addCreatorToTemplate(PermissionTemplate template, String permission) {
    this.service().addProjectCreatorToTemplate(
      new AddProjectCreatorToTemplateRequest()
        .setPermission(permission)
        .setTemplateId(template.getId()));
  }

  public void applyTemplate(PermissionTemplate template, Project project) {
    service().applyTemplate(
      new ApplyTemplateRequest()
        .setTemplateName(template.getName())
        .setProjectKey(project.getKey()));
  }

  public TemplateIdQualifier getDefaultTemplateForProject() {
    return service().searchTemplates(new SearchTemplatesRequest()).getDefaultTemplatesList()
      .stream()
      .filter(t -> t.getQualifier().equals("TRK"))
      .findFirst()
      .orElseThrow(() -> {
        throw new IllegalStateException("Cannot find default template for project");
      });
  }

  public void setDefaultTemplate(TemplateIdQualifier template) {
    service().setDefaultTemplate(new SetDefaultTemplateRequest().setTemplateId(template.getTemplateId()));
  }

  public void setDefaultTemplate(PermissionTemplate template) {
    service().setDefaultTemplate(new SetDefaultTemplateRequest().setTemplateId(template.getId()));
  }

  public PermissionsService service() {
    return session.wsClient().permissions();
  }

}
