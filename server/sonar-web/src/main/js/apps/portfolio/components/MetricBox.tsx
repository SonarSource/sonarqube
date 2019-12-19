/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import * as React from 'react';
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import Level from 'sonar-ui-common/components/ui/Level';
import { translate } from 'sonar-ui-common/helpers/l10n';
import ActivityLink from '../../../components/common/ActivityLink';
import MeasuresLink from '../../../components/common/MeasuresLink';
import Measure from '../../../components/measure/Measure';
import { getComponentDrilldownUrl } from '../../../helpers/urls';
import { METRICS_PER_TYPE } from '../utils';
import Effort from './Effort';
import MainRating from './MainRating';
import RatingFreshness from './RatingFreshness';

export interface MetricBoxProps {
  component: string;
  measures: T.Dict<string | undefined>;
  metricKey: string;
}

export default function MetricBox({ component, measures, metricKey }: MetricBoxProps) {
  const keys = METRICS_PER_TYPE[metricKey];
  const rating = measures[keys.rating];
  const lastReliabilityChange = measures[keys.last_change];
  const rawEffort = measures[keys.effort];
  const effort = rawEffort ? JSON.parse(rawEffort) : undefined;

  return (
    <div className="portfolio-box">
      <h2 className="portfolio-box-title">
        {translate(keys.label)}
        <HelpTooltip
          className="little-spacer-left"
          overlay={translate('portfolio.metric_domain', metricKey, 'help')}
        />
      </h2>

      {rating ? (
        <MainRating component={component} metric={keys.rating} value={rating} />
      ) : (
        <div className="portfolio-box-rating">
          <span className="rating no-rating">—</span>
        </div>
      )}

      {rating && (
        <>
          <h3>{translate('portfolio.metric_trend')}</h3>
          <RatingFreshness lastChange={lastReliabilityChange} rating={rating} />
        </>
      )}

      {metricKey === 'releasability'
        ? Number(effort) > 0 && (
            <>
              <h3>{translate('portfolio.lowest_rated_projects')}</h3>
              <div className="portfolio-effort">
                <Link
                  to={getComponentDrilldownUrl({
                    componentKey: component,
                    metric: 'alert_status'
                  })}>
                  <span>
                    <Measure
                      className="little-spacer-right"
                      metricKey="projects"
                      metricType="SHORT_INT"
                      value={effort}
                    />
                    {Number(effort) === 1
                      ? translate('project_singular')
                      : translate('project_plural')}
                  </span>
                </Link>
                <Level
                  aria-label={
                    Number(effort) === 1
                      ? translate('portfolio.has_qg_status')
                      : translate('portfolio.have_qg_status')
                  }
                  className="little-spacer-left"
                  level="ERROR"
                  small={true}
                />
              </div>
            </>
          )
        : effort && (
            <>
              <h3>{translate('portfolio.lowest_rated_projects')}</h3>
              <Effort component={component} effort={effort} metricKey={keys.rating} />
            </>
          )}

      <div className="portfolio-box-links">
        <div>
          <MeasuresLink component={component} metric={keys.measuresMetric} />
        </div>
        <div>
          <ActivityLink component={component} metric={keys.activity || keys.rating} />
        </div>
      </div>
    </div>
  );
}
