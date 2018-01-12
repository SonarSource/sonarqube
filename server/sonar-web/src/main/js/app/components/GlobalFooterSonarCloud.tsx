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
import { translate } from '../../helpers/l10n';

export default function GlobalFooterSonarCloud() {
  return (
    <div id="footer" className="page-footer page-container">
      <div>
        © 2008-2018, SonarCloud by{' '}
        <a href="http://www.sonarsource.com" title="SonarSource SA">
          SonarSource SA
        </a>
        . All rights reserved.
      </div>

      <ul className="page-footer-menu">
        <li className="page-footer-menu-item">
          <a href="https://about.sonarcloud.io/news/">{translate('footer.news')}</a>
        </li>
        <li className="page-footer-menu-item">
          <a href="https://about.sonarcloud.io/terms.pdf">{translate('footer.terms')}</a>
        </li>
        <li className="page-footer-menu-item">
          <a href="https://twitter.com/sonarcloud">{translate('footer.twitter')}</a>
        </li>
        <li className="page-footer-menu-item">
          <a href="https://about.sonarcloud.io/get-started/">{translate('footer.get_started')}</a>
        </li>
        <li className="page-footer-menu-item">
          <a href="https://about.sonarcloud.io/contact/">{translate('footer.help')}</a>
        </li>
        <li className="page-footer-menu-item">
          <a href="https://about.sonarcloud.io/">{translate('footer.about')}</a>
        </li>
      </ul>
    </div>
  );
}
