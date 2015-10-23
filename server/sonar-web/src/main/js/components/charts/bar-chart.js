import d3 from 'd3';
import React from 'react';

export class BarChart extends React.Component {
  constructor (props) {
    super();
    this.state = { width: props.width, height: props.height };
  }

  componentDidMount () {
    if (!this.props.width || !this.props.height) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize.bind(this));
    }
  }

  componentWillUnmount () {
    if (!this.props.width || !this.props.height) {
      window.removeEventListener('resize', this.handleResize.bind(this));
    }
  }

  handleResize () {
    let boundingClientRect = React.findDOMNode(this).parentNode.getBoundingClientRect();
    let newWidth = this.props.width || boundingClientRect.width;
    let newHeight = this.props.height || boundingClientRect.height;
    this.setState({ width: newWidth, height: newHeight });
  }

  renderXTicks (xScale, yScale) {
    if (!this.props.xTicks.length) {
      return null;
    }
    let ticks = this.props.xTicks.map((tick, index) => {
      let point = this.props.data[index];
      let x = Math.round(xScale(point.x) + xScale.rangeBand() / 2 + this.props.barsWidth / 2);
      let y = yScale.range()[0];
      return <text key={index} className="bar-chart-tick" x={x} y={y} dy="1.5em">{tick}</text>;
    });
    return <g>{ticks}</g>;
  }

  renderXValues (xScale, yScale) {
    if (!this.props.xValues.length) {
      return null;
    }
    let ticks = this.props.xValues.map((value, index) => {
      let point = this.props.data[index];
      let x = Math.round(xScale(point.x) + xScale.rangeBand() / 2 + this.props.barsWidth / 2);
      let y = yScale(point.y);
      return <text key={index} className="bar-chart-tick" x={x} y={y} dy="-1em">{value}</text>;
    });
    return <g>{ticks}</g>;
  }

  renderBars (xScale, yScale) {
    let bars = this.props.data.map((d, index) => {
      let x = Math.round(xScale(d.x) + xScale.rangeBand() / 2);
      let maxY = yScale.range()[0];
      let y = Math.round(yScale(d.y)) - /* minimum bar height */ 1;
      let height = maxY - y;
      return <rect key={index} className="bar-chart-bar"
                   x={x} y={y} width={this.props.barsWidth} height={height}/>;
    });
    return <g>{bars}</g>;
  }

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
}

BarChart.defaultProps = {
  xTicks: [],
  xValues: [],
  padding: [10, 10, 10, 10],
  barsWidth: 40
};

BarChart.propTypes = {
  data: React.PropTypes.arrayOf(React.PropTypes.object).isRequired,
  xTicks: React.PropTypes.arrayOf(React.PropTypes.any),
  xValues: React.PropTypes.arrayOf(React.PropTypes.any),
  padding: React.PropTypes.arrayOf(React.PropTypes.number),
  barsWidth: React.PropTypes.number
};
