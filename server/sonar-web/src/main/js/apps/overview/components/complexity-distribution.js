import React from 'react';

import { BarChart } from '../../../components/charts/bar-chart';
import { formatMeasure } from '../../../helpers/measures';


const HEIGHT = 80;


export const ComplexityDistribution = React.createClass({
  propTypes: {
    distribution: React.PropTypes.string.isRequired,
    of: React.PropTypes.string.isRequired
  },

  renderBarChart () {
    let data = this.props.distribution.split(';').map((point, index) => {
      let tokens = point.split('=');
      let y = parseInt(tokens[1], 10);
      let value = parseInt(tokens[0], 10);
      return {
        x: index,
        y: y,
        value: value,
        tooltip: window.tp(`overview.complexity_tooltip.${this.props.of}`, y, value)
      };
    });

    let xTicks = data.map(point => point.value);

    let xValues = data.map(point => formatMeasure(point.y, 'INT'));

    return <BarChart data={data}
                     xTicks={xTicks}
                     xValues={xValues}
                     height={HEIGHT}
                     barsWidth={10}
                     padding={[25, 0, 25, 0]}/>;
  },

  render () {
    return <div className="overview-bar-chart">
      {this.renderBarChart()}
    </div>;
  }
});
