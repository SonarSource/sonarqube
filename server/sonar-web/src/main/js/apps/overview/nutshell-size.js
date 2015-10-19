import React from 'react';
import Card from './card';
import Measure from './helpers/measure';
import DrilldownLink from './helpers/drilldown-link';

export default React.createClass({
  render() {
    let
        lines = this.props.measures['lines'],
        files = this.props.measures['files'];

    return (
        <Card>
          <div className="measures">
            <div className="measure measure-big" data-metric="lines">
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="lines">
                  <Measure value={lines} type="SHORT_INT"/>
                </DrilldownLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.lines')}</span>
            </div>
            <div className="measure measure-big" data-metric="files">
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="files">
                  <Measure value={files} type="SHORT_INT"/>
                </DrilldownLink>
              </span>
              <span className="measure-name">{window.t('overview.metric.files')}</span>
            </div>
          </div>
        </Card>
    );
  }
});
