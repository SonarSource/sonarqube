import React from 'react';

import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import DrilldownLink from '../helpers/drilldown-link';
import { getShortType } from '../helpers/metrics';
import { DonutChart } from '../../../components/charts/donut-chart';


export const CoverageMeasure = React.createClass({
  renderLeakVariation () {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    let leak = this.props.leak[this.props.metric];
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">
        {formatMeasureVariation(leak, getShortType(this.props.type))}
      </span>
    </div>;
  },

  renderLeakValue () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    if (!this.props.leakMetric) {
      return <div className="overview-detailed-measure-leak">&nbsp;</div>;
    }

    let leak = this.props.leak[this.props.leakMetric];

    let donutData = [
      { value: leak, fill: '#85bb43' },
      { value: 100 - leak, fill: '#d4333f' }
    ];

    return <div className="overview-detailed-measure-leak">
      <div className="overview-donut-chart">
        <DonutChart width="20" height="20" thickness="3" data={donutData}/>
      </div>
      <span className="overview-detailed-measure-value">
        <DrilldownLink component={this.props.component.key} metric={this.props.leakMetric}
                       period={this.props.leakPeriodIndex}>
          {formatMeasure(leak, this.props.type)}
        </DrilldownLink>
      </span>
    </div>;
  },

  renderDonut (measure) {
    if (this.props.metric !== 'PERCENT') {
      return null;
    }

    let donutData = [
      { value: measure, fill: '#85bb43' },
      { value: 100 - measure, fill: '#d4333f' }
    ];
    return <div className="overview-donut-chart">
      <DonutChart width="20" height="20" thickness="3" data={donutData}/>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        {this.renderDonut(measure)}
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
      </div>
      {this.renderLeakValue()}
      {this.renderLeakVariation()}
    </div>;
  }
});
