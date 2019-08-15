/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { Link } from 'react-router';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import { translate, translateWithParameters } from 'sonar-ui-common/helpers/l10n';
import { EDITIONS } from '../../apps/marketplace/utils';
import InstanceMessage from '../../components/common/InstanceMessage';
import { isSonarCloud } from '../../helpers/system';
import GlobalFooterBranding from './GlobalFooterBranding';
import GlobalFooterSonarCloud from './GlobalFooterSonarCloud';

interface Props {
  hideLoggedInInfo?: boolean;
  productionDatabase: boolean;
  sonarqubeEdition?: T.EditionKey;
  sonarqubeVersion?: string;
}

export default function GlobalFooter({
  hideLoggedInInfo,
  productionDatabase,
  sonarqubeEdition,
  sonarqubeVersion
}: Props) {
  if (isSonarCloud()) {
    return <GlobalFooterSonarCloud />;
  }

  const currentEdition = EDITIONS.find(edition => edition.key === sonarqubeEdition);

  return (
    <div className="page-footer page-container" id="footer">
      {productionDatabase === false && (
        <Alert display="inline" id="evaluation_warning" variant="warning">
          <p className="big">{translate('footer.production_database_warning')}</p>
          <p>
            <InstanceMessage message={translate('footer.production_database_explanation')} />
          </p>
        </Alert>
      )}

      <GlobalFooterBranding />

      <ul className="page-footer-menu">
        {!hideLoggedInInfo && currentEdition && (
          <li className="page-footer-menu-item">{currentEdition.name}</li>
        )}
        {!hideLoggedInInfo && sonarqubeVersion && (
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
          <Link to="/documentation">{translate('footer.documentation')}</Link>
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
