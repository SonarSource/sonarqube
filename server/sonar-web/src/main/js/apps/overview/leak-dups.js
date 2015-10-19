import React from 'react';
import Card from './card';
import MeasureVariation from './helpers/measure-variation';
import Donut from './helpers/donut';

export default React.createClass({
  render() {
    let
        density = this.props.leak.duplications,
        lines = this.props.leak.duplicatedLines,
        donutData = [
          { value: density, fill: '#f3ca8e' },
          { value: 100 - density, fill: '#e6e6e6' }
        ];

    if (density == null) {
      return null;
    }

    return (
        <Card>
          <div className="measures">
            <div className="measures-chart">
              <Donut data={donutData} size="47"/>
            </div>
            <div className="measure measure-big" data-metric="duplicated_lines_density">
              <span className="measure-value">
                <MeasureVariation value={density} type="PERCENT"/>
              </span>
              <span className="measure-name">{window.t('overview.metric.duplications')}</span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top measures-chart-indent">
            <li>
              <span><MeasureVariation value={lines} type="SHORT_INT"/></span>&nbsp;
              <span>{window.t('overview.metric.duplicated_lines')}</span>
            </li>
          </ul>
        </Card>
    );
  }
});
