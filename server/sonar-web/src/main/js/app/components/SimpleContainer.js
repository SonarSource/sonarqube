/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
// @flow
import React from 'react';
import GlobalFooterContainer from './GlobalFooterContainer';

type Props = {
  children?: React.Element<*> | Array<React.Element<*>>,
  hideLoggedInInfo?: boolean
};

export default class SimpleContainer extends React.PureComponent {
  props: Props;

  componentDidMount() {
    const html = document.querySelector('html');
    if (html) {
      html.classList.add('dashboard-page');
    }
  }

  componentWillUnmount() {
    const html = document.querySelector('html');
    if (html) {
      html.classList.remove('dashboard-page');
    }
  }

  render() {
    return (
      <div className="global-container">
        <div className="page-wrapper page-wrapper-global" id="container">
          <nav className="navbar navbar-global page-container" id="global-navigation">
            <div className="navbar-header" />
          </nav>

          <div id="bd" className="page-wrapper-simple">
            <div id="nonav" className="page-simple">
              {this.props.children}
            </div>
          </div>
        </div>
        <GlobalFooterContainer hideLoggedInInfo={this.props.hideLoggedInInfo} />
      </div>
    );
  }
}
