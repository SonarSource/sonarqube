import moment from 'moment';
import React from 'react';

import { Timeline } from './timeline';


export const Domain = React.createClass({
  render () {
    return <div className="overview-card">{this.props.children}</div>;
  }
});


export const DomainTitle = React.createClass({
  render () {
    if (this.props.linkTo) {
      let url = window.baseUrl + '/overview' + this.props.linkTo +
          '?id=' + encodeURIComponent(this.props.component.key);
      return <div>
        <div className="overview-title">
          {this.props.children}
          <a className="small big-spacer-left link-no-underline" href={url}>
            {window.t('more')}&nbsp;
            <i className="icon-chevron-right" style={{ position: 'relative', top: -1 }}/>
          </a>
        </div>
      </div>;
    } else {
      return <div className="overview-title">{this.props.children}</div>;
    }
  }
});


export const DomainLeakTitle = React.createClass({
  renderInline (tooltip, fromNow) {
    return <span className="overview-domain-leak-title" title={tooltip} data-toggle="tooltip">
      <span>{window.tp('overview.leak_period_x', this.props.label)}</span>
      <span className="note spacer-left">{window.tp('overview.started_x', fromNow)}</span>
    </span>;
  },

  render() {
    if (!this.props.label || !this.props.date) {
      return null;
    }
    let momentDate = moment(this.props.date);
    let fromNow = momentDate.fromNow();
    let tooltip = 'Started on ' + momentDate.format('LL');
    if (this.props.inline) {
      return this.renderInline(tooltip, fromNow);
    }
    return <span className="overview-domain-leak-title" title={tooltip} data-toggle="tooltip">
      <span>{window.tp('overview.leak_period_x', this.props.label)}</span>
      <br/>
      <span className="note">{window.tp('overview.started_x', fromNow)}</span>
    </span>;
  }
});


export const DomainHeader = React.createClass({
  render () {
    return <div className="overview-card-header">
      <DomainTitle {...this.props}>{this.props.title}</DomainTitle>
    </div>;
  }
});


export const DomainPanel = React.createClass({
  propTypes: {
    domain: React.PropTypes.string
  },

  render () {
    return <div className="overview-domain-panel">
      {this.props.children}
    </div>;
  }
});


export const DomainNutshell = React.createClass({
  render () {
    return <div className="overview-domain-nutshell">{this.props.children}</div>;
  }
});

export const DomainLeak = React.createClass({
  render () {
    return <div className="overview-domain-leak">{this.props.children}</div>;
  }
});


export const MeasuresList = React.createClass({
  render () {
    return <div className="overview-domain-measures">{this.props.children}</div>;
  }
});


export const Measure = React.createClass({
  propTypes: {
    label: React.PropTypes.string,
    composite: React.PropTypes.bool
  },

  getDefaultProps() {
    return { composite: false };
  },

  renderValue () {
    if (this.props.composite) {
      return this.props.children;
    } else {
      return <div className="overview-domain-measure-value">
        {this.props.children}
      </div>;
    }
  },

  renderLabel() {
    return this.props.label ?
        <div className="overview-domain-measure-label">{this.props.label}</div> : null;
  },

  render () {
    return <div className="overview-domain-measure">
      {this.renderValue()}
      {this.renderLabel()}
    </div>;
  }
});


export const DomainMixin = {
  renderTimelineStartDate() {
    let momentDate = moment(this.props.historyStartDate);
    let fromNow = momentDate.fromNow();
    return <span className="overview-domain-timeline-date">{window.tp('overview.started_x', fromNow)}</span>;
  },

  renderTimeline(range, displayDate) {
    if (!this.props.history) {
      return null;
    }
    let props = { history: this.props.history };
    props[range] = this.props.leakPeriodDate;
    return <div className="overview-domain-timeline">
      <Timeline {...props}/>
      {displayDate ? this.renderTimelineStartDate(range) : null}
    </div>;
  },

  hasLeakPeriod () {
    return this.props.leakPeriodDate != null;
  }
};
