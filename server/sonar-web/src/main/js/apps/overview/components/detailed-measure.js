import React from 'react';

import { formatMeasure, formatMeasureVariation, localizeMetric } from '../../../helpers/measures';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { getShortType } from '../helpers/metrics';


export const DetailedMeasure = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    let leak = this.props.leak[this.props.metric];
    let formatted = leak != null ? formatMeasureVariation(leak, getShortType(this.props.type)) : '—';
    return <div className="overview-detailed-measure-leak">
      <span className="overview-detailed-measure-value">{formatted}</span>
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
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});
