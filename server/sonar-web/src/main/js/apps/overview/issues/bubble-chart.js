import React from 'react';
import { DomainBubbleChart } from '../domain/bubble-chart';


export class IssuesBubbleChart extends React.Component {
  render () {
    return <DomainBubbleChart {...this.props}
        xMetric="violations"
        yMetric="sqale_index"
        sizeMetrics={['blocker_violations', 'critical_violations']}/>;
  }
}
