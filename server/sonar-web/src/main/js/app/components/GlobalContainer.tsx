/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import classNames from 'classnames';
import { lightTheme, ToastMessageContainer } from 'design-system';
import * as React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import A11yProvider from '../../components/a11y/A11yProvider';
import A11ySkipLinks from '../../components/a11y/A11ySkipLinks';
import SuggestionsProvider from '../../components/embed-docs-modal/SuggestionsProvider';
import NCDAutoUpdateMessage from '../../components/new-code-definition/NCDAutoUpdateMessage';
import Workspace from '../../components/workspace/Workspace';
import GlobalFooter from './GlobalFooter';
import IndexationContextProvider from './indexation/IndexationContextProvider';
import IndexationNotification from './indexation/IndexationNotification';
import LanguagesContextProvider from './languages/LanguagesContextProvider';
import MetricsContextProvider from './metrics/MetricsContextProvider';
import GlobalNav from './nav/global/GlobalNav';
import PromotionNotification from './promotion-notification/PromotionNotification';
import StartupModal from './StartupModal';
import SystemAnnouncement from './SystemAnnouncement';
import UpdateNotification from './update-notification/UpdateNotification';

const TEMP_PAGELIST_WITH_NEW_BACKGROUND = [
  '/dashboard',
  '/security_hotspots',
  '/component_measures',
  '/project/issues',
  '/project/activity',
  '/code',
  '/profiles',
  '/project/extension/securityreport/securityreport',
  '/projects',
  '/project/information',
  '/web_api_v2',
  '/quality_gates',
  '/coding_rules',
];

const TEMP_PAGELIST_WITH_NEW_BACKGROUND_WHITE = [
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
];

export default function GlobalContainer() {
  // it is important to pass `location` down to `GlobalNav` to trigger render on url change
  const location = useLocation();

  return (
    <ThemeProvider theme={lightTheme}>
      <SuggestionsProvider>
        <A11yProvider>
          <A11ySkipLinks />
          <div className="global-container">
            <div
              className={classNames('page-wrapper', {
                'new-background': TEMP_PAGELIST_WITH_NEW_BACKGROUND.some((element) =>
                  location.pathname.startsWith(element),
                ),
                'white-background': TEMP_PAGELIST_WITH_NEW_BACKGROUND_WHITE.includes(
                  location.pathname,
                ),
              })}
              id="container"
            >
              <div className="page-container">
                <Workspace>
                  <ToastMessageContainer />
                  <IndexationContextProvider>
                    <LanguagesContextProvider>
                      <MetricsContextProvider>
                        <div className="sw-sticky sw-top-0 sw-z-global-navbar">
                          <SystemAnnouncement />
                          <IndexationNotification />
                          <NCDAutoUpdateMessage />
                          <UpdateNotification dismissable />
                          <GlobalNav location={location} />
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
              </div>
              <PromotionNotification />
            </div>
            <GlobalFooter />
          </div>
          <StartupModal />
        </A11yProvider>
      </SuggestionsProvider>
    </ThemeProvider>
  );
}
