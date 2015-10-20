import React from 'react';
import Card from './card';
import Measure from './../helpers/measure';
import MeasureVariation from './../helpers/measure-variation';
import DrilldownLink from './../helpers/drilldown-link';
import Donut from './../helpers/donut';

export default React.createClass({
  render() {
    let
        newCoverage = parseInt(this.props.leak.newCoverage, 10),
        tests = this.props.leak.tests,
        donutData = [
          { value: newCoverage, fill: '#85bb43' },
          { value: 100 - newCoverage, fill: '#d4333f' }
        ];

    if (newCoverage == null || isNaN(newCoverage)) {
      return null;
    }

    return (
        <Card>
          <div className="measures">
            <div className="measures-chart">
              <Donut data={donutData} size="47"/>
            </div>
            <div className="measure measure-big" data-metric="new_coverage">
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="new_coverage" period="3">
                  <Measure value={newCoverage} type="PERCENT"/>
                </DrilldownLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.new_coverage')}</span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top measures-chart-indent">
            <li>
              <span><MeasureVariation value={tests} type="SHORT_INT"/></span>&nbsp;
              <span>{window.t('overview.metric.tests')}</span>
            </li>
          </ul>
        </Card>
    );
  }
});
