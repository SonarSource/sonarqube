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
package org.sonar.core.technicaldebt.db;

import org.sonar.core.technicaldebt.DefaultCharacteristic;

import java.io.Serializable;

public class CharacteristicDto implements Serializable {

  private Long id;
  private String kee;
  private String name;
  private Integer depth;
  private Integer order;

  public Long getId() {
    return id;
  }

  public CharacteristicDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public CharacteristicDto setKey(String s) {
    this.kee = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public CharacteristicDto setName(String s) {
    this.name = s;
    return this;
  }

  public Integer getDepth() {
    return depth;
  }

  public CharacteristicDto setDepth(Integer i) {
    this.depth = i;
    return this;
  }

  public int getOrder() {
    return order;
  }

  public CharacteristicDto setOrder(Integer i) {
    this.order = i;
    return this;
  }

  public DefaultCharacteristic toDefaultCharacteristic() {
    return new DefaultCharacteristic()
      .setKey(kee)
      .setName(name);
  }

}
