import React from 'react';
import offset from 'document-offset';
import GeneralMain from './general/main';
import IssuesMain from './issues/main';
import Meta from './meta';

export default class Overview extends React.Component {
  constructor () {
    super();
    let hash = window.location.hash;
    this.state = { section: hash.length ? hash.substr(1) : null };
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
    let child;
    switch (this.state.section) {
      case 'issues':
        child = <IssuesMain {...this.props}/>;
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
