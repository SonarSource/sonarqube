import d3 from 'd3';
import React from 'react';

import { ResizeMixin } from './../mixins/resize-mixin';
import { TooltipsMixin } from './../mixins/tooltips-mixin';


export const LineChart = React.createClass({
  propTypes: {
    data: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
    xTicks: React.PropTypes.arrayOf(React.PropTypes.any),
    xValues: React.PropTypes.arrayOf(React.PropTypes.any),
    padding: React.PropTypes.arrayOf(React.PropTypes.number),
    backdropConstraints: React.PropTypes.arrayOf(React.PropTypes.number),
    displayBackdrop: React.PropTypes.bool,
    displayPoints: React.PropTypes.bool,
    displayVerticalGrid: React.PropTypes.bool,
    height: React.PropTypes.number,
    interpolate: React.PropTypes.string
  },

  mixins: [ResizeMixin, TooltipsMixin],

  getDefaultProps() {
    return {
      displayBackdrop: true,
      displayPoints: true,
      displayVerticalGrid: true,
      xTicks: [],
      xValues: [],
      padding: [10, 10, 10, 10],
      interpolate: 'basis'
    };
  },

  getInitialState() {
    return { width: this.props.width, height: this.props.height };
  },

  renderBackdrop (xScale, yScale) {
    if (!this.props.displayBackdrop) {
      return null;
    }

    let area = d3.svg.area()
                 .x(d => xScale(d.x))
                 .y0(yScale.range()[0])
                 .y1(d => yScale(d.y))
                 .interpolate(this.props.interpolate);

    let data = this.props.data;
    if (this.props.backdropConstraints) {
      let c = this.props.backdropConstraints;
      data = data.filter(d => c[0] <= d.x && d.x <= c[1]);
    }
    return <path className="line-chart-backdrop" d={area(data)}/>;
  },

  renderPoints (xScale, yScale) {
    if (!this.props.displayPoints) {
      return null;
    }
    let points = this.props.data.map((point, index) => {
      let x = xScale(point.x);
      let y = yScale(point.y);
      return <circle key={index}
                     className="line-chart-point"
                     r="3"
                     cx={x}
                     cy={y}/>;
    });
    return <g>{points}</g>;
  },

  renderVerticalGrid (xScale, yScale) {
    if (!this.props.displayVerticalGrid) {
      return null;
    }
    let lines = this.props.data.map((point, index) => {
      let x = xScale(point.x);
      let y1 = yScale.range()[0];
      let y2 = yScale(point.y);
      return <line key={index}
                   className="line-chart-grid"
                   x1={x}
                   x2={x}
                   y1={y1}
                   y2={y2}/>;
    });
    return <g>{lines}</g>;
  },

  renderXTicks (xScale, yScale) {
    if (!this.props.xTicks.length) {
      return null;
    }
    let ticks = this.props.xTicks.map((tick, index) => {
      let point = this.props.data[index];
      let x = xScale(point.x);
      let y = yScale.range()[0];
      return <text key={index}
                   className="line-chart-tick"
                   x={x}
                   y={y}
                   dy="1.5em">{tick}</text>;
    });
    return <g>{ticks}</g>;
  },

  renderXValues (xScale, yScale) {
    if (!this.props.xValues.length) {
      return null;
    }
    let ticks = this.props.xValues.map((value, index) => {
      let point = this.props.data[index];
      let x = xScale(point.x);
      let y = yScale(point.y);
      return <text key={index}
                   className="line-chart-tick"
                   x={x}
                   y={y}
                   dy="-1em">{value}</text>;
    });
    return <g>{ticks}</g>;
  },

  renderLine (xScale, yScale) {
    let p = d3.svg.line()
                 .x(d => xScale(d.x))
                 .y(d => yScale(d.y))
                 .interpolate(this.props.interpolate);

    return <path className="line-chart-path" d={p(this.props.data)}/>;
  },

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    let availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    let availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    let maxY;
    let xScale = d3.scale.linear()
                   .domain(d3.extent(this.props.data, d => d.x))
                   .range([0, availableWidth]);
    let yScale = d3.scale.linear()
                   .range([availableHeight, 0]);

    if (this.props.domain) {
      maxY = this.props.domain[1];
      yScale.domain(this.props.domain);
    } else {
      maxY = d3.max(this.props.data, d => d.y);
      yScale.domain([0, maxY]);
    }

    return <svg className="line-chart" width={this.state.width} height={this.state.height}>
      <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
        {this.renderVerticalGrid(xScale, yScale, maxY)}
        {this.renderBackdrop(xScale, yScale)}
        {this.renderLine(xScale, yScale)}
        {this.renderPoints(xScale, yScale)}
        {this.renderXTicks(xScale, yScale)}
        {this.renderXValues(xScale, yScale)}
      </g>
    </svg>;
  }
});
