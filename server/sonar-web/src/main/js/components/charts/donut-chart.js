import d3 from 'd3';
import React from 'react';

import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';


const Sector = React.createClass({
  render() {
    let arc = d3.svg.arc()
        .outerRadius(this.props.radius)
        .innerRadius(this.props.radius - this.props.thickness);
    return <path d={arc(this.props.data)} style={{ fill: this.props.fill }}/>;
  }
});


export const DonutChart = React.createClass({
  propTypes: {
    data: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return { thickness: 6, padding: [0, 0, 0, 0] };
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    let availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    let availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    let size = Math.min(availableWidth, availableHeight);
    let radius = Math.floor(size / 2);

    let pie = d3.layout.pie()
        .sort(null)
        .value(d => d.value);
    let sectors = pie(this.props.data).map((d, i) => {
      return <Sector key={i}
                     data={d}
                     radius={radius}
                     fill={this.props.data[i].fill}
                     thickness={this.props.thickness}/>;
    });

    return <svg className="donut-chart" width={this.state.width} height={this.state.height}>
      <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
        <g transform={`translate(${radius}, ${radius})`}>
          {sectors}
        </g>
      </g>
    </svg>;
  }
});
