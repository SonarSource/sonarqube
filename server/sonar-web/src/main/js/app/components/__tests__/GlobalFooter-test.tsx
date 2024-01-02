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
import { mockAppState } from '../../../helpers/testMocks';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole, byText } from '../../../helpers/testSelector';
import { AppState } from '../../../types/appstate';
import { EditionKey } from '../../../types/editions';
import { FCProps } from '../../../types/misc';
import GlobalFooter from '../GlobalFooter';

it('should render the logged-in information', () => {
  renderGlobalFooter();

  expect(ui.databaseWarningMessage.query()).not.toBeInTheDocument();

  expect(ui.footerListItems.getAll()).toHaveLength(7);

  expect(byText('Community Edition').get()).toBeInTheDocument();
  expect(ui.versionLabel('4.2').get()).toBeInTheDocument();
  expect(ui.apiLink.get()).toBeInTheDocument();
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
    version ? byText(`footer.version_x.${version}`) : byText(/footer\.version_x/),

  // links
  websiteLink: byRole('link', { name: 'SonarQube™' }),
  companyLink: byRole('link', { name: 'SonarSource SA' }),
  licenseLink: byRole('link', { name: 'footer.license' }),
  communityLink: byRole('link', { name: 'footer.community' }),
  docsLink: byRole('link', { name: 'opens_in_new_window footer.documentation' }),
  pluginsLink: byRole('link', { name: 'opens_in_new_window footer.plugins' }),
  apiLink: byRole('link', { name: 'footer.web_api' }),
};
