import React from 'react';
import { DomainBubbleChart } from '../domain/bubble-chart';


export class CoverageBubbleChart extends React.Component {
  render () {
    return <DomainBubbleChart {...this.props}
        xMetric="complexity"
        yMetric="coverage"
        sizeMetrics={['sqale_index']}/>;
  }
}
