import React from 'react';
import { DomainBubbleChart } from '../domain/bubble-chart';


export class DuplicationsBubbleChart extends React.Component {
  render () {
    return <DomainBubbleChart {...this.props}
        xMetric="ncloc"
        yMetric="duplicated_blocks"
        sizeMetrics={['duplicated_lines']}/>;
  }
}
