/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import partition from 'lodash/partition';
import sortBy from 'lodash/sortBy';

import MeasuresList from './MeasuresList';
import { domains } from '../config/domains';

function sortMeasures (measures, order) {
  const [known, unknown] = partition(measures, measure => order.includes(measure.metric.key));
  return [
    ...sortBy(known, measure => order.indexOf(measure.metric.key)),
    ...sortBy(unknown, measure => measure.metric.name)
  ];
}

function filterCoverageMeasures (measures) {
  const hasOverallCoverage = !!measures.find(measure => measure.metric.key === 'overall_coverage');
  const hasUTCoverage = !!measures.find(measure => measure.metric.key === 'coverage');
  const hasITCoverage = !!measures.find(measure => measure.metric.key === 'it_coverage');

  // display overall coverage only if all types of coverage exist
  const shouldShowOverallCoverage = hasOverallCoverage && hasUTCoverage && hasITCoverage;

  // skip if we should display overall coverage
  if (shouldShowOverallCoverage) {
    return measures;
  }

  // otherwise, hide all overall coverage measures
  return measures.filter(measure => {
    return measure.metric.key.indexOf('overall_') !== 0 &&
        measure.metric.key.indexOf('new_overall_') !== 0;
  });
}

function filterIssuesMeasures (measures) {
  const BANNED_MEASURES = [
    'blocker_violations',
    'new_blocker_violations',
    'critical_violations',
    'new_critical_violations',
    'major_violations',
    'new_major_violations',
    'minor_violations',
    'new_minor_violations',
    'info_violations',
    'new_info_violations'
  ];
  return measures.filter(measure => !BANNED_MEASURES.includes(measure.metric.key));
}

const HomeMeasuresList = ({ domain, component }) => {
  const { measures, name } = domain;
  const config = domains[name] || {};

  const filteredMeasures = filterCoverageMeasures(filterIssuesMeasures(measures));

  const configMain = config.main || [];
  const [mainMeasures, otherMeasures] = partition(filteredMeasures, measure => configMain.includes(measure.metric.key));

  const configOrder = config.order || [];
  const sortedMainMeasures = sortMeasures(mainMeasures, configOrder);
  const sortedOtherMeasures = sortMeasures(otherMeasures, configOrder);

  return (
      <div className="home-measures-list clearfix">
        {sortedMainMeasures.length > 0 && (
            <MeasuresList
                className="main-domain-measures"
                measures={sortedMainMeasures}
                component={component}
                spaces={[]}/>
        )}

        {sortedOtherMeasures.length > 0 && (
            <MeasuresList
                measures={sortedOtherMeasures}
                component={component}
                spaces={[]}/>
        )}
      </div>
  );
};

export default HomeMeasuresList;
