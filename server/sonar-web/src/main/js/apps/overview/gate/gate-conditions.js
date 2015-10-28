import React from 'react';
import GateCondition from './gate-condition';

export default React.createClass({
  propTypes: {
    gate: React.PropTypes.object.isRequired,
    component: React.PropTypes.object.isRequired
  },

  render() {
    let conditions = this.props.gate.conditions
        .filter(c => c.level !== 'OK')
        .map(c => <GateCondition key={c.metric.name} condition={c} component={this.props.component}/>);
    return <ul className="overview-gate-conditions-list">{conditions}</ul>;
  }
});
