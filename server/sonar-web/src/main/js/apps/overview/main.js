import $ from 'jquery';
import _ from 'underscore';
import moment from 'moment';
import React from 'react';
import Gate from './gate';
import Leak from './leak';
import Nutshell from './nutshell';
import Meta from './meta';
import {getPeriodDate} from './helpers/period-label';

export default React.createClass({
  getInitialState() {
    return { leak: this.props.leak, measures: this.props.measures };
  },

  componentDidMount() {
    if (this._hasWaterLeak()) {
      this.requestLeakIssues();
      this.requestLeakDebt();
    }
    this.requestNutshellIssues();
    this.requestNutshellDebt();
  },

  _hasWaterLeak() {
    return !!_.findWhere(this.props.component.periods, { index: '3' });
  },

  _requestIssues(data) {
    let url = `${baseUrl}/api/issues/search`;
    data.ps = 1;
    data.componentUuids = this.props.component.id;
    return $.get(url, data);
  },

  requestLeakIssues() {
    let createdAfter = moment(getPeriodDate(this.props.component.periods, '3')).format('YYYY-MM-DDTHH:mm:ssZZ');
    this._requestIssues({ resolved: 'false', createdAfter, facets: 'severities,statuses' }).done(r => {
      let
          severitiesFacet = _.findWhere(r.facets, { property: 'severities' }).values,
          statusesFacet = _.findWhere(r.facets, { property: 'statuses' }).values;

      this.setState({
        leak: _.extend({}, this.state.leak, {
          newIssues: r.total,
          newBlockerIssues: _.findWhere(severitiesFacet, { val: 'BLOCKER' }).count,
          newCriticalIssues: _.findWhere(severitiesFacet, { val: 'CRITICAL' }).count,
          newOpenIssues: _.findWhere(statusesFacet, { val: 'OPEN' }).count,
          newReopenedIssues: _.findWhere(statusesFacet, { val: 'REOPENED' }).count
        })
      });
    });
  },

  requestNutshellIssues() {
    this._requestIssues({ resolved: 'false', facets: 'severities,statuses' }).done(r => {
      let
          severitiesFacet = _.findWhere(r.facets, { property: 'severities' }).values,
          statusesFacet = _.findWhere(r.facets, { property: 'statuses' }).values;

      this.setState({
        measures: _.extend({}, this.state.measures, {
          issues: r.total,
          blockerIssues: _.findWhere(severitiesFacet, { val: 'BLOCKER' }).count,
          criticalIssues: _.findWhere(severitiesFacet, { val: 'CRITICAL' }).count,
          openIssues: _.findWhere(statusesFacet, { val: 'OPEN' }).count,
          reopenedIssues: _.findWhere(statusesFacet, { val: 'REOPENED' }).count
        })
      });
    });
  },

  requestLeakDebt() {
    let createdAfter = moment(getPeriodDate(this.props.component.periods, '3')).format('YYYY-MM-DDTHH:mm:ssZZ');
    this._requestIssues({ resolved: 'false', createdAfter, facets: 'severities', facetMode: 'debt' }).done(r => {
      this.setState({
        leak: _.extend({}, this.state.leak, { newDebt: r.debtTotal })
      });
    });
  },

  requestNutshellDebt() {
    this._requestIssues({ resolved: 'false', facets: 'severities', facetMode: 'debt' }).done(r => {
      this.setState({
        measures: _.extend({}, this.state.measures, { debt: r.debtTotal })
      });
    });
  },

  render() {
    return (
        <div className="overview">
          <div className="overview-main">
            <Gate component={this.props.component} gate={this.props.gate}/>
            <Leak component={this.props.component} leak={this.state.leak} measures={this.state.measures}/>
            <Nutshell component={this.props.component} measures={this.state.measures}/>
          </div>
          <Meta component={this.props.component}/>
        </div>
    );
  }
});
