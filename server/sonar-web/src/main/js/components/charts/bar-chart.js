import d3 from 'd3';
import React from 'react';

import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';

export const BarChart = React.createClass({
  propTypes: {
    data: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    xTicks: React.PropTypes.arrayOf(React.PropTypes.any),
    xValues: React.PropTypes.arrayOf(React.PropTypes.any),
    height: React.PropTypes.number,
    padding: React.PropTypes.arrayOf(React.PropTypes.number),
    barsWidth: React.PropTypes.number
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return {
      xTicks: [],
      xValues: [],
      padding: [10, 10, 10, 10],
      barsWidth: 40
    };
  },

  getInitialState () {
    return { width: this.props.width, height: this.props.height };
  },

  renderXTicks (xScale, yScale) {
    if (!this.props.xTicks.length) {
      return null;
    }
    let ticks = this.props.xTicks.map((tick, index) => {
      let point = this.props.data[index];
      let x = Math.round(xScale(point.x) + xScale.rangeBand() / 2 + this.props.barsWidth / 2);
      let y = yScale.range()[0];
      let d = this.props.data[index];
      let tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return <text key={index}
                   className="bar-chart-tick"
                   x={x}
                   y={y}
                   dy="1.5em"
                   {...tooltipAtts}>{tick}</text>;
    });
    return <g>{ticks}</g>;
  },

  renderXValues (xScale, yScale) {
    if (!this.props.xValues.length) {
      return null;
    }
    let ticks = this.props.xValues.map((value, index) => {
      let point = this.props.data[index];
      let x = Math.round(xScale(point.x) + xScale.rangeBand() / 2 + this.props.barsWidth / 2);
      let y = yScale(point.y);
      let d = this.props.data[index];
      let tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return <text key={index}
                   className="bar-chart-tick"
                   x={x}
                   y={y}
                   dy="-1em"
                   {...tooltipAtts}>{value}</text>;
    });
    return <g>{ticks}</g>;
  },

  renderBars (xScale, yScale) {
    let bars = this.props.data.map((d, index) => {
      let x = Math.round(xScale(d.x) + xScale.rangeBand() / 2);
      let maxY = yScale.range()[0];
      let y = Math.round(yScale(d.y)) - /* minimum bar height */ 1;
      let height = maxY - y;
      let tooltipAtts = {};
      if (d.tooltip) {
        tooltipAtts['title'] = d.tooltip;
        tooltipAtts['data-toggle'] = 'tooltip';
      }
      return <rect key={index}
                   className="bar-chart-bar"
                   {...tooltipAtts}
                   x={x}
                   y={y}
                   width={this.props.barsWidth}
                   height={height}/>;
    });
    return <g>{bars}</g>;
  },

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    let availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    let availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    let maxY = d3.max(this.props.data, d => d.y);
    let xScale = d3.scale.ordinal()
                   .domain(this.props.data.map(d => d.x))
                   .rangeRoundBands([0, availableWidth]);
    let yScale = d3.scale.linear()
                   .domain([0, maxY])
                   .range([availableHeight, 0]);

    return <svg className="bar-chart" width={this.state.width} height={this.state.height}>
      <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
        {this.renderXTicks(xScale, yScale)}
        {this.renderXValues(xScale, yScale)}
        {this.renderBars(xScale, yScale)}
      </g>
    </svg>;
  }
});
