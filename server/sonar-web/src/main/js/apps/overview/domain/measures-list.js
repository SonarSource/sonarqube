import _ from 'underscore';
import React from 'react';

import DrilldownLink from '../helpers/drilldown-link';
import { getMeasures } from '../../../api/measures';


export class DomainMeasuresList extends React.Component {
  constructor () {
    super();
    this.state = { measures: {} };
  }

  componentDidMount () {
    this.requestDetails();
  }

  requestDetails () {
    return getMeasures(this.props.component.key, this.props.metricsToDisplay).then(measures => {
      this.setState({ measures });
    });
  }

  getMetricObject (metricKey) {
    return _.findWhere(this.props.metrics, { key: metricKey });
  }

  renderValue (value, metricKey, metricType) {
    if (value != null) {
      return <DrilldownLink component={this.props.component.key} metric={metricKey}>
        {window.formatMeasure(value, metricType)}
      </DrilldownLink>;
    } else {
      return '—';
    }
  }

  render () {
    let rows = this.props.metricsToDisplay.map(metric => {
      let metricObject = this.getMetricObject(metric);
      return <tr key={metric}>
        <td>{metricObject.name}</td>
        <td className="thin nowrap text-right">
          {this.renderValue(this.state.measures[metric], metric, metricObject.type)}
        </td>
      </tr>;
    });
    return <table className="data zebra">
      <tbody>{rows}</tbody>
    </table>;
  }
}

DomainMeasuresList.propTypes = {
  metricsToDisplay: React.PropTypes.arrayOf(React.PropTypes.string).isRequired
};
