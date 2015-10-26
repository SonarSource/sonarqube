import _ from 'underscore';
import moment from 'moment';
import React from 'react';

import Cards from './cards';
import LeakIssues from './leak-issues';
import LeakCoverage from './leak-coverage';
import LeakSize from './leak-size';
import LeakDups from './leak-dups';
import { periodLabel, getPeriodDate } from './../helpers/period-label';


export default React.createClass({
  render() {
    if (_.size(this.props.component.periods) < 1) {
      return null;
    }

    let period = periodLabel(this.props.component.periods, '1');
    let periodDate = getPeriodDate(this.props.component.periods, '1');

    return (
        <div className="overview-leak">
          <h2 className="overview-title">
            {window.t('overview.water_leak')}
            <span className="overview-leak-period">{period} / {moment(periodDate).format('LL')}</span>
          </h2>
          <Cards>
            <LeakIssues component={this.props.component} leak={this.props.leak} measures={this.props.measures}/>
            <LeakCoverage component={this.props.component} leak={this.props.leak}/>
            <LeakDups component={this.props.component} leak={this.props.leak}/>
            <LeakSize component={this.props.component} leak={this.props.leak}/>
          </Cards>
        </div>
    );
  }
});
