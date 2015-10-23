import React from 'react';

import { CoverageDetails } from './coverage-details';
import { TestsDetails } from './tests-details';
import { CoverageBubbleChart } from './bubble-chart';
import { CoverageTimeline } from './timeline';
import { CoverageTreemap } from './treemap';

import { getSeverities, getTags, getAssignees } from '../../../api/issues';


export default class CoverageDomain extends React.Component {
  render () {
    return <div className="overview-domain">

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
