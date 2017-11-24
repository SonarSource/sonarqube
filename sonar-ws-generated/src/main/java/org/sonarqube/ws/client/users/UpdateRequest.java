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
package org.sonarqube.ws.client.users;

import java.util.List;
import javax.annotation.Generated;

/**
 * Update a user. If a deactivated user account exists with the given login, it will be reactivated. Requires Administer System permission
 *
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/update">Further information about this action online (including a response example)</a>
 * @since 3.7
 */
@Generated("sonar-ws-generator")
public class UpdateRequest {

  private String email;
  private String login;
  private String name;
  private String scmAccount;
  private String scmAccounts;

  /**
   * User email
   *
   * Example value: "myname@email.com"
   */
  public UpdateRequest setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getEmail() {
    return email;
  }

  /**
   * User login
   *
   * This is a mandatory parameter.
   * Example value: "myuser"
   */
  public UpdateRequest setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getLogin() {
    return login;
  }

  /**
   * User name
   *
   * Example value: "My Name"
   */
  public UpdateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * SCM accounts. To set several values, the parameter must be called once for each value.
   *
   * Example value: "scmAccount=firstValue&scmAccount=secondValue&scmAccount=thirdValue"
   */
  public UpdateRequest setScmAccount(String scmAccount) {
    this.scmAccount = scmAccount;
    return this;
  }

  public String getScmAccount() {
    return scmAccount;
  }

  /**
   * This parameter is deprecated, please use 'scmAccount' instead
   *
   * Example value: "myscmaccount1,myscmaccount2"
   * @deprecated since 6.1
   */
  @Deprecated
  public UpdateRequest setScmAccounts(String scmAccounts) {
    this.scmAccounts = scmAccounts;
    return this;
  }

  public String getScmAccounts() {
    return scmAccounts;
  }
}
