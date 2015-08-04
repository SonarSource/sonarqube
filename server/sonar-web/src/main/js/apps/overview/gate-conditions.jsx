import React from 'react';
import Cards from './cards';
import Card from './card';
import GateCondition from './gate-condition';

export default React.createClass({
  render() {
    const conditions = this.props.gate.conditions
        .filter((c) => {
          return c.level !== 'OK';
        })
        .map((c) => {
          return (
              <Card key={c.metric.name}>
                <GateCondition condition={c} component={this.props.component}/>
              </Card>
          );
        });
    return <Cards>{conditions}</Cards>;
  }
});
