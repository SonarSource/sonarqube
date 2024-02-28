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
import {
  FlagMessage,
  LAYOUT_VIEWPORT_MIN_WIDTH,
  PageContentFontWrapper,
  themeBorder,
  themeColor,
} from 'design-system';
import * as React from 'react';
import InstanceMessage from '../../components/common/InstanceMessage';
import { useDocUrl } from '../../helpers/docs';
import { getEdition } from '../../helpers/editions';
import { translate, translateWithParameters } from '../../helpers/l10n';
import GlobalFooterBranding from './GlobalFooterBranding';
import { AppStateContext } from './app-state/AppStateContext';

interface GlobalFooterProps {
  hideLoggedInInfo?: boolean;
}

export default function GlobalFooter({ hideLoggedInInfo }: Readonly<GlobalFooterProps>) {
  const appState = React.useContext(AppStateContext);
  const currentEdition = appState?.edition && getEdition(appState.edition);

  const docUrl = useDocUrl();

  return (
    <StyledFooter className="sw-p-6" id="footer">
      <PageContentFontWrapper className="sw-body-sm sw-h-full sw-flex sw-flex-col sw-items-stretch">
        {appState?.productionDatabase === false && (
          <FlagMessage className="sw-mb-4" id="evaluation_warning" variant="warning">
            <p>
              <span className="sw-body-md-highlight">
                {translate('footer.production_database_warning')}
              </span>

              <br />

              <InstanceMessage message={translate('footer.production_database_explanation')} />
            </p>
          </FlagMessage>
        )}

        <div className="sw-flex sw-justify-between sw-items-center">
          <GlobalFooterBranding />

          <ul className="sw-flex sw-items-center sw-gap-3 sw-ml-4">
            {!hideLoggedInInfo && currentEdition && <li>{currentEdition.name}</li>}

            {!hideLoggedInInfo && appState?.version && (
              <li className="sw-code">
                {translateWithParameters('footer.version_x', appState.version)}
              </li>
            )}

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to="https://www.gnu.org/licenses/lgpl-3.0.txt"
              >
                {translate('footer.license')}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to="https://community.sonarsource.com/c/help/sq"
              >
                {translate('footer.community')}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone highlight={LinkHighlight.CurrentColor} to={docUrl('/')}>
                {translate('footer.documentation')}
              </LinkStandalone>
            </li>

            <li>
              <LinkStandalone
                highlight={LinkHighlight.CurrentColor}
                to={docUrl('/instance-administration/plugin-version-matrix/')}
              >
                {translate('footer.plugins')}
              </LinkStandalone>
            </li>

            {!hideLoggedInInfo && (
              <li>
                <LinkStandalone highlight={LinkHighlight.CurrentColor} to="/web_api">
                  {translate('footer.web_api')}
                </LinkStandalone>
              </li>
            )}
          </ul>
        </div>
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
