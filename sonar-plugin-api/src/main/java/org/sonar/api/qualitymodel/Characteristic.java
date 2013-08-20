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
package org.sonar.api.qualitymodel;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;
import org.sonar.api.rules.Rule;

import javax.persistence.*;

import java.util.Collections;
import java.util.List;

/**
 * @since 2.3
 */
@Entity
@Table(name = "characteristics")
public final class Characteristic implements Comparable<Characteristic> {

  public static final int ROOT_DEPTH = 1;

  @Id
  @Column(name = "id")
  @GeneratedValue
  private Integer id;

  @Column(name = "kee", nullable = true, length = 100)
  private String key;

  @Column(name = "name", nullable = true, length = 100)
  private String name;

  @Column(name = "depth")
  private int depth = ROOT_DEPTH;

  @Column(name = "characteristic_order")
  private int order = 0;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "quality_model_id")
  private Model model;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "rule_id")
  private Rule rule;

  @Column(name = "description", nullable = true, length = 4000)
  private String description;

  @Column(name = "enabled", updatable = true, nullable = true)
  private Boolean enabled = Boolean.TRUE;

  @ManyToMany
  @JoinTable(
      name = "characteristic_edges",
      joinColumns = @JoinColumn(name = "child_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "parent_id",
          referencedColumnName = "id")
  )
  private List<Characteristic> parents = Lists.newArrayList();

  @Sort(type = SortType.NATURAL)
  @ManyToMany(mappedBy = "parents", cascade = CascadeType.ALL)
  private List<Characteristic> children = Lists.newArrayList();

  @OneToMany(mappedBy = "characteristic", fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REMOVE})
  private List<CharacteristicProperty> properties = Lists.newArrayList();

  Characteristic() {
  }

  public Integer getId() {
    return id;
  }

  Characteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getKey() {
    return key;
  }

  public Characteristic setKey(String s) {
    this.key = StringUtils.trimToNull(s);
    return this;
  }

  public String getName() {
    return name;
  }

  public Characteristic setName(String s) {
    return setName(s, false);
  }

  public Characteristic setName(String s, boolean asKey) {
    this.name = StringUtils.trimToNull(s);
    if (asKey) {
      this.key = StringUtils.upperCase(this.name);
      this.key = StringUtils.replaceChars(this.key, ' ', '_');
    }
    return this;
  }

  public Model getModel() {
    return model;
  }

  Characteristic setModel(Model model) {
    this.model = model;
    return this;
  }

  public Rule getRule() {
    return rule;
  }

  public boolean hasRule() {
    return rule != null;
  }

  public Characteristic setRule(Rule r) {
    this.rule = r;
    return this;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Characteristic setEnabled(Boolean b) {
    this.enabled = b;
    return this;
  }

  public Characteristic addChildren(Characteristic... list) {
    if (list != null) {
      for (Characteristic c : list) {
        addChild(c);
      }
    }
    return this;
  }

  public Characteristic addChild(Characteristic child) {
    propagateDepth(child, depth + 1);
    child.addParents(this);
    child.setModel(model);
    children.add(child);
    return this;
  }

  Characteristic removeChild(Characteristic child) {
    children.remove(child);
    return this;
  }

  private static void propagateDepth(Characteristic characteristic, int depth) {
    characteristic.setDepth(depth);
    for (Characteristic child : characteristic.getChildren()) {
      propagateDepth(child, depth + 1);
    }
  }

  Characteristic addParents(Characteristic... pc) {
    if (pc != null) {
      Collections.addAll(this.parents, pc);
    }
    return this;
  }

  public List<Characteristic> getParents() {
    return parents;
  }

  public Characteristic getParent(String name) {
    for (Characteristic parent : parents) {
      if (StringUtils.equals(parent.getName(), name)) {
        return parent;
      }
    }
    return null;
  }

  /**
   * Enabled children sorted by insertion order
   */
  public List<Characteristic> getChildren() {
    return getChildren(true);
  }

  /**
   * Enabled children sorted by insertion order
   */
  public List<Characteristic> getChildren(boolean onlyEnabled) {
    if (onlyEnabled) {
      return children;
    }
    List<Characteristic> result = Lists.newArrayList();
    for (Characteristic child : children) {
      if (child.getEnabled()) {
        result.add(child);
      }
    }
    return result;
  }

  public Characteristic getChild(String name) {
    for (Characteristic child : children) {
      if (StringUtils.equals(child.getName(), name)) {
        return child;
      }
    }
    return null;
  }

  public int getDepth() {
    return depth;
  }

  public boolean isRoot() {
    return depth == ROOT_DEPTH;
  }

  Characteristic setDepth(int i) {
    this.depth = i;
    return this;
  }

  public int getOrder() {
    return order;
  }

  Characteristic setOrder(int i) {
    this.order = i;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Characteristic setDescription(String s) {
    this.description = s;
    return this;
  }

  public CharacteristicProperty setProperty(String key, String value) {
    return addProperty(CharacteristicProperty.create(key).setTextValue(value));
  }

  public CharacteristicProperty setProperty(String key, Double value) {
    return addProperty(CharacteristicProperty.create(key).setValue(value));
  }

  public CharacteristicProperty addProperty(CharacteristicProperty property) {
    property.setCharacteristic(this);
    properties.add(property);
    return property;
  }

  public CharacteristicProperty getProperty(String key) {
    for (CharacteristicProperty property : properties) {
      if (StringUtils.equals(key, property.getKey())) {
        return property;
      }
    }
    return null;
  }

  public String getPropertyTextValue(String key, String defaultValue) {
    CharacteristicProperty property = getProperty(key);
    String value = property != null ? property.getTextValue() : null;
    return StringUtils.defaultIfEmpty(value, defaultValue);
  }

  public Double getPropertyValue(String key, Double defaultValue) {
    CharacteristicProperty property = getProperty(key);
    Double value = property != null ? property.getValue() : null;
    return value == null ? defaultValue : value;
  }

  public List<CharacteristicProperty> getProperties() {
    return properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Characteristic that = (Characteristic) o;
    if (key != null ? !key.equals(that.key) : that.key != null) {
      return false;
    }
    if (rule != null ? !rule.equals(that.rule) : that.rule != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = key != null ? key.hashCode() : 0;
    result = 31 * result + (rule != null ? rule.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("key", key)
        .append("name", name)
        .append("rule", rule)
        .append("description", description)
        .toString();
  }

  public int compareTo(Characteristic o) {
    if (equals(o)) {
      return 0;
    }
    return order - o.order;
  }

  public static Characteristic create() {
    return new Characteristic();
  }

  public static Characteristic createByName(String name) {
    return new Characteristic().setName(name, true);
  }

  public static Characteristic createByKey(String key, String name) {
    return new Characteristic().setKey(key).setName(name, false);
  }

  public static Characteristic createByRule(Rule rule) {
    return new Characteristic().setRule(rule);
  }
}
