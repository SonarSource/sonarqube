import React from 'react';

import Measure from './../helpers/measure';
import { periodLabel, getPeriodDate } from './../helpers/period-label';
import DrilldownLink from './../helpers/drilldown-link';


export default React.createClass({
  render() {
    let metricName = window.t('metric', this.props.condition.metric.name, 'name'),
        threshold = this.props.condition.level === 'ERROR' ?
                    this.props.condition.error : this.props.condition.warning,
        period = this.props.condition.period ?
                 `(${periodLabel(this.props.component.periods, this.props.condition.period)})` : null,
        periodDate = getPeriodDate(this.props.component.periods, this.props.condition.period);

    let classes = 'alert_' + this.props.condition.level.toUpperCase();

    return (
        <div>
          <h4 className="overview-gate-condition-metric">{metricName}<br/><span className="nowrap">{period}</span></h4>
          <div className="overview-gate-condition-value">
            <DrilldownLink component={this.props.component.key} metric={this.props.condition.metric.name}
                           period={this.props.condition.period} periodDate={periodDate}>
              <span className={classes}>
                <Measure value={this.props.condition.actual} type={this.props.condition.metric.type}/>
              </span>
            </DrilldownLink>&nbsp;
            <span className="overview-gate-condition-itself">
              {window.t('quality_gates.operator', this.props.condition.op, 'short')}&nbsp;
              <Measure value={threshold} type={this.props.condition.metric.type}/>
            </span>
          </div>
        </div>
    );
  }
});
