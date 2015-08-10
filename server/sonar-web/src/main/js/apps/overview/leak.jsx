import React from 'react';
import Cards from './cards';
import LeakIssues from './leak-issues';
import LeakCoverage from './leak-coverage';
import LeakSize from './leak-size';
import LeakDups from './leak-dups';
import {periodLabel} from './helpers/period-label';

export default React.createClass({
  render() {
    if (_.size(this.props.component.periods) < 3) {
      return null;
    }

    const period = periodLabel(this.props.component.periods, '3');

    return (
        <div className="overview-leak">
          <div className="overview-title">
            {window.t('overview.water_leak')}
            <span className="overview-leak-period">{period}</span>
          </div>
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
