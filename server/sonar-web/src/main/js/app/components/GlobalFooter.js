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
import GlobalFooterForSonarQubeDotCom from './GlobalFooterForSonarQubeDotCom';
import GlobalFooterBranding from './GlobalFooterBranding';
import { translate, translateWithParameters } from '../../helpers/l10n';

type Props = {
  hideLoggedInInfo?: boolean,
  productionDatabase: boolean,
  sonarqubeDotCom?: { value: string },
  sonarqubeVersion?: string
};

export default function GlobalFooter({
  hideLoggedInInfo,
  productionDatabase,
  sonarqubeDotCom,
  sonarqubeVersion
}: Props) {
  if (sonarqubeDotCom && sonarqubeDotCom.value === 'true') {
    return <GlobalFooterForSonarQubeDotCom hideLoggedInInfo={hideLoggedInInfo} />;
  }

  return (
    <div id="footer" className="page-footer page-container">
      {productionDatabase === false &&
        <div className="alert alert-danger">
          <p className="big" id="evaluation_warning">
            {translate('footer.production_database_warning')}
          </p>
          <p>
            {translate('footer.production_database_explanation')}
          </p>
        </div>}

      <GlobalFooterBranding />

      <div>
        {!hideLoggedInInfo &&
          sonarqubeVersion &&
          translateWithParameters('footer.version_x', sonarqubeVersion)}
        {!hideLoggedInInfo && sonarqubeVersion && ' - '}
        <a href="http://www.gnu.org/licenses/lgpl-3.0.txt">{translate('footer.licence')}</a>
        {' - '}
        <a href="http://www.sonarqube.org">{translate('footer.community')}</a>
        {' - '}
        <a href="https://redirect.sonarsource.com/doc/home.html">
          {translate('footer.documentation')}
        </a>
        {' - '}
        <a href="https://redirect.sonarsource.com/doc/community.html">
          {translate('footer.support')}
        </a>
        {' - '}
        <a href="https://redirect.sonarsource.com/doc/plugin-library.html">
          {translate('footer.plugins')}
        </a>
        {!hideLoggedInInfo && ' - '}
        {!hideLoggedInInfo && <Link to="/web_api">{translate('footer.web_api')}</Link>}
        {!hideLoggedInInfo && ' - '}
        {!hideLoggedInInfo && <Link to="/about">{translate('footer.about')}</Link>}
      </div>
    </div>
  );
}
