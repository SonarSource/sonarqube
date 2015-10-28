import React from 'react';

import { DuplicationsDetails } from './duplications-details';
import { DuplicationsBubbleChart } from './bubble-chart';
import { DuplicationsTimeline } from './timeline';
import { DuplicationsTreemap } from './treemap';


export default class extends React.Component {
  render () {
    return <div className="overview-detailed-page">
      <div className="overview-domain-header">
        <h2 className="overview-title">Duplications</h2>
      </div>

      <a className="overview-detailed-page-back" href="#">
        <i className="icon-chevron-left"/>
      </a>

      <DuplicationsTimeline {...this.props}/>
      <div className="flex-columns">
        <div className="flex-column flex-column-half">
          <DuplicationsDetails {...this.props}/>
        </div>
      </div>

      <DuplicationsBubbleChart {...this.props}/>
      <DuplicationsTreemap {...this.props}/>
    </div>;
  }
}
