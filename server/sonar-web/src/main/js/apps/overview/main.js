import React from 'react';
import offset from 'document-offset';

import GeneralMain from './general/main';
import IssuesMain from './issues/main';
import CoverageMain from './coverage/main';
import DuplicationsMain from './duplications/main';
import SizeMain from './size/main';
import Meta from './meta';

import { getMetrics } from '../../api/metrics';

export default class Overview extends React.Component {
  constructor () {
    super();
    let hash = window.location.hash;
    this.state = { section: hash.length ? hash.substr(1) : null };
  }

  componentDidMount () {
    this.requestMetrics();
  }

  requestMetrics () {
    return getMetrics().then(metrics => this.setState({ metrics }));
  }

  handleRoute (section, el) {
    this.setState({ section }, () => this.scrollToEl(el));
    window.location.href = '#' + section;
  }

  scrollToEl (el) {
    let top = offset(el).top - el.getBoundingClientRect().height;
    window.scrollTo(0, top);
  }

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
        <GeneralMain {...this.props} section={this.state.section} onRoute={this.handleRoute.bind(this)}/>
        {child}
      </div>
      <Meta component={this.props.component}/>
    </div>;
  }
}
