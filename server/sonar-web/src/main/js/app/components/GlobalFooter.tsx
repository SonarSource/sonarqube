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
import { LinkHighlight, LinkStandalone } from '@sonarsource/echoes-react';
import { FlagMessage, LAYOUT_VIEWPORT_MIN_WIDTH, themeBorder, themeColor } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import InstanceMessage from '../../components/common/InstanceMessage';
import AppVersionStatus from '../../components/shared/AppVersionStatus';
import { DocLink } from '../../helpers/doc-links';
import { useDocUrl } from '../../helpers/docs';
import { getEdition } from '../../helpers/editions';
import GlobalFooterBranding from './GlobalFooterBranding';
import { useAppState } from './app-state/withAppStateContext';

interface GlobalFooterProps {
  hideLoggedInInfo?: boolean;
}

export default function GlobalFooter({ hideLoggedInInfo }: Readonly<GlobalFooterProps>) {
  const appState = useAppState();
  const currentEdition = appState?.edition && getEdition(appState.edition);
  const intl = useIntl();

  const docUrl = useDocUrl();

  return (
    <StyledFooter className="sw-p-6" id="footer">
      <div className="sw-typo-default sw-h-full sw-flex sw-flex-col sw-items-stretch">
        {appState?.productionDatabase === false && (
          <FlagMessage className="sw-mb-4" id="evaluation_warning" variant="warning">
            <p>
              <span className="sw-typo-lg-semibold">
                {intl.formatMessage({ id: 'footer.production_database_warning' })}
              </span>

              <br />

              <InstanceMessage
                message={intl.formatMessage({ id: 'footer.production_database_explanation' })}
              />
            </p>
          </FlagMessage>
        )}

        <div className="sw-flex sw-justify-between sw-items-center">
          <GlobalFooterBranding />

          <ul className="sw-flex sw-items-center sw-gap-3 sw-ml-4">
            {!hideLoggedInInfo && currentEdition && <li>{currentEdition.name}</li>}

            {!hideLoggedInInfo && appState?.version && (
              <li className="sw-code">
                <AppVersionStatus />
              </li>
            )}

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to="https://www.gnu.org/licenses/lgpl-3.0.txt"
              >
                {intl.formatMessage({ id: 'footer.license' })}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to="https://community.sonarsource.com/c/help/sq"
              >
                {intl.formatMessage({ id: 'footer.community' })}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone highlight={LinkHighlight.CurrentColor} to={docUrl(DocLink.Root)}>
                {intl.formatMessage({ id: 'footer.documentation' })}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to={docUrl(DocLink.InstanceAdminPluginVersionMatrix)}
              >
                {intl.formatMessage({ id: 'footer.plugins' })}
              </LinkStandalone>
            </li>

            {!hideLoggedInInfo && (
              <li>
                <LinkStandalone highlight={LinkHighlight.CurrentColor} to="/web_api">
                  {intl.formatMessage({ id: 'footer.web_api' })}
                </LinkStandalone>
              </li>
            )}
          </ul>
        </div>
      </div>
    </StyledFooter>
  );
}

const StyledFooter = styled.div`
  color: var(--echoes-color-text-subdued);
  background-color: ${themeColor('backgroundSecondary')};
  border-top: ${themeBorder('default')};
  box-sizing: border-box;
  min-width: ${LAYOUT_VIEWPORT_MIN_WIDTH}px;
`;
