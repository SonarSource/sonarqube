import React from 'react';
import offset from 'document-offset';

import GeneralMain from './general/main';
import IssuesMain from './issues/main';
import CoverageMain from './coverage/main';
import DuplicationsMain from './duplications/main';
import SizeMain from './size/main';
import Meta from './meta';

import { getMetrics } from '../../api/metrics';


export const Overview = React.createClass({
  getInitialState () {
    let hash = window.location.hash;
    return { section: hash.length ? hash.substr(1) : null };
  },

  componentWillMount () {
    window.addEventListener('hashchange', this.handleHashChange);
  },

  componentDidMount () {
    this.requestMetrics();
  },

  componentWillUnmount () {
    window.removeEventListener('hashchange', this.handleHashChange);
  },

  requestMetrics () {
    return getMetrics().then(metrics => this.setState({ metrics }));
  },

  handleRoute (section, el) {
    if (section !== this.state.section) {
      this.setState({ section }, () => this.scrollToEl(el));
      window.location.href = '#' + section;
    } else {
      this.setState({ section: null });
      window.location.href = '#';
    }
  },

  handleHashChange () {
    let hash = window.location.hash;
    this.setState({ section: hash.substr(1) });
  },

  scrollToEl (el) {
    let top = offset(el).top - el.getBoundingClientRect().height;
    window.scrollTo(0, top);
  },

  render () {
    if (!this.state.metrics) {
      return null;
    }

    let child;
    switch (this.state.section) {
      case 'issues':
        child = <IssuesMain {...this.props} {...this.state}/>;
        break;
      case 'coverage':
        child = <CoverageMain {...this.props} {...this.state}/>;
        break;
      case 'duplications':
        child = <DuplicationsMain {...this.props} {...this.state}/>;
        break;
      case 'size':
        child = <SizeMain {...this.props} {...this.state}/>;
        break;
      default:
        child = null;
    }

    return <div className="overview">
      <div className="overview-main">
        <GeneralMain {...this.props} section={this.state.section} onRoute={this.handleRoute}/>
        {child}
      </div>
      <Meta component={this.props.component}/>
    </div>;
  }
});
