/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;

/**
 * @since 1.10
 * @deprecated since 5.6. Sensor should only save measures and not read them.
 */
@Deprecated
public final class MeasuresFilters {

  private MeasuresFilters() {
  }

  public static MeasuresFilter<Collection<Measure>> all() {
    return new MeasuresFilter<Collection<Measure>>() {
      @Override
      public Collection<Measure> filter(Collection<Measure> measures) {
        Collection<Measure> all = new ArrayList<>();
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
      public Measure filter(@Nullable Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetricKey().equals(metricKey)) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  /**
   * @since 2.0
   */
  public static MeasuresFilter<Measure> measure(final Measure measure) {
    return new MeasuresFilter<Measure>() {
      @Override
      public Measure filter(@Nullable Collection<Measure> measures) {
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
          && ((RuleMeasure) measure).ruleKey() != null;
      }

      @Override
      public Collection<RuleMeasure> filter(@Nullable Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        List<RuleMeasure> result = new ArrayList<>();
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

  /**
   * @deprecated since 5.2. The measures related to rules are computed on server side by Compute Engine.
   */
  @Deprecated
  private abstract static class AbstractRuleMeasureFilter<M> extends MetricFilter<M> {
    protected AbstractRuleMeasureFilter(Metric metric) {
      super(metric);
    }

    private boolean apply(Measure measure) {
      return measure instanceof RuleMeasure
        && filterOnMetricKey().equals(measure.getMetricKey())
        && doApply((RuleMeasure) measure);
    }

    abstract boolean doApply(RuleMeasure ruleMeasure);

    @Override
    public M filter(@Nullable Collection<Measure> measures) {
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

  /**
   * @deprecated since 5.2. Useless by design because of Compute Engine
   */
  @Deprecated
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
