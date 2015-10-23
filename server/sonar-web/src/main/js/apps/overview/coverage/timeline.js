import React from 'react';

import { DomainTimeline } from '../domain/timeline';
import { filterMetricsForDomains } from '../helpers/metrics';


const DOMAINS = [
  'Tests',
  'Tests (Integration)',
  'Tests (Overall)'
];


export class CoverageTimeline extends React.Component {
  render () {
    return <DomainTimeline {...this.props}
        initialMetric="coverage"
        metrics={filterMetricsForDomains(this.props.metrics, DOMAINS)}/>;
  }
}
