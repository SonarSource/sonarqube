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
import { addDays, subDays } from 'date-fns';
import * as React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import SystemServiceMock from '../../../api/mocks/SystemServiceMock';
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { AppState } from '../../../types/appstate';
import { EditionKey } from '../../../types/editions';
import { FCProps } from '../../../types/misc';
import GlobalFooter from '../GlobalFooter';

const systemMock = new SystemServiceMock();

afterEach(() => {
  systemMock.reset();
});

it('should render the logged-in information', async () => {
  renderGlobalFooter();

  expect(ui.databaseWarningMessage.query()).not.toBeInTheDocument();

  expect(ui.footerListItems.getAll()).toHaveLength(7);

  expect(byText('Community Edition').get()).toBeInTheDocument();
  expect(ui.versionLabel('4.2').get()).toBeInTheDocument();
  expect(await ui.ltaDocumentationLinkActive.find()).toBeInTheDocument();
  expect(ui.apiLink.get()).toBeInTheDocument();
});

it('should render the inactive version and cleanup build number', async () => {
  systemMock.setSystemUpgrades({ installedVersionActive: false });
  renderGlobalFooter({}, { version: '4.2 (build 12345)' });

  expect(ui.versionLabel('4.2.12345').get()).toBeInTheDocument();
  expect(await ui.ltaDocumentationLinkInactive.find()).toBeInTheDocument();
});

it('should show active status if offline and did not reach EOL', async () => {
  systemMock.setSystemUpgrades({ installedVersionActive: undefined });
  renderGlobalFooter(
    {},
    { version: '4.2 (build 12345)', versionEOL: addDays(new Date(), 10).toISOString() },
  );

  expect(await ui.ltaDocumentationLinkActive.find()).toBeInTheDocument();
});

it('should show inactive status if offline and reached EOL', async () => {
  systemMock.setSystemUpgrades({ installedVersionActive: undefined });
  renderGlobalFooter(
    {},
    { version: '4.2 (build 12345)', versionEOL: subDays(new Date(), 10).toISOString() },
  );

  expect(await ui.ltaDocumentationLinkInactive.find()).toBeInTheDocument();
});

it('should not render missing logged-in information', () => {
  renderGlobalFooter({}, { edition: undefined, version: '' });

  expect(ui.footerListItems.getAll()).toHaveLength(5);

  expect(byText('Community Edition').query()).not.toBeInTheDocument();
  expect(ui.versionLabel().query()).not.toBeInTheDocument();
});

it('should not render the logged-in information', () => {
  renderGlobalFooter({ hideLoggedInInfo: true });

  expect(ui.databaseWarningMessage.query()).not.toBeInTheDocument();

  expect(ui.footerListItems.getAll()).toHaveLength(4);

  expect(byText('Community Edition').query()).not.toBeInTheDocument();
  expect(ui.versionLabel().query()).not.toBeInTheDocument();
  expect(ui.apiLink.query()).not.toBeInTheDocument();
});

it('should show the db warning message', () => {
  renderGlobalFooter({}, { productionDatabase: false });

  expect(ui.databaseWarningMessage.get()).toBeInTheDocument();
});

function renderGlobalFooter(
  props: Partial<FCProps<typeof GlobalFooter>> = {},
  appStateOverride: Partial<AppState> = {},
) {
  return renderComponent(<GlobalFooter {...props} />, '/', {
    appState: mockAppState({
      productionDatabase: true,
      edition: EditionKey.community,
      version: '4.2',
      ...appStateOverride,
    }),
  });
}

const ui = {
  footerListItems: byRole('listitem'),
  databaseWarningMessage: byText('footer.production_database_warning'),

  versionLabel: (version?: string) =>
    version ? byText(/footer\.version\.*(\d.\d)/) : byText(/footer\.version/),

  // links
  websiteLink: byRole('link', { name: 'SonarQube™' }),
  companyLink: byRole('link', { name: 'SonarSource SA' }),
  licenseLink: byRole('link', { name: 'footer.license' }),
  communityLink: byRole('link', { name: 'footer.community' }),
  docsLink: byRole('link', { name: 'opens_in_new_window footer.documentation' }),
  pluginsLink: byRole('link', { name: 'opens_in_new_window footer.plugins' }),
  apiLink: byRole('link', { name: 'footer.web_api' }),
  ltaDocumentationLinkActive: byRole('link', {
    name: `footer.version.status.active open_in_new_window`,
  }),
  ltaDocumentationLinkInactive: byRole('link', {
    name: `footer.version.status.inactive open_in_new_window`,
  }),
};
