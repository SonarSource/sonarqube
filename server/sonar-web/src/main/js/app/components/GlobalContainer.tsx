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

import { ThemeProvider } from '@emotion/react';
import styled from '@emotion/styled';
import { Outlet, useLocation } from 'react-router-dom';
import { lightTheme, themeColor } from '~design-system';
import A11yProvider from '~sonar-aligned/components/a11y/A11yProvider';
import A11ySkipLinks from '~sonar-aligned/components/a11y/A11ySkipLinks';
import SuggestionsProvider from '../../components/embed-docs-modal/SuggestionsProvider';
import NCDAutoUpdateMessage from '../../components/new-code-definition/NCDAutoUpdateMessage';
import Workspace from '../../components/workspace/Workspace';
import GlobalFooterCodescan from './GlobalFooterCodescan';
import CalculationChangeMessage from './calculation-notification/CalculationChangeMessage';
import IndexationContextProvider from './indexation/IndexationContextProvider';
import IndexationNotification from './indexation/IndexationNotification';
import LanguagesContextProvider from './languages/LanguagesContextProvider';
import MetricsContextProvider from './metrics/MetricsContextProvider';
import ModeTour from './ModeTour';
import GlobalNav from './nav/global/GlobalNav';
import StartupModal from './StartupModal';
import SystemAnnouncement from './SystemAnnouncement';
import { UpdateNotification } from './update-notification/UpdateNotification';
import BranchStatusContextProvider from './branch-status/BranchStatusContextProvider';

/*
 * These pages need a white background (aka 'secondary', rather than the default 'primary')
 * This should be revisited at some point (why the exception?)
 */
const PAGES_WITH_SECONDARY_BACKGROUND = [
  '/tutorials',
  '/projects/create',
  '/project/baseline',
  '/project/branches',
  '/project/key',
  '/project/deletion',
  '/project/links',
  '/project/import_export',
  '/project/quality_gate',
  '/project/quality_profiles',
  '/project/webhooks',
  '/admin/webhooks',
  '/project_roles',
  '/admin/permissions',
  '/admin/permission_templates',
  '/project/background_tasks',
  '/admin/background_tasks',
  '/admin/groups',
  '/admin/marketplace',
  '/admin/system',
  '/admin/users',
  '/admin/settings',
  '/admin/settings/encryption',
  '/admin/extension/license/support',
  '/admin/audit',
  '/admin/projects_management',
  '/account/projects',
];

export default function GlobalContainer() {
  // it is important to pass `location` down to `GlobalNav` to trigger render on url change
  const location = useLocation();

  return (
    <ThemeProvider theme={lightTheme}>
      <SuggestionsProvider>
        <A11yProvider>
          <A11ySkipLinks />
          <GlobalContainerWrapper>
            <GlobalBackground
              secondary={PAGES_WITH_SECONDARY_BACKGROUND.includes(location.pathname)}
              className="sw-box-border sw-flex-[1_0_auto]"
              id="container"
            >
              <BranchStatusContextProvider>
                <Workspace>
                  <IndexationContextProvider>
                    <LanguagesContextProvider>
                      <MetricsContextProvider>
                        <div className="sw-sticky sw-top-0 sw-z-global-navbar">
                          <SystemAnnouncement />
                          <IndexationNotification />
                          <NCDAutoUpdateMessage />
                          <UpdateNotification dismissable />
                          <GlobalNav location={location} />
                          <ModeTour />
                          <CalculationChangeMessage />
                          {/* The following is the portal anchor point for the component nav
                          * See ComponentContainer.tsx
                          */}
                          <div id="component-nav-portal" />
                        </div>
                        <Outlet />
                      </MetricsContextProvider>
                    </LanguagesContextProvider>
                  </IndexationContextProvider>
                </Workspace>
              </BranchStatusContextProvider>
            </GlobalBackground>
            <GlobalFooterCodescan />
          </GlobalContainerWrapper>
          <StartupModal />
        </A11yProvider>
      </SuggestionsProvider>
    </ThemeProvider>
  );
}

const GlobalContainerWrapper = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 100vh;
`;

const GlobalBackground = styled.div<{ secondary: boolean }>`
  background-color: ${({ secondary }) =>
    themeColor(secondary ? 'backgroundSecondary' : 'backgroundPrimary')};
`;
