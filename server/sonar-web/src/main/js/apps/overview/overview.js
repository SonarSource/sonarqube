import React from 'react';

import Gate from './main/gate/gate';
import GeneralMain from './main/main';
import Meta from './meta';
import { SizeMain } from './size/main';
import { DuplicationsMain } from './duplications/main';

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
      <SizeMain {...this.props} {...this.state}/>
    </div>;
  },

  renderDuplications () {
    return <div className="overview">
      <DuplicationsMain {...this.props} {...this.state}/>
    </div>;
  },

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }
    switch (this.state.route) {
      case '':
        return this.renderMain();
      case '/size':
        return this.renderSize();
      case '/duplications':
        return this.renderDuplications();
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
