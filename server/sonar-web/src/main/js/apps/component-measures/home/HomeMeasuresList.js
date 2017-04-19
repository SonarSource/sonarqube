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
import { partition, sortBy } from 'lodash';
import MeasuresList from './MeasuresList';
import { domains } from '../config/domains';
import { getLocalizedMetricName } from '../../../helpers/l10n';

function sortMeasures(measures, order) {
  const [known, unknown] = partition(measures, measure => order.includes(measure.metric.key));
  return [
    ...sortBy(known, measure => order.indexOf(measure.metric.key)),
    ...sortBy(unknown, measure => getLocalizedMetricName(measure.metric))
  ];
}

function filterIssuesMeasures(measures) {
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

  const filteredMeasures = filterIssuesMeasures(measures);

  const configMain = config.main || [];
  const [mainMeasures, otherMeasures] = partition(filteredMeasures, measure =>
    configMain.includes(measure.metric.key)
  );

  const configOrder = config.order || [];
  const sortedMainMeasures = sortMeasures(mainMeasures, configOrder);
  const sortedOtherMeasures = sortMeasures(otherMeasures, configOrder);

  return (
    <div className="home-measures-list clearfix">
      {sortedMainMeasures.length > 0 &&
        <MeasuresList
          className="main-domain-measures"
          measures={sortedMainMeasures}
          component={component}
          spaces={[]}
        />}

      {sortedOtherMeasures.length > 0 &&
        <MeasuresList measures={sortedOtherMeasures} component={component} spaces={[]} />}
    </div>
  );
};

export default HomeMeasuresList;
