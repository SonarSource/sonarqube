import React from 'react';

const Sector = React.createClass({
  render() {
    const arc = d3.svg.arc()
        .outerRadius(this.props.radius)
        .innerRadius(this.props.radius - this.props.thickness);
    return <path d={arc(this.props.data)} style={{ fill: this.props.fill }}></path>;
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
    const radius = this.props.size / 2;
    const pie = d3.layout.pie().sort(null)
        .value(d => {
          return d.value
        });
    const data = this.props.data;
    const sectors = pie(data).map((d, i) => {
      return <Sector
          key={i}
          data={d}
          fill={data[i].fill}
          radius={radius}
          thickness={this.props.thickness}/>;
    });
    return (
        <svg width={this.props.size} height={this.props.size}>
          <g transform={`translate(${radius}, ${radius})`}>{sectors}</g>
        </svg>
    );
  }
});
