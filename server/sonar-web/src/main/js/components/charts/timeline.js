import d3 from 'd3';
import React from 'react';

export class Timeline extends React.Component {
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

  renderBackdrop (xScale, yScale, maxY) {
    if (!this.props.displayBackdrop) {
      return null;
    }

    let area = d3.svg.area()
        .x(d => xScale(d.date))
        .y0(maxY)
        .y1(d => yScale(d.value))
        .interpolate(this.props.interpolate);

    // TODO extract styling
    return <path d={area(this.props.snapshots)} fill="#4b9fd5" fillOpacity="0.2"/>;
  }

  renderLine (xScale, yScale) {
    let path = d3.svg.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.value))
        .interpolate(this.props.interpolate);

    // TODO extract styling
    return <path d={path(this.props.snapshots)} stroke="#4b9fd5" strokeWidth={this.props.lineWidth} fill="none"/>;
  }

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    let maxY = d3.max(this.props.snapshots, d => d.value);
    let xScale = d3.time.scale()
        .domain(d3.extent(this.props.snapshots, d => d.date))
        .range([0, this.state.width - this.props.lineWidth]);
    let yScale = d3.scale.linear()
        .domain([0, maxY])
        .range([this.state.height, 0]);

    return <svg width={this.state.width} height={this.state.height}>
      <g transform={`translate(${this.props.lineWidth / 2}, ${this.props.lineWidth / 2})`}>
        {this.renderBackdrop(xScale, yScale, maxY)}
        {this.renderLine(xScale, yScale)}
      </g>
    </svg>;
  }
}

Timeline.defaultProps = {
  lineWidth: 2,
  displayBackdrop: true,
  interpolate: 'basis'
};

Timeline.propTypes = {
  snapshots: React.PropTypes.arrayOf(React.PropTypes.object).isRequired
};
