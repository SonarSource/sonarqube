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
package org.sonar.api.measures;

import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @since 1.10
 */
public final class MeasuresFilters {

  private MeasuresFilters() {
  }

  public static MeasuresFilter<Collection<Measure>> all() {
    return new MeasuresFilter<Collection<Measure>>() {
      @Override
      public Collection<Measure> filter(Collection<Measure> measures) {
        Collection<Measure> all = new ArrayList<Measure>();
        for (Measure measure : measures) {
          if (measure != null) {
            all.add(measure);
          }
        }
        return all;
      }
    };
  }

  public static MeasuresFilter<Measure> metric(final org.sonar.api.batch.measure.Metric<?> metric) {
    return metric(metric.key());
  }

  public static MeasuresFilter<Measure> metric(final String metricKey) {
    return new MetricFilter<Measure>(metricKey) {
      @Override
      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetricKey().equals(metricKey) &&
            measure.getCharacteristic() == null &&
            measure.getPersonId() == null) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  public static MeasuresFilter<Measure> characteristic(final Metric metric, final Characteristic characteristic) {
    return new MetricFilter<Measure>(metric) {

      @Override
      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetric().equals(metric) &&
            measure.getPersonId() == null &&
            isSameCharacteristic(measure, characteristic)) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  private static boolean isSameCharacteristic(Measure measure, final Characteristic characteristic) {
    Characteristic measureCharacteristic = measure.getCharacteristic();
    return measureCharacteristic != null &&
      measureCharacteristic.equals(characteristic);
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public static MeasuresFilter<Measure> requirement(final Metric metric, final Requirement requirement) {
    return new MetricFilter<Measure>(metric) {

      @Override
      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetric().equals(metric) &&
            measure.getPersonId() == null &&
            isSameRequirement(measure, requirement)) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  private static boolean isSameRequirement(Measure measure, final Requirement requirement) {
    Requirement measureRequirement = measure.getRequirement();
    return measureRequirement != null &&
      measureRequirement.equals(requirement);
  }

  /**
   * @since 2.0
   */
  public static MeasuresFilter<Measure> measure(final Measure measure) {
    return new MeasuresFilter<Measure>() {
      @Override
      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure m : measures) {
          if (m.equals(measure)) {
            return m;
          }
        }
        return null;
      }
    };
  }

  public static MeasuresFilter<RuleMeasure> rule(final Metric metric, final RuleKey ruleKey) {
    return new RuleFilter(metric, ruleKey);
  }

  public static MeasuresFilter<RuleMeasure> rule(final Metric metric, final Rule rule) {
    return rule(metric, rule.ruleKey());
  }

  public static MeasuresFilter<Collection<RuleMeasure>> rules(final Metric metric) {
    return new MetricFilter<Collection<RuleMeasure>>(metric) {

      private boolean apply(Measure measure) {
        return measure instanceof RuleMeasure && metric.equals(measure.getMetric())
          && measure.getPersonId() == null && ((RuleMeasure) measure).ruleKey() != null;
      }

      @Override
      public Collection<RuleMeasure> filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        List<RuleMeasure> result = new ArrayList<RuleMeasure>();
        for (Measure measure : measures) {
          if (apply(measure)) {
            result.add((RuleMeasure) measure);
          }
        }
        return result;
      }
    };
  }

  /**
   * Used for internal optimizations.
   */
  public abstract static class MetricFilter<M> implements MeasuresFilter<M> {
    private final String metricKey;

    protected MetricFilter(Metric metric) {
      this.metricKey = metric.getKey();
    }

    protected MetricFilter(String metricKey) {
      this.metricKey = metricKey;
    }

    public String filterOnMetricKey() {
      return metricKey;
    }
  }

  private abstract static class AbstractRuleMeasureFilter<M> extends MetricFilter<M> {
    protected AbstractRuleMeasureFilter(Metric metric) {
      super(metric);
    }

    private boolean apply(Measure measure) {
      return measure instanceof RuleMeasure
        && filterOnMetricKey().equals(measure.getMetricKey())
        && measure.getPersonId() == null
        && doApply((RuleMeasure) measure);
    }

    abstract boolean doApply(RuleMeasure ruleMeasure);

    @Override
    public M filter(Collection<Measure> measures) {
      if (measures == null) {
        return null;
      }
      for (Measure measure : measures) {
        if (apply(measure)) {
          return (M) measure;
        }
      }
      return null;
    }
  }

  private static class RuleFilter extends AbstractRuleMeasureFilter<RuleMeasure> {
    private RuleKey ruleKey;

    protected RuleFilter(Metric metric, RuleKey ruleKey) {
      super(metric);
      this.ruleKey = ruleKey;
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return measure.ruleKey() != null
        && ruleKey.equals(measure.ruleKey());
    }
  }
}
