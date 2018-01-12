/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import * as PropTypes from 'prop-types';
import { Link } from 'react-router';
import GlobalFooterSonarCloud from './GlobalFooterSonarCloud';
import GlobalFooterBranding from './GlobalFooterBranding';
import { translate, translateWithParameters } from '../../helpers/l10n';

interface Props {
  hideLoggedInInfo?: boolean;
  productionDatabase: boolean;
  sonarqubeVersion?: string;
}

export default class GlobalFooter extends React.PureComponent<Props> {
  static contextTypes = {
    onSonarCloud: PropTypes.bool
  };

  render() {
    const { hideLoggedInInfo, productionDatabase, sonarqubeVersion } = this.props;
    if (this.context.onSonarCloud) {
      return <GlobalFooterSonarCloud />;
    }

    return (
      <div id="footer" className="page-footer page-container">
        {productionDatabase === false && (
          <div className="alert alert-danger">
            <p className="big" id="evaluation_warning">
              {translate('footer.production_database_warning')}
            </p>
            <p>{translate('footer.production_database_explanation')}</p>
          </div>
        )}

        <GlobalFooterBranding />

        <ul className="page-footer-menu">
          {!hideLoggedInInfo &&
            sonarqubeVersion && (
              <li className="page-footer-menu-item">
                {translateWithParameters('footer.version_x', sonarqubeVersion)}
              </li>
            )}
          <li className="page-footer-menu-item">
            <a href="http://www.gnu.org/licenses/lgpl-3.0.txt">{translate('footer.license')}</a>
          </li>
          <li className="page-footer-menu-item">
            <a href="http://www.sonarqube.org">{translate('footer.community')}</a>
          </li>
          <li className="page-footer-menu-item">
            <a href="https://redirect.sonarsource.com/doc/home.html">
              {translate('footer.documentation')}
            </a>
          </li>
          <li className="page-footer-menu-item">
            <a href="https://redirect.sonarsource.com/doc/community.html">
              {translate('footer.support')}
            </a>
          </li>
          <li className="page-footer-menu-item">
            <a href="https://redirect.sonarsource.com/doc/plugin-library.html">
              {translate('footer.plugins')}
            </a>
          </li>
          {!hideLoggedInInfo && (
            <li className="page-footer-menu-item">
              <Link to="/web_api">{translate('footer.web_api')}</Link>
            </li>
          )}
          {!hideLoggedInInfo && (
            <li className="page-footer-menu-item">
              <Link to="/about">{translate('footer.about')}</Link>
            </li>
          )}
        </ul>
      </div>
    );
  }
}
