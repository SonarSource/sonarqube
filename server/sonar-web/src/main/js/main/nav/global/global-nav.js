import React from 'react';
import GlobalNavBranding from './global-nav-branding';
import GlobalNavMenu from './global-nav-menu';
import GlobalNavUser from './global-nav-user';
import GlobalNavSearch from './global-nav-search';
import ShortcutsHelpView from './shortcuts-help-view';

export default React.createClass({
  componentDidMount() {
    window.addEventListener('keypress', this.onKeyPress);
  },

  componentWillUnmount() {
    window.removeEventListener('keypress', this.onKeyPress);
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
    if (e) {
      e.preventDefault();
    }
    new ShortcutsHelpView().render();
  },

  render() {
    return (
        <div className="container">
          <GlobalNavBranding {...this.props}/>

          <GlobalNavMenu {...this.props}/>

          <ul className="nav navbar-nav navbar-right">
            <GlobalNavUser {...this.props}/>
            <GlobalNavSearch {...this.props}/>
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
