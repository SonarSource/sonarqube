import React from 'react';

import { DomainLeakTitle } from '../main/components';


export const Legend = React.createClass({
  render() {
    return <div className="overview-legend overview-leak">
      <DomainLeakTitle label={this.props.leakPeriodLabel} date={this.props.leakPeriodDate}/>
    </div>;
  }
});
