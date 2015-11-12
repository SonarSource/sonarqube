import React from 'react';

import { DomainLeakTitle } from '../main/components';


export const Legend = React.createClass({
  render() {
    if (!this.props.leakPeriodDate) {
      return null;
    }
    return <div className="overview-legend">
      <span className="overview-legend-leak"/>
      <DomainLeakTitle label={this.props.leakPeriodLabel} date={this.props.leakPeriodDate}/>
    </div>;
  }
});
