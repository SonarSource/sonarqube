import React from 'react';

import Gate from './gate/gate';
import GeneralMain from './main/main';
import Meta from './meta';
import { StructureMain } from './domains/structure-domain';
import { DuplicationsMain } from './domains/duplications-domain';
import { CoverageMain } from './domains/coverage-domain';
import { DebtMain } from './domains/debt-domain';

import { getMetrics } from '../../api/metrics';
import { RouterMixin } from '../../components/router/router';


export const Overview = React.createClass({
  mixins: [RouterMixin],

  getInitialState () {
    return { ready: false };
  },

  componentDidMount () {
    this.requestMetrics();
  },

  requestMetrics () {
    return getMetrics().then(metrics => this.setState({ ready: true, metrics }));
  },

  renderLoading () {
    return <div className="text-center">
      <i className="spinner spinner-margin"/>
    </div>;
  },

  renderMain() {
    return <div className="overview">
      <div className="overview-main">
        <Gate component={this.props.component} gate={this.props.gate}/>
        <GeneralMain {...this.props} {...this.state} navigate={this.navigate}/>
      </div>
      <Meta component={this.props.component}/>
    </div>;
  },

  renderSize () {
    return <div className="overview">
      <StructureMain {...this.props} {...this.state}/>
    </div>;
  },

  renderDuplications () {
    return <div className="overview">
      <DuplicationsMain {...this.props} {...this.state}/>
    </div>;
  },

  renderTests () {
    return <div className="overview">
      <CoverageMain {...this.props} {...this.state}/>
    </div>;
  },

  renderIssues () {
    return <div className="overview">
      <DebtMain {...this.props} {...this.state}/>
    </div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }
    switch (this.state.route) {
      case '':
        return this.renderMain();
      case '/structure':
        return this.renderSize();
      case '/duplications':
        return this.renderDuplications();
      case '/coverage':
        return this.renderTests();
      case '/debt':
        return this.renderIssues();
      default:
        throw new Error('Unknown route: ' + this.state.route);
    }
  }
});


export const EmptyOverview = React.createClass({
  render() {
    return (
        <div className="page">
          <div className="alert alert-warning">
            {window.t('provisioning.no_analysis')}
          </div>
        </div>
    );
  }
});
