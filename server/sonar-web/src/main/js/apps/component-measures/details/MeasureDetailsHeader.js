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
import Measure from './../components/Measure';
import LanguageDistribution from '../../../components/charts/LanguageDistribution';
import LeakPeriodLegend from '../components/LeakPeriodLegend';
import { ComplexityDistribution } from '../../../components/shared/complexity-distribution';
import { isDiffMetric } from '../utils';
import { TooltipsContainer } from '../../../components/mixins/tooltips-mixin';
import { getLocalizedMetricName } from '../../../helpers/l10n';
import IssueTypeIcon from '../../../components/ui/IssueTypeIcon';

export default function MeasureDetailsHeader({ measure, metric, secondaryMeasure, leakPeriod }) {
  return (
    <header className="measure-details-header">
      <h2 className="measure-details-metric">
        <IssueTypeIcon query={metric.key} className="little-spacer-right" />
        {getLocalizedMetricName(metric)}
      </h2>

      {isDiffMetric(metric) &&
        <div className="pull-right">
          <LeakPeriodLegend period={leakPeriod} />
        </div>}

      <TooltipsContainer options={{ html: false }}>
        <div className="measure-details-value">

          {isDiffMetric(metric)
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
