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
import { Link } from 'react-router';
import { connect } from 'react-redux';
import { getAppState } from '../../store/rootReducer';
import GlobalFooterBranding from './GlobalFooterBranding';

class GlobalFooter extends React.Component {
  render() {
    const { sonarqubeVersion, productionDatabase } = this.props;

    return (
      <div id="footer" className="page-footer page-container">
        {productionDatabase === false &&
          <div className="alert alert-danger">
            <p className="big" id="evaluation_warning">
              Embedded database should be used for evaluation purpose only
            </p>
            <p>
              The embedded database will not scale, it will not support upgrading to newer
              {' '}
              versions of SonarQube, and there is no support for migrating your data out of it
              {' '}
              into a different database engine.
            </p>
          </div>}

        <GlobalFooterBranding />

        <div>
          Version {sonarqubeVersion}
          {' - '}
          <a href="http://www.gnu.org/licenses/lgpl-3.0.txt">LGPL v3</a>
          {' - '}
          <a href="http://www.sonarqube.org">Community</a>
          {' - '}
          <a href="https://redirect.sonarsource.com/doc/home.html">Documentation</a>
          {' - '}
          <a href="https://redirect.sonarsource.com/doc/community.html">Get Support</a>
          {' - '}
          <a href="https://redirect.sonarsource.com/doc/plugin-library.html">Plugins</a>
          {' - '}
          <Link to="/web_api">Web API</Link>
          {' - '}
          <Link to="/about">About</Link>
        </div>
      </div>
    );
  }
}

const mapStateToProps = state => ({
  sonarqubeVersion: getAppState(state).version,
  productionDatabase: getAppState(state).productionDatabase
});

export default connect(mapStateToProps)(GlobalFooter);
