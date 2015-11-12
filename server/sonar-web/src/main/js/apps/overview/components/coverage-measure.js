import classNames from 'classnames';
import React from 'react';

import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { DonutChart } from '../../../components/charts/donut-chart';


export const CoverageMeasure = React.createClass({
  renderLeak () {
    if (this.props.leak == null) {
      return null;
    }
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">
        {formatMeasureVariation(this.props.leak, 'PERCENT')}
      </span>
    </div>;
  },

  renderDonut () {
    let donutData = [
      { value: this.props.measure, fill: '#85bb43' },
      { value: 100 - this.props.measure, fill: '#d4333f' }
    ];
    return <div className="overview-donut-chart">
      <DonutChart width="90" height="90" thickness="3" data={donutData}/>
      <div className="overview-detailed-measure-value">
        <DrilldownLink component={this.props.component.key} metric={this.props.metric} period={this.props.period}>
          {formatMeasure(this.props.measure, 'PERCENT')}
        </DrilldownLink>
      </div>
    </div>;
  },

  render () {
    if (this.props.measure == null) {
      return null;
    }

    let className = classNames('overview-detailed-measure', {
      'overview-leak': this.props.period
    });

    return <li className={className}>
      <div className="overview-detailed-measure-nutshell space-between">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        {this.renderDonut(this.props.measure)}
      </div>
      {this.renderLeak()}
    </li>;
  }
});
