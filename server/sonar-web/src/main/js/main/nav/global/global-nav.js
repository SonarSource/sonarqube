/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
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
