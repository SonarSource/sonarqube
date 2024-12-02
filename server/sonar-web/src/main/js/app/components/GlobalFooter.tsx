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
import { useIntl } from 'react-intl';
import {
  FlagMessage,
  LAYOUT_VIEWPORT_MIN_WIDTH,
  SeparatorCircleIcon,
  themeBorder,
  themeColor,
} from '~design-system';
import InstanceMessage from '../../components/common/InstanceMessage';
import AppVersionStatus from '../../components/shared/AppVersionStatus';
import { COMMUNITY_FORUM_URL, DocLink } from '../../helpers/doc-links';
import { useDocUrl } from '../../helpers/docs';
import { getEdition } from '../../helpers/editions';
import { getInstanceVersionNumber } from '../../helpers/strings';
import { useStandardExperienceModeQuery } from '../../queries/mode';
import { EditionKey } from '../../types/editions';
import GlobalFooterBranding from './GlobalFooterBranding';
import { useAppState } from './app-state/withAppStateContext';

interface GlobalFooterProps {
  hideLoggedInInfo?: boolean;
}

export default function GlobalFooter({ hideLoggedInInfo }: Readonly<GlobalFooterProps>) {
  const appState = useAppState();
  const { data: isStandardMode } = useStandardExperienceModeQuery();
  const currentEdition = appState?.edition && getEdition(appState.edition);
  const intl = useIntl();
  const version = getInstanceVersionNumber(appState.version);

  const docUrl = useDocUrl();

  const isCommunityBuildRunning = appState.edition === EditionKey.community;

  return (
    <StyledFooter className="sw-p-6" id="footer">
      <div className="sw-h-full sw-flex sw-flex-col sw-items-stretch">
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

        <div className="sw-text-xs sw-flex sw-justify-between sw-items-center">
          <GlobalFooterBranding />

          {!hideLoggedInInfo && (
            <ul className="sw-code sw-flex sw-items-center sw-gap-1">
              {currentEdition && (
                <>
                  <li>{currentEdition.name}</li>
                  <SeparatorCircleIcon aria-hidden as="li" />
                </>
              )}

              {appState?.version && (
                <>
                  <li>{intl.formatMessage({ id: 'footer.version.short' }, { version })}</li>
                  <SeparatorCircleIcon aria-hidden as="li" />
                  <li>
                    <AppVersionStatus statusOnly />
                  </li>
                </>
              )}
              {isStandardMode !== undefined && (
                <>
                  <SeparatorCircleIcon aria-hidden as="li" />
                  <li className="sw-uppercase">
                    {intl.formatMessage({
                      id: `footer.mode.${isStandardMode ? 'STANDARD' : 'MQR'}`,
                    })}
                  </li>
                </>
              )}
            </ul>
          )}

          <ul className="sw-flex sw-items-center sw-gap-3">
            <li>
              {isCommunityBuildRunning ? (
                <LinkStandalone
                  shouldOpenInNewTab
                  highlight={LinkHighlight.CurrentColor}
                  to="https://www.gnu.org/licenses/lgpl-3.0.txt"
                >
                  {intl.formatMessage({ id: 'footer.license.lgplv3' })}
                </LinkStandalone>
              ) : (
                <LinkStandalone
                  shouldOpenInNewTab
                  highlight={LinkHighlight.CurrentColor}
                  to="https://www.sonarsource.com/legal/sonarqube/terms-and-conditions/"
                >
                  {intl.formatMessage({ id: 'footer.license.sqs' })}
                </LinkStandalone>
              )}
            </li>

            <li>
              <LinkStandalone
                shouldOpenInNewTab
                highlight={LinkHighlight.CurrentColor}
                to={COMMUNITY_FORUM_URL}
              >
                {intl.formatMessage({ id: 'footer.community' })}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                shouldOpenInNewTab
                highlight={LinkHighlight.CurrentColor}
                to={docUrl(DocLink.Root)}
              >
                {intl.formatMessage({ id: 'footer.documentation' })}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                shouldOpenInNewTab
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
