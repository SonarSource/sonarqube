/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.v2.api.user.request;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import org.sonar.server.v2.common.model.UpdateField;

public class UserUpdateRestRequest {

  private UpdateField<String> login = UpdateField.undefined();
  private UpdateField<String> name = UpdateField.undefined();
  private UpdateField<String> email = UpdateField.undefined();
  private UpdateField<List<String>> scmAccounts = UpdateField.undefined();
  private UpdateField<String> externalProvider = UpdateField.undefined();
  private UpdateField<String> externalLogin = UpdateField.undefined();
  private UpdateField<String> externalId = UpdateField.undefined();

  @Size(min = 2, max = 100)
  @Schema(description = "User login")
  public UpdateField<String> getLogin() {
    return login;
  }

  public void setLogin(String login) {
    this.login = UpdateField.withValue(login);
  }

  @Size(max = 200)
  @Schema(description = "User first name and last name", implementation = String.class)
  public UpdateField<String> getName() {
    return name;
  }

  public void setName(String name) {
    this.name = UpdateField.withValue(name);
  }

  @Email
  @Size(min = 1, max = 100)
  @Schema(implementation = String.class, description = "Email")
  public UpdateField<String> getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = UpdateField.withValue(email);
  }

  @ArraySchema(arraySchema = @Schema(description = "List of SCM accounts."), schema = @Schema(implementation = String.class))
  public UpdateField<List<String>> getScmAccounts() {
    return scmAccounts;
  }

  public void setScmAccounts(List<String> scmAccounts) {
    this.scmAccounts = UpdateField.withValue(scmAccounts);
  }

  @Schema(implementation = String.class, description = """
    New identity provider. Only providers configured in your platform are supported. This could be: github, gitlab, bitbucket, saml, LDAP, LDAP_{serverKey}
    (according to your server configuration file).
    Warning: when this is updated, the user will only be able to authenticate using the new identity provider. Also, it is not possible to remove the identity provider of a user.
    """)
  public UpdateField<String> getExternalProvider() {
    return externalProvider;
  }

  public void setExternalProvider(@Nullable String externalProvider) {
    this.externalProvider = UpdateField.withValue(externalProvider);
  }

  @Size(min = 1, max = 255)
  @Schema(implementation = String.class, description = "New external login, usually the login used in the authentication system.")
  public UpdateField<String> getExternalLogin() {
    return externalLogin;
  }

  public void setExternalLogin(@Nullable String externalLogin) {
    this.externalLogin = UpdateField.withValue(externalLogin);
  }

  @Size(min = 1, max = 255)
  @Schema(implementation = String.class, description = "New external id in the authentication system.")
  public UpdateField<String> getExternalId() {
    return externalId;
  }

  public void setExternalId(@Nullable String externalId) {
    this.externalId = UpdateField.withValue(externalId);
  }
}
