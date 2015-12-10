import moment from 'moment';
import React from 'react';

import { formatMeasure, localizeMetric } from '../../../helpers/measures';
import { DrilldownLink } from '../../../components/shared/drilldown-link';
import { IssuesLink } from '../../../components/shared/issues-link';
import { getShortType } from '../helpers/metrics';
import SeverityHelper from '../../../components/shared/severity-helper';


export const IssueMeasure = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    let leak = this.props.leak[this.props.metric];
    let added = this.props.leak[this.props.leakMetric];
    let removed = added - leak;

    return <div className="overview-detailed-measure-leak">
      <ul className="list-inline">
        <li className="text-danger">
          <IssuesLink className="text-danger overview-detailed-measure-value"
                      component={this.props.component.key} params={{ resolved: 'false' }}>
            +{formatMeasure(added, getShortType(this.props.type))}
          </IssuesLink>
        </li>
        <li className="text-success">
          <span className="text-success overview-detailed-measure-value">
            -{formatMeasure(removed, getShortType(this.props.type))}
          </span>
        </li>
      </ul>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});


export const AddedRemovedMeasure = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    let leak = this.props.leak[this.props.metric];
    let added = this.props.leak[this.props.leakMetric] || 0;
    let removed = added - leak;

    let createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    return <div className="overview-detailed-measure-leak">
      <ul>
        <li style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left">{window.t('overview.added')}</small>
          <IssuesLink className="text-danger"
                      component={this.props.component.key} params={{ resolved: 'false', createdAfter }}>
            <span className="overview-detailed-measure-value">
              {formatMeasure(added, getShortType(this.props.type))}
            </span>
          </IssuesLink>
        </li>
        <li className="little-spacer-top" style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left">{window.t('overview.removed')}</small>
          <span className="text-success">
            {formatMeasure(removed, getShortType(this.props.type))}
          </span>
        </li>
      </ul>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});


export const AddedRemovedDebt = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    let leak = this.props.leak[this.props.metric];
    let added = this.props.leak[this.props.leakMetric] || 0;
    let removed = added - leak;

    return <div className="overview-detailed-measure-leak">
      <ul>
        <li style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left">Added</small>
          <DrilldownLink className="text-danger" component={this.props.component.key} metric={this.props.leakMetric}
                         period={this.props.leakPeriodIndex}>
            <span className="overview-detailed-measure-value">
              {formatMeasure(added, getShortType(this.props.type))}
            </span>
          </DrilldownLink>
        </li>
        <li className="little-spacer-top" style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left">Removed</small>
          <span className="text-success">
            {formatMeasure(removed, getShortType(this.props.type))}
          </span>
        </li>
      </ul>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});


export const OnNewCodeMeasure = React.createClass({
  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    let onNewCode = this.props.leak[this.props.leakMetric];

    return <div className="overview-detailed-measure-leak">
      <ul>
        <li className="little-spacer-top" style={{ display: 'flex', alignItems: 'center' }}>
          <small className="flex-1 text-left">{window.t('overview.on_new_code')}</small>
          <DrilldownLink component={this.props.component.key} metric={this.props.leakMetric}
                         period={this.props.leakPeriodIndex}>
            <span className="overview-detailed-measure-value">
              {formatMeasure(onNewCode, getShortType(this.props.type))}
            </span>
          </DrilldownLink>
        </li>
      </ul>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.props.metric];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">{localizeMetric(this.props.metric)}</span>
        <span className="overview-detailed-measure-value">
          <DrilldownLink component={this.props.component.key} metric={this.props.metric}>
            {formatMeasure(measure, this.props.type)}
          </DrilldownLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});


export const SeverityMeasure = React.createClass({
  getMetric () {
    return this.props.severity.toLowerCase() + '_violations';
  },

  getNewMetric () {
    return 'new_' + this.props.severity.toLowerCase() + '_violations';
  },


  renderLeak () {
    if (!this.props.leakPeriodDate) {
      return null;
    }

    let leak = this.props.leak[this.getMetric()] || 0;
    let added = this.props.leak[this.getNewMetric()] || 0;
    let removed = added - leak;

    let createdAfter = moment(this.props.leakPeriodDate).format('YYYY-MM-DDTHH:mm:ssZZ');

    return <div className="overview-detailed-measure-leak">
      <ul>
        <li style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left text-ellipsis">{window.t('overview.added')}</small>
          <IssuesLink className="text-danger"
                      component={this.props.component.key}
                      params={{ resolved: 'false', severities: this.props.severity, createdAfter: createdAfter }}>
            <span className="overview-detailed-measure-value">
              {formatMeasure(added, 'SHORT_INT')}
            </span>
          </IssuesLink>
        </li>
        <li className="little-spacer-top" style={{ display: 'flex', alignItems: 'baseline' }}>
          <small className="flex-1 text-left text-ellipsis">{window.t('overview.removed')}</small>
          <span className="text-success">
            {formatMeasure(removed, 'SHORT_INT')}
          </span>
        </li>
      </ul>
    </div>;
  },

  render () {
    let measure = this.props.measures[this.getMetric()];
    if (measure == null) {
      return null;
    }

    return <div className="overview-detailed-measure">
      <div className="overview-detailed-measure-nutshell">
        <span className="overview-detailed-measure-name">
          <SeverityHelper severity={this.props.severity}/>
        </span>
        <span className="overview-detailed-measure-value">
          <IssuesLink component={this.props.component.key}
                      params={{ resolved: 'false', severities: this.props.severity }}>
            {formatMeasure(measure, 'SHORT_INT')}
          </IssuesLink>
        </span>
        {this.props.children}
      </div>
      {this.renderLeak()}
    </div>;
  }
});
