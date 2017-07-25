/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import React from 'react';
import { Link } from 'react-router';
import Measure from './../components/Measure';
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';
import HistoryIcon from '../../../components/icons-components/HistoryIcon';
import Tooltip from '../../../components/controls/Tooltip';
import { ComplexityDistribution } from '../../../components/shared/complexity-distribution';
import { isDiffMetric } from '../../../helpers/measures';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { getComponentMeasureHistory } from '../../../helpers/urls';
import { getLocalizedMetricName, translate } from '../../../helpers/l10n';

export default function MeasureDetailsHeader({
  component,
  measure,
  metric,
  secondaryMeasure,
  leakPeriod
}) {
  const isDiff = isDiffMetric(metric.key);
  return (
    <header className="measure-details-header">
      <h2 className="measure-details-metric">
        <IssueTypeIcon query={metric.key} className="little-spacer-right" />
        {getLocalizedMetricName(metric)}
        {!isDiff &&
          <Tooltip placement="right" overlay={translate('component_measures.show_metric_history')}>
            <Link
              className="js-show-history spacer-left button button-small button-compact"
              to={getComponentMeasureHistory(component.key, metric.key)}>
              <HistoryIcon />
            </Link>
          </Tooltip>}
      </h2>

      {isDiff &&
        <div className="pull-right">
          <LeakPeriodLegend component={component} period={leakPeriod} />
        </div>}

      <TooltipsContainer options={{ html: false }}>
        <div className="measure-details-value">
          {isDiff
            ? <div className="measure-details-value-leak">
                <Measure measure={measure} metric={metric} />
              </div>
            : <div className="measure-details-value-absolute">
                <Measure measure={measure} metric={metric} />
              </div>}

          {secondaryMeasure &&
            secondaryMeasure.metric === 'ncloc_language_distribution' &&
            <div className="measure-details-secondary">
              <LanguageDistribution distribution={secondaryMeasure.value} />
            </div>}

          {secondaryMeasure &&
            secondaryMeasure.metric === 'function_complexity_distribution' &&
            <div className="measure-details-secondary">
              <ComplexityDistribution distribution={secondaryMeasure.value} of="function" />
            </div>}

          {secondaryMeasure &&
            secondaryMeasure.metric === 'file_complexity_distribution' &&
            <div className="measure-details-secondary">
              <ComplexityDistribution distribution={secondaryMeasure.value} of="file" />
            </div>}
        </div>
      </TooltipsContainer>
    </header>
  );
}
