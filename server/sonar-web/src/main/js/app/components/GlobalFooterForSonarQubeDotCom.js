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
import { translate } from '../../helpers/l10n';

export default function GlobalFooterForSonarQubeDotCom() {
  return (
    <div id="footer" className="page-footer page-container">
      <div>
        © 2008-2017, SonarCloud.io by
        {' '}
        <a href="http://www.sonarsource.com" title="SonarSource SA">SonarSource SA</a>
        . All rights reserved.
      </div>

      <div>
        <a href="https://about.sonarcloud.io/news/">{translate('footer.news')}</a>
        {' - '}
        <a href="https://about.sonarcloud.io/terms.pdf">{translate('footer.terms')}</a>
        {' - '}
        <a href="https://twitter.com/sonarqube">{translate('footer.twitter')}</a>
        {' - '}
        <a href="https://about.sonarcloud.io/get-started/">{translate('footer.get_started')}</a>
        {' - '}
        <a href="https://about.sonarcloud.io/contact/">{translate('footer.help')}</a>
        {' - '}
        {<Link to="/about">{translate('footer.about')}</Link>}
      </div>
    </div>
  );
}
