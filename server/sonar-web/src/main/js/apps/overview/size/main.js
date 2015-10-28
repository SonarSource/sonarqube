import React from 'react';

import { SizeTimeline } from './timeline';
import { SizeDetails } from './size-details';
import { ComplexityDetails } from './complexity-details';
import { CommentsDetails } from './comments-details';
import { ComplexityDistribution } from './complexity-distribution';
import { LanguageDistribution } from './language-distribution';
import { SizeTreemap } from './treemap';


export default class extends React.Component {
  render () {
    return <div className="overview-detailed-page">
      <div className="overview-domain-header">
        <h2 className="overview-title">Size</h2>
      </div>

      <a className="overview-detailed-page-back" href="#">
        <i className="icon-chevron-left"/>
      </a>

      <SizeTimeline {...this.props}/>

      <div className="flex-columns">
        <div className="flex-column flex-column-third">
          <SizeDetails {...this.props}/>
        </div>
        <div className="flex-column flex-column-two-thirds">
          <LanguageDistribution {...this.props}/>
        </div>
      </div>

      <div className="flex-columns">
        <div className="flex-column flex-column-third">
          <ComplexityDetails {...this.props}/>
        </div>
        <div className="flex-column flex-column-two-thirds">
          <ComplexityDistribution {...this.props}/>
        </div>
      </div>

      <div className="flex-columns">
        <div className="flex-column flex-column-third">
          <CommentsDetails {...this.props}/>
        </div>
      </div>

      <SizeTreemap {...this.props}/>
    </div>;
  }
}
