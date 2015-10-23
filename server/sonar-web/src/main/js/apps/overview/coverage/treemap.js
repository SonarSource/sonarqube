import d3 from 'd3';
import React from 'react';

import { DomainTreemap } from '../domain/treemap';


const COLORS_5 = ['#ee0000', '#f77700', '#ffee00', '#80cc00', '#00aa00'];


export class CoverageTreemap extends React.Component {
  render () {
    let scale = d3.scale.linear()
        .domain([0, 25, 50, 75, 100])
        .range(COLORS_5);
    return <DomainTreemap {...this.props}
        sizeMetric="ncloc"
        colorMetric="coverage"
        scale={scale}/>;
  }
}
