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
package org.sonar.api.measures;

import org.sonar.api.rules.Rule;
import org.sonar.api.technicaldebt.batch.Characteristic;
import org.sonar.api.technicaldebt.batch.Requirement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @since 1.10
 */
public final class MeasuresFilters {

  private MeasuresFilters() {
  }

  public static MeasuresFilter<Collection<Measure>> all() {
    return new MeasuresFilter<Collection<Measure>>() {
      public Collection<Measure> filter(Collection<Measure> measures) {
        return measures;
      }
    };
  }

  public static MeasuresFilter<Measure> metric(final Metric metric) {
    return metric(metric.getKey());
  }

  public static MeasuresFilter<Measure> metric(final String metricKey) {
    return new MetricFilter<Measure>(metricKey) {

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

      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          Characteristic measureCharacteristic = measure.getCharacteristic();
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetric().equals(metric) &&
            measure.getPersonId() == null &&
            measureCharacteristic != null &&
            measureCharacteristic.equals(characteristic)) {
            return measure;
          }
        }
        return null;
      }
    };
  }

  public static MeasuresFilter<Measure> requirement(final Metric metric, final Requirement requirement) {
    return new MetricFilter<Measure>(metric) {

      public Measure filter(Collection<Measure> measures) {
        if (measures == null) {
          return null;
        }
        for (Measure measure : measures) {
          if (measure.getClass().equals(Measure.class) &&
            measure.getMetric().equals(metric) &&
            measure.getPersonId() == null &&
            measure.getRequirement() != null &&
            measure.getRequirement().equals(requirement)) {
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

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public static MeasuresFilter<RuleMeasure> ruleCategory(final Metric metric, final Integer category) {
    return new RuleCategoryFilter(metric, category);
  }

  public static MeasuresFilter<RuleMeasure> rule(final Metric metric, final Rule rule) {
    return new RuleFilter(metric, rule);
  }

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  public static MeasuresFilter<Collection<RuleMeasure>> ruleCategories(final Metric metric) {
    return new MetricFilter<Collection<RuleMeasure>>(metric) {

      public Collection<RuleMeasure> filter(Collection<Measure> measures) {
        return Collections.emptyList();
      }
    };
  }

  public static MeasuresFilter<Collection<RuleMeasure>> rules(final Metric metric) {
    return new MetricFilter<Collection<RuleMeasure>>(metric) {

      private boolean apply(Measure measure) {
        return measure instanceof RuleMeasure && metric.equals(measure.getMetric())
          && measure.getPersonId() == null && ((RuleMeasure) measure).getRule() != null;
      }

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

  /**
   * @deprecated since 2.5. See http://jira.codehaus.org/browse/SONAR-2007
   */
  @Deprecated
  private static class RuleCategoryFilter extends AbstractRuleMeasureFilter<RuleMeasure> {

    protected RuleCategoryFilter(Metric metric, Integer categ) {
      super(metric);
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return false;
    }
  }


  private static class RuleFilter extends AbstractRuleMeasureFilter<RuleMeasure> {
    private Rule rule;

    protected RuleFilter(Metric metric, Rule rule) {
      super(metric);
      this.rule = rule;
    }

    @Override
    boolean doApply(RuleMeasure measure) {
      return measure.getRule() != null
        && rule.equals(measure.getRule());
    }
  }
}
