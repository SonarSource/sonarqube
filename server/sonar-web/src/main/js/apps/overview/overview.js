import React from 'react';

import Gate from './gate/gate';
import GeneralMain from './general/main';
import Meta from './meta';
import { getMetrics } from '../../api/metrics';


export const Overview = React.createClass({
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

  render () {
    if (!this.state.ready) {
      return this.renderLoading();
    }

    return <div className="overview">
      <div className="overview-main">
        <Gate component={this.props.component} gate={this.props.gate}/>
        <GeneralMain {...this.props} {...this.state}/>
      </div>
      <Meta component={this.props.component}/>
    </div>;
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
