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
package org.sonar.db.user;

import java.util.Date;

/**
 * @since 3.0
 */
public final class AuthorDto {

  private Long id;
  private Long personId;
  private String login;
  private Date createdAt;
  private Date updatedAt;

  public Long getId() {
    return id;
  }

  public AuthorDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getPersonId() {
    return personId;
  }

  public AuthorDto setPersonId(Long personId) {
    this.personId = personId;
    return this;
  }

  public String getLogin() {
    return login;
  }

  public AuthorDto setLogin(String login) {
    this.login = login;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;// NOSONAR May expose internal representation by returning reference to mutable object
  }

  public AuthorDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;// NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;// NOSONAR May expose internal representation by returning reference to mutable object
  }

  public AuthorDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;// NOSONAR May expose internal representation by incorporating reference to mutable object
    return this;
  }

}
