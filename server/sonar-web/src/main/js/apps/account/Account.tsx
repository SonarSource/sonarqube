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

import { LargeCenteredLayout, PageContentFontWrapper, TopBar } from 'design-system';
import * as React from 'react';
import { createPortal } from 'react-dom';
import { Helmet } from 'react-helmet-async';
import { Outlet } from 'react-router-dom';
import A11ySkipTarget from '~sonar-aligned/components/a11y/A11ySkipTarget';
import { useCurrentLoginUser } from '../../app/components/current-user/CurrentUserContext';
import Suggestions from '../../components/embed-docs-modal/Suggestions';
import { translate, translateWithParameters } from '../../helpers/l10n';
import Nav from './components/Nav';
import UserCard from './components/UserCard';

export default function Account() {
  const currentUser = useCurrentLoginUser();
  const [portalAnchor, setPortalAnchor] = React.useState<Element | null>(null);

  // Set portal anchor on mount
  React.useEffect(() => {
    setPortalAnchor(document.getElementById('component-nav-portal'));
  }, []);

  const title = translate('my_account.page');

  return (
    <div id="account-page">
      {portalAnchor &&
        createPortal(
          <header>
            <TopBar>
              <div className="sw-flex sw-items-center sw-gap-2 sw-pb-4">
                <UserCard user={currentUser} />
              </div>

              <Nav />
            </TopBar>
          </header>,
          portalAnchor,
        )}

      <LargeCenteredLayout as="main">
        <PageContentFontWrapper className="sw-body-sm sw-py-8">
          <Suggestions suggestions="account" />

          <Helmet
            defaultTitle={title}
            defer={false}
            titleTemplate={translateWithParameters(
              'page_title.template.with_category',
              translate('my_account.page'),
            )}
          />

          <A11ySkipTarget anchor="account_main" />

          <Outlet />
        </PageContentFontWrapper>
      </LargeCenteredLayout>
    </div>
  );
}
