import React from 'react';
import GlobalNavBranding from './global-nav-branding';
import GlobalNavMenu from './global-nav-menu';
import GlobalNavUser from './global-nav-user';
import GlobalNavSearch from './global-nav-search';
import ShortcutsHelpView from './shortcuts-help-view';

let $ = jQuery;

export default React.createClass({
  getInitialState() {
    return this.props;
  },

  componentDidMount() {
    this.loadGlobalNavDetails();
  },

  loadGlobalNavDetails() {
    $.get(`${window.baseUrl}/api/navigation/global`).done(r => {
      this.setState(r);
    });
  },

  openHelp(e) {
    e.preventDefault();
    new ShortcutsHelpView().render();
  },

  render() {
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
