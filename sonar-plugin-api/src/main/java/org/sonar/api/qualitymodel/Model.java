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

import com.google.common.collect.Lists;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rules.Rule;

import javax.persistence.*;
import java.util.List;

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
  private List<Characteristic> characteristics = Lists.newArrayList();

  /**
   * Use the factory method <code>Model</code>
   */
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

  public Characteristic addCharacteristic(Characteristic c) {
    c.setModel(this);
    c.setOrder(characteristics.size() + 1);
    characteristics.add(c);
    return c;
  }

  /**
   * @return enabled characteristics
   */
  public List<Characteristic> getCharacteristics() {
    return getCharacteristics(true);
  }

  public List<Characteristic> getCharacteristics(boolean onlyEnabled) {
    if (!onlyEnabled) {
      return characteristics;
    }
    List<Characteristic> result = Lists.newArrayList();
    for (Characteristic characteristic : characteristics) {
      if (characteristic.getEnabled()) {
        result.add(characteristic);
      }
    }
    return result;
  }

  /**
   * Search for an ENABLED characteristic by its key.
   */
  public Characteristic getCharacteristicByKey(String key) {
    for (Characteristic characteristic : characteristics) {
      if (characteristic.getEnabled() && StringUtils.equals(key, characteristic.getKey())) {
        return characteristic;
      }
    }
    return null;
  }

  /**
   * Search for an ENABLED characteristic with the specified rule.
   */
  public Characteristic getCharacteristicByRule(Rule rule) {
    if (rule != null) {
      for (Characteristic characteristic : characteristics) {
        if (characteristic.getEnabled() && ObjectUtils.equals(rule, characteristic.getRule())) {
          return characteristic;
        }
      }
    }
    return null;
  }

  /**
   * Search for ENABLED characteristics by their depth.
   */
  public List<Characteristic> getCharacteristicsByDepth(int depth) {
    List<Characteristic> result = Lists.newArrayList();
    for (Characteristic c : characteristics) {
      if (c.getEnabled() && c.getDepth() == depth) {
        result.add(c);
      }
    }
    return result;
  }

  /**
   * Search for an ENABLED characteristic by its name.
   */
  public Characteristic getCharacteristicByName(String name) {
    for (Characteristic characteristic : characteristics) {
      if (characteristic.getEnabled() && StringUtils.equals(name, characteristic.getName())) {
        return characteristic;
      }
    }
    return null;
  }

  public Model removeCharacteristic(Characteristic characteristic) {
    if (characteristic.getId() == null) {
      characteristics.remove(characteristic);
      for (Characteristic parent : characteristic.getParents()) {
        parent.removeChild(characteristic);
      }
    } else {
      characteristic.setEnabled(false);
    }
    for (Characteristic child : characteristic.getChildren()) {
      removeCharacteristic(child);
    }
    return this;
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
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("id", id)
        .append("name", name)
        .toString();
  }

  public int compareTo(Model o) {
    return getName().compareTo(o.getName());
  }

}
