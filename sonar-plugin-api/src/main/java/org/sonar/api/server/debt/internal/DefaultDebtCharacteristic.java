/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.api.server.debt.internal;

import org.sonar.api.server.debt.DebtCharacteristic;

/**
 * @since 4.3
 */
public class DefaultDebtCharacteristic implements DebtCharacteristic {

  private Integer id;
  private String key;
  private String name;
  private Integer order;
  private Integer parentId;

  @Override
  public Integer id() {
    return id;
  }

  public DefaultDebtCharacteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  @Override
  public String key() {
    return key;
  }

  public DefaultDebtCharacteristic setKey(String key) {
    this.key = key;
    return this;
  }

  @Override
  public String name() {
    return name;
  }

  public DefaultDebtCharacteristic setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public Integer order() {
    return order;
  }

  public DefaultDebtCharacteristic setOrder(Integer order) {
    this.order = order;
    return this;
  }

  @Override
  public Integer parentId() {
    return parentId;
  }

  public DefaultDebtCharacteristic setParentId(Integer parentId) {
    this.parentId = parentId;
    return this;
  }
}
