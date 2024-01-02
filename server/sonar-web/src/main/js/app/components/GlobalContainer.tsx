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
import { Outlet, useLocation } from 'react-router-dom';
import A11yProvider from '../../components/a11y/A11yProvider';
import A11ySkipLinks from '../../components/a11y/A11ySkipLinks';
import SuggestionsProvider from '../../components/embed-docs-modal/SuggestionsProvider';
import Workspace from '../../components/workspace/Workspace';
import BranchStatusContextProvider from './branch-status/BranchStatusContextProvider';
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

export default function GlobalContainer() {
  // it is important to pass `location` down to `GlobalNav` to trigger render on url change
  const location = useLocation();

  return (
    <SuggestionsProvider>
      <A11yProvider>
        <StartupModal>
          <A11ySkipLinks />
          <div className="global-container">
            <div className="page-wrapper" id="container">
              <div className="page-container">
                <BranchStatusContextProvider>
                  <Workspace>
                    <IndexationContextProvider>
                      <LanguagesContextProvider>
                        <MetricsContextProvider>
                          <SystemAnnouncement />
                          <IndexationNotification />
                          <UpdateNotification dismissable={true} />
                          <GlobalNav location={location} />
                          <Outlet />
                        </MetricsContextProvider>
                      </LanguagesContextProvider>
                    </IndexationContextProvider>
                  </Workspace>
                </BranchStatusContextProvider>
              </div>
              <PromotionNotification />
            </div>
            <GlobalFooter />
          </div>
        </StartupModal>
      </A11yProvider>
    </SuggestionsProvider>
  );
}
