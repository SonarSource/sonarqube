import React from 'react';

import { DuplicationsDetails } from './duplications-details';
import { DuplicationsBubbleChart } from './bubble-chart';
import { DuplicationsTimeline } from './timeline';
import { DuplicationsTreemap } from './treemap';

import { getSeverities, getTags, getAssignees } from '../../../api/issues';


export default class DuplicationsDomain extends React.Component {
  render () {
    return <div className="overview-domain">
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
