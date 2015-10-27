import React from 'react';
import Card from './card';
import Measure from './../helpers/measure';
import DrilldownLink from './../helpers/drilldown-link';
import Donut from './../helpers/donut';

export default React.createClass({
  render() {
    let
        coverage = this.props.measures.coverage,
        tests = this.props.measures.tests,
        donutData = [
          { value: coverage, fill: '#85bb43' },
          { value: 100 - coverage, fill: '#d4333f' }
        ];

    if (coverage == null) {
      return null;
    }

    return (
        <Card>
          <div className="measures">
            <div className="measures-chart">
              <Donut data={donutData} size="47"/>
            </div>
            <div className="measure measure-big">
              <span className="measure-name">{window.t('overview.metric.coverage')}</span>
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="overall_coverage">
                  <Measure value={coverage} type="PERCENT"/>
                </DrilldownLink>
              </span>
            </div>
          </div>
          <ul className="list-inline big-spacer-top measures-chart-indent">
            <li>
              <DrilldownLink component={this.props.component.key} metric="tests">
                <Measure value={tests} type="SHORT_INT"/>
              </DrilldownLink>&nbsp;
              <span>{window.t('overview.metric.tests')}</span>
            </li>
          </ul>
        </Card>
    );
  }
});
