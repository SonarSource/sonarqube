import React from 'react';

import { DomainTimeline } from '../domain/timeline';
import { filterMetricsForDomains } from '../helpers/metrics';


const DOMAINS = ['Size', 'Complexity', 'Documentation'];


export class SizeTimeline extends React.Component {
  render () {
    return <DomainTimeline {...this.props}
        initialMetric="ncloc"
        metrics={filterMetricsForDomains(this.props.metrics, DOMAINS)}/>;
  }
}
