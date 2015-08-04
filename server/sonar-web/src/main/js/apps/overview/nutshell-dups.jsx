import React from 'react';
import Card from './card';
import Measure from './helpers/measure';
import DrilldownLink from './helpers/drilldown-link';
import Donut from './helpers/donut';

export default React.createClass({
  render: function () {
    const
        density = this.props.measures.duplications,
        lines = this.props.measures.duplicatedLines,
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
            <div className="measure measure-big">
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="duplicated_lines_density">
                  <Measure value={density} type="PERCENT"/>
                </DrilldownLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.duplications')}</span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top measures-chart-indent">
            <li>
              <DrilldownLink component={this.props.component.key} metric="duplicated_lines">
                <Measure value={lines} type="SHORT_INT"/>
              </DrilldownLink>&nbsp;
              <span>{window.t('overview.metric.duplicated_lines')}</span>
            </li>
          </ul>
        </Card>
    );
  }
});
