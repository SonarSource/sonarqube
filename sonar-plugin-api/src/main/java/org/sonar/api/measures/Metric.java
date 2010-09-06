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
package org.sonar.api.measures;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.sonar.api.BatchExtension;
import org.sonar.api.ServerExtension;
import org.sonar.api.database.BaseIdentifiable;

import javax.persistence.*;

/**
 * @since 1.10
 */
@Table(name = "metrics")
@Entity(name = "Metric")
public class Metric extends BaseIdentifiable implements ServerExtension, BatchExtension {

  /**
   * A metric bigger value means a degradation
   */
  public final static int DIRECTION_WORST = -1;
  /**
   * A metric bigger value means an improvement
   */
  public final static int DIRECTION_BETTER = 1;
  /**
   * The metric direction has no meaning
   */
  public final static int DIRECTION_NONE = 0;

  public enum ValueType {
    INT, FLOAT, PERCENT, BOOL, STRING, MILLISEC, DATA, LEVEL, DISTRIB
  }

  public enum Level {
    OK("Green"), WARN("Orange"), ERROR("Red");

    private String colorName;

    Level(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }
  }

  public enum Origin {
    JAV, GUI, WS
  }

  @Transient
  private Formula formula;

  @Column(name = "name", updatable = false, nullable = false, length = 64)
  private String key;

  @Column(name = "description", updatable = true, nullable = true, length = 255)
  private String description;

  @Column(name = "val_type", updatable = true, nullable = true)
  @Enumerated(EnumType.STRING)
  private ValueType type;

  @Column(name = "direction", updatable = true, nullable = true)
  private Integer direction;

  @Column(name = "domain", updatable = true, nullable = true, length = 60)
  private String domain;

  @Column(name = "short_name", updatable = true, nullable = true, length = 64)
  private String name;

  @Column(name = "qualitative", updatable = true, nullable = true)
  private Boolean qualitative = Boolean.FALSE;

  @Column(name = "user_managed", updatable = true, nullable = true)
  private Boolean userManaged = Boolean.FALSE;

  @Column(name = "enabled", updatable = true, nullable = true)
  private Boolean enabled = Boolean.TRUE;

  @Column(name = "origin", updatable = true, nullable = true, length = 3)
  @Enumerated(EnumType.STRING)
  private Origin origin;

  @Column(name = "worst_value", updatable = true, nullable = true, precision = 30, scale = 20)
  private Double worstValue;

  @Column(name = "best_value", updatable = true, nullable = true, precision = 30, scale = 20)
  private Double bestValue;

  @Column(name = "optimized_best_value", updatable = true, nullable = true)
  private Boolean optimizedBestValue;

  @Column(name = "hidden", updatable = true, nullable = true)
  private Boolean hidden = Boolean.FALSE;


  /**
   * Creates an empty metric
   */
  @Deprecated public Metric() {
  }

  /**
   * Creates a metric based on its key. Shortcut to Metric(key, ValueType.INT)
   *
   * @param key the metric key
   */
  public Metric(String key) {
    this(key, ValueType.INT);
  }

  /**
   * Creates a metric based on a key and a type. Shortcut to
   * Metric(key, key, key, type, -1, Boolean.FALSE, null, false)
   *
   * @param key  the key
   * @param type the type
   */
  public Metric(String key, ValueType type) {
    this(key, key, key, type, -1, Boolean.FALSE, null, false);
  }

