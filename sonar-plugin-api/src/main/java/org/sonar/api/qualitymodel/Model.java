/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.qualitymodel;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.rules.Rule;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

/**
 * @since 2.3
 */
@Entity
@Table(name = "quality_models")
public final class Model implements Comparable<Model> {

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Characteristic> characteristics = new ArrayList<Characteristic>();

  Model() {
  }

  public static Model create() {
    return new Model();
  }

  public static Model createByName(String s) {
    return new Model().setName(s);
  }

  public Characteristic createCharacteristicByName(String name) {
    Characteristic c = new Characteristic().setName(name, true);
    return addCharacteristic(c);
  }

  public Characteristic createCharacteristicByKey(String key, String name) {
    Characteristic c = new Characteristic().setKey(key).setName(name, false);
    return addCharacteristic(c);
  }

  public Characteristic createCharacteristicByRule(Rule rule) {
    Characteristic c = new Characteristic().setRule(rule);
    return addCharacteristic(c);
  }

  public Integer getId() {
    return id;
  }

  Model setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public List<Characteristic> getRootCharacteristics() {
    return getCharacteristicsByDepth(Characteristic.ROOT_DEPTH);
  }

  public Model setName(String name) {
    this.name = StringUtils.trim(name);
    return this;
  }

  private Characteristic addCharacteristic(Characteristic c) {
    c.setModel(this);
    c.setOrder(characteristics.size()+1);
    characteristics.add(c);
    return c;
  }

  public List<Characteristic> getCharacteristics() {
    return characteristics;
  }

  public Characteristic getCharacteristicByKey(String key) {
    for (Characteristic characteristic : getCharacteristics()) {
      if (StringUtils.equals(key, characteristic.getKey())) {
        return characteristic;
      }
    }
    return null;
  }

  public List<Characteristic> getCharacteristicsByDepth(int depth) {
    List<Characteristic> result = new ArrayList<Characteristic>();
    for (Characteristic c : characteristics) {
      if (c.getDepth()==depth) {
        result.add(c);
      }
    }
    return result;
  }

  public Characteristic getCharacteristicByName(String name) {
    for (Characteristic characteristic : getCharacteristics()) {
      if (StringUtils.equals(name, characteristic.getName())) {
        return characteristic;
      }
    }
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Model model = (Model) o;
    if (name != null ? !name.equals(model.name) : model.name != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return name != null ? name.hashCode() : 0;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", id)
        .append("name", name)
        .toString();
  }

  public int compareTo(Model o) {
    return getName().compareTo(o.getName());
  }

}
