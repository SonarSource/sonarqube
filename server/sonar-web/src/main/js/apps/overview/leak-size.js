import React from 'react';
import Card from './card';
import MeasureVariation from './helpers/measure-variation';

export default React.createClass({
  render() {
    let
        lines = this.props.leak.lines,
        files = this.props.leak.files;

    return (
        <Card>
          <div className="measures">
            <div className="measure measure-big" data-metric="lines">
              <span className="measure-value">
                <MeasureVariation value={lines} type="SHORT_INT"/>
              </span>
              <span className="measure-name">{window.t('overview.metric.lines')}</span>
            </div>
            <div className="measure measure-big" data-metric="files">
              <span className="measure-value">
                <MeasureVariation value={files} type="SHORT_INT"/>
              </span>
              <span className="measure-name">{window.t('overview.metric.files')}</span>
            </div>
          </div>
        </Card>
    );
  }
});
