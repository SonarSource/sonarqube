/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.qualitymodel;

import javax.persistence.*;

/**
 * @since 2.3
 */
@Entity
@Table(name = "characteristic_properties")
public final class CharacteristicProperty {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "kee", nullable = true, length = 100)
  private String key;

  @Column(name = "value", nullable = true)
  private Double value;

  @Column(name = "text_value", nullable = true, length = 4000)
  private String textValue;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "characteristic_id", updatable = true, nullable = false)
  private Characteristic characteristic;

  /**
   * Use the factory method create()
   */
  CharacteristicProperty() {
  }

  public static CharacteristicProperty create(String key) {
    return new CharacteristicProperty().setKey(key);
  }

  public Integer getId() {
    return id;
  }

  CharacteristicProperty setId(Integer i) {
    this.id = i;
    return this;
  }

  public String getKey() {
    return key;
  }

  public CharacteristicProperty setKey(String s) {
    this.key = s;
    return this;
  }

  public String getTextValue() {
    return textValue;
  }

  public Double getValue() {
    return value;
  }

  public Long getValueAsLong() {
    if (value!=null) {
      return value.longValue();
    }
    return null;
  }

  public CharacteristicProperty setTextValue(String s) {
    this.textValue = s;
    return this;
  }

  public CharacteristicProperty setValue(Double d) {
    this.value = d;
    return this;
  }

  Characteristic getCharacteristic() {
    return characteristic;
  }

  CharacteristicProperty setCharacteristic(Characteristic c) {
    this.characteristic = c;
    return this;
  }
}
