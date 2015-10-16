import React from 'react';
import Avatar from '../../../components/shared/avatar';
import RecentHistory from '../component/recent-history';

export default React.createClass({
  renderAuthenticated() {
    return (
        <li className="dropdown">
          <a className="dropdown-toggle" data-toggle="dropdown" href="#">
            <Avatar email={window.SS.userEmail} size={20}/>&nbsp;
            {window.SS.userName}&nbsp;<i className="icon-dropdown"/>
          </a>
          <ul className="dropdown-menu dropdown-menu-right">
            <li>
              <a href={`${window.baseUrl}/account/index`}>{window.t('layout.user_panel.my_profile')}</a>
            </li>
            <li>
              <a onClick={this.handleLogout} href="#">{window.t('layout.logout')}</a>
            </li>
          </ul>
        </li>
    );
  },

  renderAnonymous() {
    return (
        <li>
          <a onClick={this.handleLogin} href="#">{window.t('layout.login')}</a>
        </li>
    );
  },

  handleLogin(e) {
    e.preventDefault();
    const returnTo = window.location.pathname + window.location.search;
    window.location = `${window.baseUrl}/sessions/new?return_to=${encodeURIComponent(returnTo)}${window.location.hash}`;
  },

  handleLogout(e) {
    e.preventDefault();
    RecentHistory.clear();
    window.location = `${window.baseUrl}/sessions/logout`;
  },

  render() {
    const isUserAuthenticated = !!window.SS.user;
    return isUserAuthenticated ? this.renderAuthenticated() : this.renderAnonymous();
  }
});
