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
import userEvent from '@testing-library/user-event';
import { last } from 'lodash';
import React from 'react';
import { Route } from 'react-router-dom';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import ProjectLinksServiceMock from '../../../api/mocks/ProjectLinksServiceMock';
import { mockComponent } from '../../../helpers/mocks/component';
import { renderAppWithComponentContext } from '../../../helpers/testReactTestingUtils';
import ProjectLinksApp from '../ProjectLinksApp';

const componentsMock = new ProjectLinksServiceMock();

afterEach(() => {
  componentsMock.reset();
});

it('renders project links app and can do CRUD operations', async () => {
  const { ui } = getPageObjects();

  const newLinkName1 = 'link1';
  const newLinkName2 = 'issue';
  renderProjectLinksApp();
  await ui.appIsLoaded();

  expect(ui.noResultsTable.get()).toBeInTheDocument();

  // Create link
  await ui.createLink(newLinkName1, 'https://link.com');
  expect(ui.deleteLinkButton(newLinkName1).get()).toBeInTheDocument();
  expect(ui.noResultsTable.query()).not.toBeInTheDocument();

  // Create invalid link with provided type
  await ui.createLink(newLinkName2, 'invalidurl');
  expect(ui.deleteLinkButton(newLinkName2).query()).not.toBeInTheDocument();
  expect(byText('project_links.issue').get()).toBeInTheDocument();

  // Delete link
  await ui.deleteLink(newLinkName1);
  expect(ui.deleteLinkButton(newLinkName1).query()).not.toBeInTheDocument();
});

function renderProjectLinksApp() {
  return renderAppWithComponentContext(
    'project/links',
    () => <Route path="project/links" element={<ProjectLinksApp />} />,
    {},
    { component: mockComponent() },
  );
}

function getPageObjects() {
  const user = userEvent.setup();

  const ui = {
    pageTitle: byRole('heading', { name: 'project_links.page' }),
    noResultsTable: byText('project_links.no_results'),
    createLinkButton: byRole('button', { name: 'create' }),
    nameInput: byRole('textbox', { name: /project_links.name/ }),
    urlInput: byRole('textbox', { name: /project_links.url/ }),
    cancelDialogButton: byRole('button', { name: 'cancel' }),
    deleteLinkButton: (name: string) =>
      byRole('button', { name: `project_links.delete_x_link.${name}` }),
    deleteButton: byRole('button', { name: 'delete' }),
  };

  async function appIsLoaded() {
    expect(await ui.pageTitle.find()).toBeInTheDocument();
  }

  async function createLink(name: string, url: string) {
    await user.click(ui.createLinkButton.get());
    await user.type(ui.nameInput.get(), name);
    await user.type(ui.urlInput.get(), url);
    await user.click(last(ui.createLinkButton.getAll()) as HTMLElement);
  }

  async function deleteLink(name: string) {
    await user.click(ui.deleteLinkButton(name).get());
    await user.click(ui.deleteButton.get());
  }

  return {
    ui: { ...ui, appIsLoaded, createLink, deleteLink },
    user,
  };
}
