import d3 from 'd3';
import React from 'react';

import { DomainTreemap } from '../domain/treemap';


const COLORS_5 = ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000'];


export class DuplicationsTreemap extends React.Component {
  render () {
    let scale = d3.scale.linear()
        .domain([0, 25, 50, 75, 100])
        .range(COLORS_5);
    return <DomainTreemap {...this.props}
        sizeMetric="ncloc"
        colorMetric="duplicated_lines_density"
        scale={scale}/>;
  }
}
