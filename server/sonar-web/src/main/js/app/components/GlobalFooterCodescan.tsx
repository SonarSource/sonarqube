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
import styled from '@emotion/styled';
import { getYear } from 'date-fns';
import {
  LAYOUT_VIEWPORT_MIN_WIDTH,
  PageContentFontWrapper,
  themeBorder,
  themeColor,
} from '~design-system';
import * as React from 'react';
import { translate } from '../../helpers/l10n';

export default function GlobalFooterCodescan() {
  return (
    <StyledFooter className="sw-p-6" id="footer">
      <PageContentFontWrapper className="sw-typo-default sw-h-full sw-flex sw-flex-col sw-items-stretch">
        <div className="sw-flex sw-justify-center sw-items-center">
          <div>
            © 2017-{getYear(new Date())}&nbsp;
            <a
              href="https://www.codescan.io"
              rel="noopener noreferrer"
              target="_blank"
              title="CodeScan Enterprises LLC"
              className="hyperlink-text"
            >
              CodeScan Enterprises LLC
            </a>
            . All rights reserved.
          </div>
        </div>

        <StyledFooterLinks className="sw-flex sw-justify-center sw-items-center">
          <div>Version {translate('footer.codescan_version')}</div>
          <div>
            <a
              rel="noopener noreferrer"
              target="_blank"
              href="https://www.gnu.org/licenses/lgpl-3.0.txt"
              className="hyperlink-text"
            >
              LGPL v3
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              className="hyperlink-text"
              target="_blank"
              href="https://www.codescan.io/tos/"
            >
              {translate('footer.terms')}
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              target="_blank"
              href="https://www.linkedin.com/company/code-scan"
              className="hyperlink-text"
            >
              Linkedin
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              target="_blank"
              href="https://www.facebook.com/CodeScanForSalesforce/"
              className="hyperlink-text"
            >
              Facebook
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              className="hyperlink-text"
              target="_blank"
              href="https://twitter.com/CodeScanforSFDC"
            >
              Twitter
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              className="hyperlink-text"
              target="_blank"
              href="https://www.codescan.io/contact/"
            >
              {translate('footer.help')}
            </a>
          </div>
          <div>
            <a
              rel="noopener noreferrer"
              target="_blank"
              href="https://knowledgebase.autorabit.com/codescan/docs/codescan-getting-started"
              className="hyperlink-text"
            >
              {translate('footer.about')}
            </a>
          </div>
        </StyledFooterLinks>
      </PageContentFontWrapper>
    </StyledFooter>
  );
}

const StyledFooter = styled.div`
  background-color: ${themeColor('backgroundSecondary')};
  border-top: ${themeBorder('default')};
  box-sizing: border-box;
  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;
`;

const StyledFooterLinks = styled.div`
  > div:before {
    content: '-';
    padding: 0 4px;
    -webkit-user-select: none;
    -ms-user-select: none;
    user-select: none;
  }

  > div:first-child:before {
    display: none;
  }
`;
