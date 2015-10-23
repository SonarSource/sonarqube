import React from 'react';

import { DomainTimeline } from '../domain/timeline';
import { filterMetricsForDomains } from '../helpers/metrics';


const DOMAINS = ['Duplication'];


export class DuplicationsTimeline extends React.Component {
  render () {
    return <DomainTimeline {...this.props}
        initialMetric="duplicated_lines_density"
        metrics={filterMetricsForDomains(this.props.metrics, DOMAINS)}/>;
  }
}
