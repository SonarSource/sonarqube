import $ from 'jquery';
import d3 from 'd3';
import React from 'react';

export class Bubble extends React.Component {
  handleClick () {
    if (this.props.link) {
      window.location = this.props.link;
    }
  }

  render () {
    let tooltipAttrs = {};
    if (this.props.tooltip) {
      tooltipAttrs = {
        'data-toggle': 'tooltip',
        'title': this.props.tooltip
      };
    }
    return <circle onClick={this.handleClick.bind(this)} className="bubble-chart-bubble"
                   r={this.props.r} {...tooltipAttrs}
                   transform={`translate(${this.props.x}, ${this.props.y})`}/>;
  }
}


export class BubbleChart extends React.Component {
  constructor (props) {
    super();
    this.state = { width: props.width, height: props.height };
  }

  componentDidMount () {
    if (!this.props.width || !this.props.height) {
      this.handleResize();
      window.addEventListener('resize', this.handleResize.bind(this));
    }
    this.initTooltips();
  }

  componentDidUpdate () {
    this.initTooltips();
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

  initTooltips () {
    $('[data-toggle="tooltip"]', React.findDOMNode(this))
        .tooltip({ container: 'body', placement: 'bottom', html: true });
  }

  getXRange (xScale, sizeScale, availableWidth) {
    var minX = d3.min(this.props.items, d => xScale(d.x) - sizeScale(d.size)),
        maxX = d3.max(this.props.items, d => xScale(d.x) + sizeScale(d.size)),
        dMinX = minX < 0 ? xScale.range()[0] - minX : xScale.range()[0],
        dMaxX = maxX > xScale.range()[1] ? maxX - xScale.range()[1] : 0;
    return [dMinX, availableWidth - dMaxX];
  }

  getYRange (yScale, sizeScale, availableHeight) {
    var minY = d3.min(this.props.items, d => yScale(d.y) - sizeScale(d.size)),
        maxY = d3.max(this.props.items, d => yScale(d.y) + sizeScale(d.size)),
        dMinY = minY < 0 ? yScale.range()[1] - minY : yScale.range()[1],
        dMaxY = maxY > yScale.range()[0] ? maxY - yScale.range()[0] : 0;
    return [availableHeight - dMaxY, dMinY];
  }

  renderXGrid (xScale, yScale) {
    if (!this.props.displayXGrid) {
      return null;
    }

    let lines = xScale.ticks().map((tick, index) => {
      let x = xScale(tick);
      let y1 = yScale.range()[0];
      let y2 = yScale.range()[1];

      // TODO extract styling
      return <line key={index} x1={x} x2={x} y1={y1} y2={y2}
                   shapeRendering="crispEdges" strokeWidth="0.3" stroke="#ccc"/>;
    });

    return <g ref="xGrid">{lines}</g>;
  }

  renderYGrid (xScale, yScale) {
    if (!this.props.displayYGrid) {
      return null;
    }

    let lines = yScale.ticks(5).map((tick, index) => {
      let y = yScale(tick);
      let x1 = xScale.range()[0];
      let x2 = xScale.range()[1];

      // TODO extract styling
      return <line key={index} x1={x1} x2={x2} y1={y} y2={y}
                   shapeRendering="crispEdges" strokeWidth="0.3" stroke="#ccc"/>;
    });

    return <g ref="yGrid">{lines}</g>;
  }

  renderXTicks (xScale, yScale) {
    if (!this.props.displayXTicks) {
      return null;
    }

    let ticks = xScale.ticks().map((tick, index) => {
      let x = xScale(tick);
      let y = yScale.range()[0];
      let text = this.props.formatXTick(tick);

      // TODO extract styling
      return <text key={index} className="bubble-chart-tick" x={x} y={y} dy="1.5em">{text}</text>;
    });

    return <g>{ticks}</g>;
  }

  renderYTicks (xScale, yScale) {
    if (!this.props.displayYTicks) {
      return null;
    }

    let ticks = yScale.ticks(5).map((tick, index) => {
      let x = xScale.range()[0];
      let y = yScale(tick);
      let text = this.props.formatYTick(tick);

      // TODO extract styling
      return <text key={index} className="bubble-chart-tick bubble-chart-tick-y"
                   x={x} y={y} dx="-0.5em" dy="0.3em">{text}</text>;
    });

    return <g>{ticks}</g>;
  }

  render () {
    if (!this.state.width || !this.state.height) {
      return <div/>;
    }

    let availableWidth = this.state.width - this.props.padding[1] - this.props.padding[3];
    let availableHeight = this.state.height - this.props.padding[0] - this.props.padding[2];

    let xScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.x)])
        .range([0, availableWidth])
        .nice();
    let yScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.y)])
        .range([availableHeight, 0])
        .nice();
    let sizeScale = d3.scale.linear()
        .domain([0, d3.max(this.props.items, d => d.size)])
        .range(this.props.sizeRange);

    xScale.range(this.getXRange(xScale, sizeScale, availableWidth));
    yScale.range(this.getYRange(yScale, sizeScale, availableHeight));

    let bubbles = this.props.items
        .map((item, index) => {
          let tooltip = index < this.props.tooltips.length ? this.props.tooltips[index] : null;
          return <Bubble key={index}
                         tooltip={tooltip}
                         link={item.link}
                         x={xScale(item.x)} y={yScale(item.y)} r={sizeScale(item.size)}/>;
        });

    return <svg className="bubble-chart" width={this.state.width} height={this.state.height}>
      <g transform={`translate(${this.props.padding[3]}, ${this.props.padding[0]})`}>
        {this.renderXGrid(xScale, yScale)}
        {this.renderXTicks(xScale, yScale)}
        {this.renderYGrid(xScale, yScale)}
        {this.renderYTicks(xScale, yScale)}
        {bubbles}
      </g>
    </svg>;
  }
}

BubbleChart.defaultProps = {
  sizeRange: [5, 45],
  displayXGrid: true,
  displayYGrid: true,
  displayXTicks: true,
  displayYTicks: true,
  tooltips: [],
  padding: [10, 10, 10, 10],
  formatXTick: d => d,
  formatYTick: d => d
};

BubbleChart.propTypes = {
  sizeRange: React.PropTypes.arrayOf(React.PropTypes.number),
  displayXGrid: React.PropTypes.bool,
  displayYGrid: React.PropTypes.bool,
  padding: React.PropTypes.arrayOf(React.PropTypes.number),
  formatXTick: React.PropTypes.func,
  formatYTick: React.PropTypes.func
};
