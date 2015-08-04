import React from 'react';
import GateConditions from './gate-conditions';

export default React.createClass({
  render: function () {
    if (!this.props.gate || !this.props.gate.level) {
      return null;
    }

    const
        badgeClassName = 'badge badge-' + this.props.gate.level.toLowerCase(),
        badgeText = window.t('overview.gate', this.props.gate.level);

    return (
        <div className="overview-gate">
          <div className="overview-title">
            {window.t('overview.quality_gate')}
            <span className={badgeClassName}>{badgeText}</span>
          </div>
          <GateConditions gate={this.props.gate} component={this.props.component}/>
        </div>
    );
  }
});
