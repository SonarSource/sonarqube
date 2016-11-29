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
// @flow
import React from 'react';
import { connect } from 'react-redux';
import { getAppState } from '../store/rootReducer';

class GlobalFooter extends React.Component {
  render () {
    const { sonarqubeVersion, productionDatabase } = this.props;

    return (
        <div id="footer" className="page-footer page-container">
          {!productionDatabase && (
              <div className="alert alert-danger">
                <p className="big" id="evaluation_warning">
                  Embedded database should be used for evaluation purpose only
                </p>
                <p>
                  The embedded database will not scale, it will not support upgrading to newer versions of SonarQube,
                  and there is no support for migrating your data out of it into a different database engine.
                </p>
              </div>
          )}

          <div>
            This application is based on
            {' '}
            <a href="http://www.sonarqube.org/" title="SonarQube&trade;">SonarQube&trade;</a>
            {' '}
            but is <strong>not</strong> an official version provided by
            {' '}
            <a href="http://www.sonarsource.com" title="SonarSource SA">SonarSource SA</a>.
          </div>


          <div>
            Version {sonarqubeVersion}
            {' - '}
            <a href="http://www.gnu.org/licenses/lgpl-3.0.txt">LGPL v3</a>
            {' - '}
            <a href="http://www.sonarqube.org">Community</a>
            {' - '}
            <a href="http://www.sonarqube.org/documentation">Documentation</a>
            {' - '}
            <a href="http://www.sonarqube.org/support">Get Support</a>
            {' - '}
            <a href="http://redirect.sonarsource.com/doc/plugin-library.html">Plugins</a>
            {' - '}
            <a href={window.baseUrl + '/web_api'}>Web API</a>
            {' - '}
            <a href={window.baseUrl + '/about'}>About</a>
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
