/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import { getYear } from "date-fns";
import { translate } from "../../helpers/l10n";

export default function GlobalFooterCodescan() {
  return (
      <div className="page-footer page-container" id="footer">
        <div>
          © 2017-{getYear(new Date())} <a
            href="https://www.codescan.io"
            rel="noopener noreferrer"
            target="_blank"
            title="CodeScan Enterprises LLC">
          CodeScan Enterprises LLC
        </a>
          . All rights reserved.
        </div>

        <ul className="page-footer-menu">
          <li className="page-footer-menu-item">
            Version {translate('footer.codescan_version')}
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank" href="https://www.gnu.org/licenses/lgpl-3.0.txt">LGPL v3</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank"
               href="https://www.codescan.io/tos/">{translate('footer.terms')}</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank" href="https://www.linkedin.com/company/code-scan">Linkedin</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank"
               href="https://www.facebook.com/CodeScanForSalesforce/">Facebook</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank" href="https://twitter.com/CodeScanforSFDC">Twitter</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank"
               href="https://www.codescan.io/contact/">{translate('footer.help')}</a>
          </li>
          <li className="page-footer-menu-item">
            <a rel="noopener noreferrer" target="_blank"
               href="https://knowledgebase.autorabit.com/codescan/docs/codescan-getting-started">{translate('footer.about')}</a>
          </li>
        </ul>
      </div>
  );
}
