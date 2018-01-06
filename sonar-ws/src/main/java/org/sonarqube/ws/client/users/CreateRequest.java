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
package org.sonarqube.ws.client.users;

import java.util.List;
import javax.annotation.Generated;

/**
 * This is part of the internal API.
 * This is a POST request.
 * @see <a href="https://next.sonarqube.com/sonarqube/web_api/api/users/create">Further information about this action online (including a response example)</a>
 * @since 3.7
 */
@Generated("sonar-ws-generator")
public class CreateRequest {

  private String email;
  private String local;
  private String login;
  private String name;
  private String password;
  private List<String> scmAccount;
  private List<String> scmAccounts;

  /**
   * Example value: "myname@email.com"
   */
  public CreateRequest setEmail(String email) {
    this.email = email;
    return this;
  }

  public String getEmail() {
    return email;
  }

  /**
   * Possible values:
   * <ul>
   *   <li>"true"</li>
   *   <li>"false"</li>
   *   <li>"yes"</li>
   *   <li>"no"</li>
   * </ul>
   */
  public CreateRequest setLocal(String local) {
    this.local = local;
    return this;
  }

  public String getLocal() {
    return local;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "myuser"
   */
  public CreateRequest setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getLogin() {
    return login;
  }

  /**
   * This is a mandatory parameter.
   * Example value: "My Name"
   */
  public CreateRequest setName(String name) {
    this.name = name;
    return this;
  }

  public String getName() {
    return name;
  }

  /**
   * Example value: "mypassword"
   */
  public CreateRequest setPassword(String password) {
    this.password = password;
    return this;
  }

  public String getPassword() {
    return password;
  }

  /**
   * Example value: "scmAccount=firstValue&scmAccount=secondValue&scmAccount=thirdValue"
   */
  public CreateRequest setScmAccount(List<String> scmAccount) {
    this.scmAccount = scmAccount;
    return this;
  }

  public List<String> getScmAccount() {
    return scmAccount;
  }

  /**
   * Example value: "myscmaccount1,myscmaccount2"
   * @deprecated since 6.1
   */
  @Deprecated
  public CreateRequest setScmAccounts(List<String> scmAccounts) {
    this.scmAccounts = scmAccounts;
    return this;
  }

  public List<String> getScmAccounts() {
    return scmAccounts;
  }
}
