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
import sortBy from '../../../../../../node_modules/lodash/sortBy';
import partition from '../../../../../../node_modules/lodash/partition';
import React from 'react';

import MeasuresList from './MeasuresList';
import { domains } from '../config/domains';

const sortMeasures = (measures, order) => {
  const [known, unknown] = partition(measures, measure => order.includes(measure.metric.key));
  return [
    ...sortBy(known, measure => order.indexOf(measure.metric.key)),
    ...sortBy(unknown, measure => measure.metric.name)
  ];
};

export default class AllMeasuresDomain extends React.Component {
  render () {
    const { domain, component, leakPeriodLabel, displayHeader } = this.props;

    const hasLeak = !!leakPeriodLabel;
    const { measures } = domain;
    const domainConfig = domains[domain.name] || { main: [], order: [] };
    const mainMetrics = domainConfig.main;
    const orderedMeasures = domainConfig.order;
    const [mainMeasures, otherMeasures] = partition(measures,
        measure => mainMetrics.indexOf(measure.metric.key) !== -1);
    const sortedMainMeasures = sortMeasures(mainMeasures, orderedMeasures);
    const sortedOtherMeasures = sortMeasures(otherMeasures, orderedMeasures);
    const finalMeasures = [...sortedMainMeasures, ...sortedOtherMeasures];

    return (
        <li>
          {displayHeader && (
              <header className="page-header">
                <h3 className="page-title">{domain.name}</h3>
              </header>
          )}

          <MeasuresList
              measures={finalMeasures}
              hasLeak={hasLeak}
              component={component}/>
        </li>
    );
  }
}

AllMeasuresDomain.defaultProps = {
  displayHeader: true
};

AllMeasuresDomain.propTypes = {
  displayHeader: React.PropTypes.bool
};

