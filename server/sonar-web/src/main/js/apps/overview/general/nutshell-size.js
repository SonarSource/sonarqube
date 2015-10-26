import React from 'react';
import Card from './card';
import Measure from './../helpers/measure';
import DrilldownLink from './../helpers/drilldown-link';

export default React.createClass({
  render() {
    let
        lines = this.props.measures['lines'],
        files = this.props.measures['files'];

    let active = this.props.section === 'size';

    return (
        <Card linkTo="size" active={active} onRoute={this.props.onRoute}>
          <div className="measures">
            <div className="measure measure-big" data-metric="lines">
              <span className="measure-name">{window.t('overview.metric.lines')}</span>
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="lines">
                  <Measure value={lines} type="SHORT_INT"/>
                </DrilldownLink>
              </span>
            </div>
            <div className="measure measure-big" data-metric="files">
              <span className="measure-name">{window.t('overview.metric.files')}</span>
              <span className="measure-value">
                <DrilldownLink component={this.props.component.key} metric="files">
                  <Measure value={files} type="SHORT_INT"/>
                </DrilldownLink>
              </span>
            </div>
          </div>
        </Card>
    );
  }
});
