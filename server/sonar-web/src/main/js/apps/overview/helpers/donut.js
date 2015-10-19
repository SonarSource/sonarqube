import d3 from 'd3';
import React from 'react';

let Sector = React.createClass({
  render() {
    let arc = d3.svg.arc()
        .outerRadius(this.props.radius)
        .innerRadius(this.props.radius - this.props.thickness);
    return <path d={arc(this.props.data)} style={{ fill: this.props.fill }}/>;
  }
});

export default React.createClass({
  getDefaultProps() {
    return {
      size: 30,
      thickness: 6
    };
  },

  render() {
    let radius = this.props.size / 2;
    let pie = d3.layout.pie()
        .sort(null)
        .value(d => d.value);
    let data = this.props.data;
    let sectors = pie(data).map((d, i) => {
      return <Sector key={i} data={d} fill={data[i].fill} radius={radius} thickness={this.props.thickness}/>;
    });
    return <svg width={this.props.size} height={this.props.size}>
      <g transform={`translate(${radius}, ${radius})`}>{sectors}</g>
    </svg>;
  }
});
