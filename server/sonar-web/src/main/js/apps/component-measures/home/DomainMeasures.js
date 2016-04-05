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
import MeasureBubbleChartContainer from '../components/bubbleChart/MeasureBubbleChartContainer';
import { getLeakPeriodLabel } from '../../../helpers/periods';
import { hasBubbleChart } from '../utils';
import { domains as domainsConf } from '../config/domains';

const sortMeasures = (measures, order) => {
  const [known, unknown] = partition(measures, measure => order.includes(measure.metric.key));
  return [
    ...sortBy(known, measure => order.indexOf(measure.metric.key)),
    ...sortBy(unknown, measure => measure.metric.name)
  ];
};

const filterCoverageMeasures = measures => {
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
};

export default class DomainMeasures extends React.Component {
  render () {
    const { component, domains, periods } = this.props;
    const { domainName } = this.props.params;
    const domain = domains.find(d => d.name === domainName);
    const { measures } = domain;
    const leakPeriodLabel = getLeakPeriodLabel(periods);

    const filteredMeasures = filterCoverageMeasures(measures);
    const conf = domainsConf[domainName];
    const order = conf ? conf.order : [];
    const spaces = conf && conf.spaces ? conf.spaces : [];
    const sortedMeasures = sortMeasures(filteredMeasures, order);

    return (
        <section id="component-measures-domain">
          <MeasuresList
              measures={sortedMeasures}
              component={component}
              spaces={spaces}
              hasLeak={leakPeriodLabel != null}/>

          {hasBubbleChart(domainName) && (
              <MeasureBubbleChartContainer domainName={domainName}/>
          )}
        </section>
    );
  }
}
