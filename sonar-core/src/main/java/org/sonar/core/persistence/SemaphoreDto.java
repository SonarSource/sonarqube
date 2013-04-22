/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.persistence;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import java.util.Date;

/**
 * @since 3.4
 */
public class SemaphoreDto {
  private Long id;
  private String name;
  private String checksum;
  private Date lockedAt;
  private Date createdAt;
  private Date updatedAt;

  public String getName() {
    return name;
  }

  public SemaphoreDto setName(String s) {
    this.name = s;
    this.checksum = DigestUtils.md5Hex(s);
    return this;
  }

  public Date getLockedAt() {
    return lockedAt;
  }

  public SemaphoreDto setLockedAt(Date d) {
    this.lockedAt = d;
    return this;
  }

  public Long getId() {
    return id;
  }

  public SemaphoreDto setId(Long id) {
    this.id = id;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public SemaphoreDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public SemaphoreDto setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }
}
