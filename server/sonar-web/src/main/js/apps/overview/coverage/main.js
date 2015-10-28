import React from 'react';

import { CoverageDetails } from './coverage-details';
import { TestsDetails } from './tests-details';
import { CoverageBubbleChart } from './bubble-chart';
import { CoverageTimeline } from './timeline';
import { CoverageTreemap } from './treemap';


export default class extends React.Component {
  render () {
    return <div className="overview-detailed-page">
      <div className="overview-domain-header">
        <h2 className="overview-title">Coverage & Tests</h2>
      </div>

      <a className="overview-detailed-page-back" href="#">
        <i className="icon-chevron-left"/>
      </a>

      <CoverageTimeline {...this.props}/>

      <div className="flex-columns">
        <div className="flex-column flex-column-half">
          <CoverageDetails {...this.props}/>
        </div>
        <div className="flex-column flex-column-half">
          <TestsDetails {...this.props}/>
        </div>
      </div>

      <CoverageBubbleChart {...this.props}/>
      <CoverageTreemap {...this.props}/>
    </div>;
  }
}
