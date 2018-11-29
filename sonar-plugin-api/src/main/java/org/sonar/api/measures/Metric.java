/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.measures;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.scanner.ScannerSide;
import org.sonar.api.server.ServerSide;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Used to define a metric in a plugin. Should be used with {@link Metrics} extension point.
 */
@ScannerSide
@ServerSide
@ComputeEngineSide
public class Metric<G extends Serializable> implements Serializable, org.sonar.api.batch.measure.Metric<G> {

  /**
   * @since 5.3
   */
  public static final int DEFAULT_DECIMAL_SCALE = 1;

  /**
   * The maximum supported value of scale for decimal metrics
   *
   * @since 5.3
   */
  public static final int MAX_DECIMAL_SCALE = 5;

  /**
   * A metric bigger value means a degradation
   */
  public static final int DIRECTION_WORST = -1;
  /**
   * A metric bigger value means an improvement
   */
  public static final int DIRECTION_BETTER = 1;
  /**
   * The metric direction has no meaning
   */
  public static final int DIRECTION_NONE = 0;

  public enum ValueType {
    INT(Integer.class),
    FLOAT(Double.class),
    PERCENT(Double.class),
    BOOL(Boolean.class),
    STRING(String.class),
    MILLISEC(Long.class),
    DATA(String.class),
    LEVEL(Metric.Level.class),
    DISTRIB(String.class),
    RATING(Integer.class),
    WORK_DUR(Long.class);

    private final Class valueClass;

    ValueType(Class valueClass) {
      this.valueClass = valueClass;
    }

    private Class valueType() {
      return valueClass;
    }

    public static String[] names() {
      ValueType[] values = values();
      String[] names = new String[values.length];
      for (int i = 0; i < values.length; i += 1) {
        names[i] = values[i].name();
      }

      return names;
    }
  }

  public enum Level {
    OK("Green"),
    /**
     * @deprecated in 7.6.
     */
    @Deprecated
    WARN("Orange"),
    ERROR("Red");

    private static final List<String> NAMES = Arrays.stream(values())
      .map(Level::name)
      .collect(Collectors.toList());

    private String colorName;

    Level(String colorName) {
      this.colorName = colorName;
    }

    public String getColorName() {
      return colorName;
    }

    public static List<String> names() {
      return NAMES;
    }
  }

  private Integer id;
  private String key;
  private String description;
  private ValueType type;
  private Integer direction;
  private String domain;
  private String name;
  private Boolean qualitative = Boolean.FALSE;
  private Boolean userManaged = Boolean.FALSE;
  private Boolean enabled = Boolean.TRUE;
  private Double worstValue;
  private Double bestValue;
  private Boolean optimizedBestValue;
  private Boolean hidden = Boolean.FALSE;
  private Boolean deleteHistoricalData;
  private Integer decimalScale;

  private Metric(Builder builder) {
    this.key = builder.key;
    this.name = builder.name;
    this.description = builder.description;
    this.type = builder.type;
    this.direction = builder.direction;
    this.domain = builder.domain;
    this.qualitative = builder.qualitative;
    this.enabled = Boolean.TRUE;
    this.worstValue = builder.worstValue;
    this.optimizedBestValue = builder.optimizedBestValue;
    this.bestValue = builder.bestValue;
    this.hidden = builder.hidden;
    this.userManaged = builder.userManaged;
    this.deleteHistoricalData = builder.deleteHistoricalData;
    this.decimalScale = builder.decimalScale;
  }

  /**
   * Creates an empty metric
   *
   * @deprecated in 1.12. Use the {@link Builder} factory.
   */
  @Deprecated
  public Metric() {
  }