  public Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, String domain) {
    this(key, name, description, type, direction, qualitative, domain, false);
  }

  /**
   * Creates a fully qualified metric. This defaults some values:
   * <ul>
   * <li>origin : Origin.JAV</li>
   * </ul>
   *
   * @param key         the metric key
   * @param name        the metric name
   * @param description the metric description
   * @param type        the metric type
   * @param direction   the metric direction
   * @param qualitative whether the metric is qualitative
   * @param domain      the metric domain
   * @param userManaged whether the metric is user managed
   */
  @Deprecated
  public Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, String domain, boolean userManaged) {
    this.key = key;
    this.description = description;
    this.type = type;
    this.direction = direction;
    this.domain = domain;
    this.name = name;
    this.qualitative = qualitative;
    this.userManaged = userManaged;
    this.origin = Origin.JAV;
    if (ValueType.PERCENT.equals(this.type)) {
      this.bestValue = (direction==DIRECTION_BETTER ? 100.0 : 0.0);
      this.worstValue = (direction==DIRECTION_BETTER ? 0.0 : 100.0);
    }
  }

  /**
   * Creates a fully qualified metric. This defaults some values:
   * <ul>
   * <li>origin : Origin.JAV</li>
   * <li>enabled : true</li>
   * <li>userManaged : true</li>
   * </ul>
   *
   * @param key         the metric key
   * @param name        the metric name
   * @param type        the metric type
   * @param direction   the metric direction
   * @param qualitative whether the metric is qualitative
   * @param domain      the metric domain
   * @param formula     the metric formula
   */
  public Metric(String key, String name, ValueType type, Integer direction, Boolean qualitative, String domain, Formula formula) {
    this.key = key;
    this.name = name;
    this.type = type;
    this.direction = direction;
    this.domain = domain;
    this.qualitative = qualitative;
    this.origin = Origin.JAV;
    this.enabled = true;
    this.userManaged = false;
    this.formula = formula;
    if (ValueType.PERCENT.equals(this.type)) {
      this.bestValue = (direction==DIRECTION_BETTER ? 100.0 : 0.0);
      this.worstValue = (direction==DIRECTION_BETTER ? 0.0 : 100.0);
    }
  }

  /**
   * @return the metric formula
   */
  public Formula getFormula() {
    return formula;
  }

  /**
   * Sets the metric formula
   *
   * @param formula the formula
   * @return this
   */
  public Metric setFormula(Formula formula) {
    this.formula = formula;
    return this;
  }

  /**
   * @return wether the metric is qualitative
   */
  public Boolean getQualitative() {
    return qualitative;
  }

  /**
   * Sets whether the metric is qualitative
   *
   * @param qualitative whether the metric is qualitative
   * @return this
   */
  public Metric setQualitative(Boolean qualitative) {
    this.qualitative = qualitative;
    return this;
  }

  /**
   * @return the metric key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets the metric key
   *
   * @param key the key
   * @return this
   */
  public Metric setKey(String key) {
    this.key = key;
    return this;
  }

  /**
   * @return the metric type
   */
  public ValueType getType() {
    return type;
  }

  /**
   * Sets the metric type
   *
   * @param type the type
   * @return this
   */
  public Metric setType(ValueType type) {
    this.type = type;
    return this;
  }

  /**
   * @return the metric description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the metric description
   *
   * @param description the description
   * @return this
   */
  public Metric setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * @return whether the metric is a managed by the users (manual metric)
   */
  public Boolean getUserManaged() {
    return userManaged;
  }

  /**
   * Sets whether the metric is user managed
   *
   * @param userManaged whether the metric is user managed
   * @return this
   */
  public Metric setUserManaged(Boolean userManaged) {
    this.userManaged = userManaged;
    return this;
  }

  /**
   * @return whether the metric is enabled
   */
  public Boolean getEnabled() {
    return enabled;
  }

  /**
   * Sets whether the metric is enabled
   *
   * @param enabled whether the metric is enabled
   * @return this
   */
  public Metric setEnabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * @return the metric direction
   */
  public Integer getDirection() {
    return direction;
  }

  /**
   * Sets the metric direction.
   *
   * @param direction the direction
   */
  public Metric setDirection(Integer direction) {
    this.direction = direction;
    return this;
  }

  /**
   * @return the domain of the metric
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Sets the domain for the metric (General, Complexity...)
   *
   * @param domain the domain
   * @return this
   */
  public Metric setDomain(String domain) {
    this.domain = domain;
    return this;
  }

  /**
   * @return the metric name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the metric name
   *
   * @param name the name
   * @return this
   */
  public Metric setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @return the origin of the metric - Internal use only
   */
  public Origin getOrigin() {
    return origin;
  }

  /**
   * Set the origin of the metric - Internal use only
   *
   * @param origin the origin
   * @return this
   */
  public Metric setOrigin(Origin origin) {
    this.origin = origin;
    return this;
  }

  public Double getWorstValue() {
    return worstValue;
  }

  public Double getBestValue() {
    return bestValue;
  }

  /**
   * @return this
   */
  public Metric setWorstValue(Double d) {
    this.worstValue = d;
    return this;
  }

  /**
   * @param bestValue the best value. It can be null.
   * @return this
   */
  public Metric setBestValue(Double bestValue) {
    this.bestValue = bestValue;
    return this;
  }

  /**
   * @return whether the metric is of a numeric type (int, percentage...)
   */
  public boolean isNumericType() {
    return ValueType.INT.equals(type)
        || ValueType.FLOAT.equals(type)
        || ValueType.PERCENT.equals(type)
        || ValueType.BOOL.equals(type)
        || ValueType.MILLISEC.equals(type);
  }

  /**
   * @return whether the metric is of type data
   */
  public boolean isDataType() {
    return ValueType.DATA.equals(type) || ValueType.DISTRIB.equals(type);
  }

  /**
   * @return whether the metric is of type percentage
   */
  public boolean isPercentageType() {
    return ValueType.PERCENT.equals(type);
  }

  public Metric setOptimizedBestValue(Boolean b) {
    this.optimizedBestValue = b;
    return this;
  }

  public Boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  public Boolean isHidden() {
    return hidden;
  }

  public Metric setHidden(Boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Metric)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    Metric other = (Metric) obj;
    return key.equals(other.getKey());
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("key", key)
        .append("name", name)
        .append("type", type)
        .append("enabled", enabled)
        .append("qualitative", qualitative)
        .append("direction", direction)
        .append("domain", domain)
        .append("worstValue", worstValue)
        .append("bestValue", bestValue)
        .append("optimizedBestValue", optimizedBestValue)
        .append("hidden", hidden)
        .toString();
  }

  /**
   * Merge with fields from other metric. All fields are copied, except the id.
   * @return this
   */
  public Metric merge(final Metric with) {
    this.description = with.description;
    this.domain = with.domain;
    this.enabled = with.enabled;
    this.qualitative = with.qualitative;
    this.worstValue = with.worstValue;
    this.bestValue = with.bestValue;
    this.optimizedBestValue = with.optimizedBestValue;
    this.direction = with.direction;
    this.key = with.key;
    this.type = with.type;
    this.name = with.name;
    this.userManaged = with.userManaged;
    this.origin = with.origin;
    this.hidden = with.hidden;
    return this;
  }
}
