import React from 'react';

import { getPeriodLabel, getPeriodDate } from '../helpers/periods';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { formatMeasure } from '../../../helpers/measures';


const Measure = React.createClass({
  render() {
    if (this.props.value == null || isNaN(this.props.value)) {
      return null;
    }
    let formatted = formatMeasure(this.props.value, this.props.type);
    return <span>{formatted}</span>;
  }
});


export default React.createClass({
  render() {
    let metricName = window.t('metric', this.props.condition.metric.name, 'name');
    let threshold = this.props.condition.level === 'ERROR' ?
                    this.props.condition.error : this.props.condition.warning;
    let period = this.props.condition.period ?
                 getPeriodLabel(this.props.component.periods, this.props.condition.period) : null;
    let periodDate = getPeriodDate(this.props.component.periods, this.props.condition.period);

    let classes = 'alert_' + this.props.condition.level.toUpperCase();

    return (
        <li className="overview-gate-condition">
          <div className="little-spacer-bottom">{period}</div>

          <div style={{ display: 'flex', alignItems: 'center' }}>
            <div className="overview-gate-condition-value">
              <DrilldownLink component={this.props.component.key} metric={this.props.condition.metric.name}
                             period={this.props.condition.period} periodDate={periodDate}>
              <span className={classes}>
                <Measure value={this.props.condition.actual} type={this.props.condition.metric.type}/>
              </span>
              </DrilldownLink>&nbsp;
            </div>

            <div className="overview-gate-condition-metric">
              <div>{metricName}</div>
              <div>
                {window.t('quality_gates.operator', this.props.condition.op, 'short')}
                {' '}
                <Measure value={threshold} type={this.props.condition.metric.type}/>
              </div>
            </div>
          </div>
        </li>
    );
  }
});
