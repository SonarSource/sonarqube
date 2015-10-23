import d3 from 'd3';
import React from 'react';

import { DomainTreemap } from '../domain/treemap';


const COLORS_5 = ['#00aa00', '#80cc00', '#ffee00', '#f77700', '#ee0000'];


export class IssuesTreemap extends React.Component {
  render () {
    let scale = d3.scale.ordinal()
        .domain([1, 2, 3, 4, 5])
        .range(COLORS_5);
    return <DomainTreemap {...this.props}
        sizeMetric="ncloc"
        colorMetric="sqale_rating"
        scale={scale}/>;
  }
}
