import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import GlobalNavBranding from './global-nav-branding';
import GlobalNavMenu from './global-nav-menu';
import GlobalNavUser from './global-nav-user';
import GlobalNavSearch from './global-nav-search';
import ShortcutsHelpView from './shortcuts-help-view';

export default React.createClass({
  getInitialState() {
    return _.extend({}, this.props, { ready: false });
  },

  componentDidMount() {
    this.loadGlobalNavDetails();
    window.addEventListener('keypress', this.onKeyPress);
  },

  componentWillUnmount() {
    window.removeEventListener('keypress', this.onKeyPress);
  },

  loadGlobalNavDetails() {
    $.get(`${window.baseUrl}/api/navigation/global`).done(r => {
      this.setState(_.extend({ ready: true }, r));
    });
  },

  onKeyPress(e) {
    var tagName = e.target.tagName;
    if (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA') {
      var code = e.keyCode || e.which;
      if (code === 63) {
        this.openHelp();
      }
    }
  },

  openHelp(e) {
    e && e.preventDefault();
    new ShortcutsHelpView().render();
  },

  render() {
    if (!this.state.ready) {
      return null;
    }

    return (
        <div className="container">
          <GlobalNavBranding {...this.state}/>

          <GlobalNavMenu {...this.state}/>

          <ul className="nav navbar-nav navbar-right">
            <GlobalNavUser {...this.state}/>
            <GlobalNavSearch {...this.state}/>
            <li>
              <a onClick={this.openHelp} href="#">
                <i className="icon-help navbar-icon"/>
              </a>
            </li>
          </ul>
        </div>
    );
  }
});
