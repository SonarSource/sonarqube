import React from 'react';

import { BarChart } from '../../../components/charts/bar-chart';
import { getMeasures } from '../../../api/measures';


const HEIGHT = 120;
const COMPLEXITY_DISTRIBUTION_METRIC = 'file_complexity_distribution';


export class ComplexityDistribution extends React.Component {
  constructor (props) {
    super(props);
    this.state = { loading: true };
  }

  componentDidMount () {
    this.requestData();
  }

  requestData () {
    return getMeasures(this.props.component.key, [COMPLEXITY_DISTRIBUTION_METRIC]).then(measures => {
      this.setState({ loading: false, distribution: measures[COMPLEXITY_DISTRIBUTION_METRIC] });
    });
  }

  renderLoading () {
    return <div className="overview-chart-placeholder" style={{ height: HEIGHT }}>
      <i className="spinner"/>
    </div>;
  }

  renderBarChart () {
    if (this.state.loading) {
      return this.renderLoading();
    }

    let data = this.state.distribution.split(';').map((point, index) => {
      let tokens = point.split('=');
      return { x: index, y: parseInt(tokens[1], 10), value: parseInt(tokens[0], 10) };
    });

    let xTicks = data.map(point => point.value);

    let xValues = data.map(point => window.formatMeasure(point.y, 'INT'));

    return <BarChart data={data}
                     xTicks={xTicks}
                     xValues={xValues}
                     height={HEIGHT}
                     padding={[25, 30, 50, 30]}/>;
  }

  render () {
    return <div className="overview-bar-chart">
      <div className="overview-domain-header">
        <h2 className="overview-title">&nbsp;</h2>
        <ul className="list-inline small">
          <li>X: Complexity/file</li>
          <li>Size: Number of Files</li>
        </ul>
      </div>
      <div>
        {this.renderBarChart()}
      </div>
    </div>;
  }
}