  /**
   * Creates a metric based on its key. Shortcut to Metric(key, ValueType.INT)
   *
   * @param key the metric key
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key) {
    this(key, ValueType.INT);
  }

  /**
   * Creates a metric based on a key and a type. Shortcut to
   * Metric(key, key, key, type, -1, Boolean.FALSE, null, false)
   *
   * @param key  the key
   * @param type the type
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key, ValueType type) {
    this(key, key, key, type, -1, Boolean.FALSE, null, false);
  }

  /**
   * @deprecated since 2.7 use the {@link Builder} factory.
   */
  @Deprecated
  public Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, String domain) {
    this(key, name, description, type, direction, qualitative, domain, false);
  }

  /**
   * Creates a fully qualified metric.
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
  private Metric(String key, String name, String description, ValueType type, Integer direction, Boolean qualitative, @Nullable String domain,
                 boolean userManaged) {
    this.key = key;
    this.description = description;
    this.type = type;
    this.direction = direction;
    this.domain = domain;
    this.name = name;
    this.qualitative = qualitative;
    this.userManaged = userManaged;
    if (ValueType.PERCENT == this.type) {
      this.bestValue = (direction == DIRECTION_BETTER) ? 100.0 : 0.0;
      this.worstValue = (direction == DIRECTION_BETTER) ? 0.0 : 100.0;
      this.decimalScale = DEFAULT_DECIMAL_SCALE;
    } else if (ValueType.FLOAT == this.type) {
      this.decimalScale = DEFAULT_DECIMAL_SCALE;
    }
  }

  /**
   * For internal use only
   */
  public Integer getId() {
    return id;
  }

  /**
   * For internal use only
   */
  public Metric<G> setId(@Nullable Integer id) {
    this.id = id;
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
  public Metric<G> setQualitative(Boolean qualitative) {
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
  public Metric<G> setKey(String key) {
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
  public Metric<G> setType(ValueType type) {
    this.type = type;
    return this;
  }

  /**
   * @return the metric description
   */
  @CheckForNull
  public String getDescription() {
    return description;
  }

  /**
   * Sets the metric description
   *
   * @param description the description
   * @return this
   */
  public Metric<G> setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * @return whether the metric is a managed by the users ("manual metric")
   */
  public Boolean getUserManaged() {
    return userManaged;
  }

  /**
   * Sets whether the metric is managed by users ("manual metric")
   *
   * @param userManaged whether the metric is user managed
   * @return this
   */
  public Metric<G> setUserManaged(Boolean userManaged) {
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
  public Metric<G> setEnabled(Boolean enabled) {
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
  public Metric<G> setDirection(Integer direction) {
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
  public Metric<G> setDomain(String domain) {
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
  public Metric<G> setName(String name) {
    this.name = name;
    return this;
  }

  public Double getWorstValue() {
    return worstValue;
  }

  @CheckForNull
  public Double getBestValue() {
    return bestValue;
  }

  /**
   * @return this
   */
  public Metric<G> setWorstValue(@Nullable Double d) {
    this.worstValue = d;
    return this;
  }

  /**
   * @param bestValue the best value. It can be null.
   * @return this
   */
  public Metric<G> setBestValue(@Nullable Double bestValue) {
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
      || ValueType.MILLISEC.equals(type)
      || ValueType.RATING.equals(type)
      || ValueType.WORK_DUR.equals(type);
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

  public Metric<G> setOptimizedBestValue(@Nullable Boolean b) {
    this.optimizedBestValue = b;
    return this;
  }

  /**
   * @return null for manual metrics
   */
  @CheckForNull
  public Boolean isOptimizedBestValue() {
    return optimizedBestValue;
  }

  public Boolean isHidden() {
    return hidden;
  }

  public Metric<G> setHidden(Boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  public Boolean getDeleteHistoricalData() {
    return deleteHistoricalData;
  }

  /**
   * Return the number scale if metric type is {@link ValueType#FLOAT}, else {@code null}
   *
   * @since 5.3
   */
  @CheckForNull
  public Integer getDecimalScale() {
    return decimalScale;
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
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  /**
   * Merge with fields from other metric. All fields are copied, except the id.
   *
   * @return this
   */
  public Metric<G> merge(final Metric with) {
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
    this.hidden = with.hidden;
    this.deleteHistoricalData = with.deleteHistoricalData;
    return this;
  }

  /**
   * Metric.Builder is used to create metric definitions. It must be preferred to creating new instances of the Metric class directly.
   *
   * @since 2.7
   */
  public static final class Builder {
    private String key;
    private Metric.ValueType type;
    private String name;
    private String description;
    private Integer direction = DIRECTION_NONE;
    private Boolean qualitative = Boolean.FALSE;
    private String domain = null;
    private Double worstValue;
    private Double bestValue;
    private boolean optimizedBestValue = false;
    private boolean hidden = false;
    private boolean userManaged = false;
    private boolean deleteHistoricalData = false;
    private Integer decimalScale = null;

    /**
     * Creates a new {@link Builder} object.
     *
     * @param key  the metric key, should be unique among all metrics
     * @param name the metric name
     * @param type the metric type
     */
    public Builder(String key, String name, ValueType type) {
      checkArgument(isNotBlank(key), "Metric key can not be blank");
      checkArgument(isNotBlank(name), "Name of metric %s must be set", key);
      checkArgument(type != null, "Type of metric %s must be set", key);
      this.key = key;
      this.name = name;
      this.type = type;
    }

    /**
     * Sets the metric description.
     *
     * @param d the description
     * @return the builder
     */
    public Builder setDescription(String d) {
      this.description = d;
      return this;
    }

    /**
     * Sets the metric direction (used for numeric values only), which is used in the Web UI to show if the trend of a metric is good or not.
     * <ul>
     * <li>Metric.DIRECTION_WORST: indicates that an increase of the metric value is not a good thing (example: the complexity of a function)</li>
     * <li>Metric.DIRECTION_BETTER: indicates that an increase of the metric value is a good thing (example: the code coverage of a function)</li>
     * <li>Metric.DIRECTION_NONE: indicates that the variation of the metric value is neither good nor bad (example: number of files).</li>
     * </ul>
     * Metric.DIRECTION_NONE is the default value.
     *
     * @param d the direction
     * @return the builder
     * @see Metric#DIRECTION_WORST
     * @see Metric#DIRECTION_BETTER
     * @see Metric#DIRECTION_NONE
     */
    public Builder setDirection(Integer d) {
      this.direction = d;
      return this;
    }

    /**
     * Sets whether the metric is qualitative or not. Default value is false.
     * <br>
     * If set to true, then variations of this metric will be highlighted in the Web UI (for instance, trend icons will be red or green instead of default grey).
     *
     * @param b Boolean.TRUE if the metric is qualitative
     * @return the builder
     */
    public Builder setQualitative(Boolean b) {
      this.qualitative = b;
      return this;
    }

    /**
     * Sets the domain for the metric (General, Complexity...). This is used to group metrics in the Web UI.
     * <br>
     * By default, the metric belongs to no specific domain.
     *
     * @param d the domain
     * @return the builder
     */
    public Builder setDomain(String d) {
      this.domain = d;
      return this;
    }

    /**
     * Sets the worst value that the metric can get (example: 0.0 for code coverage). No worst value is set by default.
     *
     * @param d the worst value
     * @return the builder
     */
    public Builder setWorstValue(Double d) {
      this.worstValue = d;
      return this;
    }

    /**
     * Sets the best value that the metric can get (example: 100.0 for code coverage). No best value is set by default.
     * <br>
     * Resources would be hidden on drilldown page, if the value of measure equals to best value.
     *
     * @param d the best value
     * @return the builder
     */
    public Builder setBestValue(Double d) {
      this.bestValue = d;
      return this;
    }

    /**
     * Specifies whether file-level measures that equal to the defined best value are stored or not. Default is false.
     * <br>
     * Example with the metric that stores the number of violation ({@link CoreMetrics#VIOLATIONS}):
     * if a file has no violation, then the value '0' won't be stored in the database.
     *
     * @param b true if the measures must not be stored when they equal to the best value
     * @return the builder
     */
    public Builder setOptimizedBestValue(boolean b) {
      this.optimizedBestValue = b;
      return this;
    }

    /**
     * Sets whether the metric should be hidden in Web UI (e.g. in Time Machine). Default is false.
     *
     * @param b true if the metric should be hidden.
     * @return the builder
     */
    public Builder setHidden(boolean b) {
      this.hidden = b;
      return this;
    }

    /**
     * Specifies whether this metric can be edited online in the "Manual measures" page. Default is false.
     *
     * @param b true if the metric can be edited online.
     * @return the builder
     * @since 2.10
     */
    public Builder setUserManaged(boolean b) {
      this.userManaged = b;
      return this;
    }

    /**
     * Specifies whether measures from the past can be automatically deleted to minimize database volume.
     * <br>
     * By default, historical data are kept.
     *
     * @param b true if measures from the past can be deleted automatically.
     * @return the builder
     * @since 2.14
     */
    public Builder setDeleteHistoricalData(boolean b) {
      this.deleteHistoricalData = b;
      return this;
    }

    /**
     * Scale to be used if the metric has decimal type ({@link ValueType#FLOAT} or {@link ValueType#PERCENT}).
     * Default is 1. It is not set (({@code null}) on non-decimal metrics.
     *
     * @since 5.3
     */
    public Builder setDecimalScale(int scale) {
      checkArgument(scale >= 0, "Scale of decimal metric %s must be positive: %d", key, scale);
      checkArgument(scale <= MAX_DECIMAL_SCALE, "Scale of decimal metric [%s] must be less than or equal %s: %s", key, MAX_DECIMAL_SCALE, scale);
      this.decimalScale = scale;
      return this;
    }

    /**
     * Creates a new metric definition based on the properties set on this metric builder.
     *
     * @return a new {@link Metric} object
     */
    public <G extends Serializable> Metric<G> create() {
      if (ValueType.PERCENT == this.type) {
        this.bestValue = (direction == DIRECTION_BETTER) ? 100.0 : 0.0;
        this.worstValue = (direction == DIRECTION_BETTER) ? 0.0 : 100.0;
        this.decimalScale = firstNonNull(decimalScale, DEFAULT_DECIMAL_SCALE);

      } else if (ValueType.FLOAT == this.type) {
        this.decimalScale = firstNonNull(decimalScale, DEFAULT_DECIMAL_SCALE);
      }
      return new Metric<>(this);
    }
  }

  @Override
  public String key() {
    return getKey();
  }

  @Override
  public Class<G> valueType() {
    return getType().valueType();
  }
}
