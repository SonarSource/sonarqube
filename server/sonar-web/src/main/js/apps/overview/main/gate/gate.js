import React from 'react';

import GateConditions from './gate-conditions';
import GateEmpty from './gate-empty';


export default React.createClass({
  render() {
    if (!this.props.gate || !this.props.gate.level) {
      return this.props.component.qualifier === 'TRK' ? <GateEmpty/> : null;
    }

    let level = this.props.gate.level.toLowerCase(),
        badgeClassName = 'badge badge-' + level,
        badgeText = window.t('overview.gate', this.props.gate.level);

    return (
        <div className="overview-gate">
          <h2 className="overview-title">
            {window.t('overview.quality_gate')}
            <span className={badgeClassName}>{badgeText}</span>
          </h2>
          <GateConditions gate={this.props.gate} component={this.props.component}/>
        </div>
    );
  }
});
