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
import * as React from 'react';
import { byLabelText, byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { ComponentQualifier } from '~sonar-aligned/types/component';
import { getAlmSettings } from '../../../../api/alm-settings';
import CurrentUserContextProvider from '../../../../app/components/current-user/CurrentUserContextProvider';
import { mockAppState, mockCurrentUser, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { AlmKeys } from '../../../../types/alm-settings';
import { FCProps } from '../../../../types/misc';
import { CurrentUser } from '../../../../types/users';
import PageHeader from '../PageHeader';

jest.mock('../../../../api/alm-settings', () => ({
  getAlmSettings: jest.fn().mockResolvedValue([]),
}));
jest.mock('../../../../api/components', () => ({
  searchProjects: jest.fn().mockResolvedValue({}),
}));

const ui = {
  buttonAddProject: byRole('button', { name: 'projects.add' }),
  buttonAddApplication: byRole('button', { name: 'projects.create_application' }),
  searchBar: byLabelText('search_verb'),
  selectPerspective: byLabelText('projects.perspective'),
  selectSort: byLabelText('projects.sort_by'),
  buttonSortProject: byLabelText('projects.sort_ascending'),
  projectNumber: byText(12),
  projectsText: byText('projects_'),
  buttonHomePage: byLabelText('homepage.check'),
  selectOptionAzure: byText('my_account.add_project.azure'),
  selectOptionAzureImage: byRole('img', { name: 'azure' }),
  selectOptionGitlab: byText('my_account.add_project.gitlab'),
  selectOptionGitlabImage: byRole('img', { name: 'gitlab' }),
  selectOptionBitbucket: byText('my_account.add_project.bitbucket'),
  selectOptionBitbucketCloud: byText('my_account.add_project.bitbucketcloud'),
  selectOptionManual: byText('my_account.add_project.manual'),
  selectOptionMore: byText('my_account.add_project.more_others'),
  selectOptionNewCode: byText('projects.view.new_code'),
  selectOptionAnalysisDate: byText('projects.sorting.analysis_date'),
  mandatoryFieldWarning: byText('fields_marked_with_x_required'),
};

beforeEach(() => {
  jest.clearAllMocks();
});

it('should work correctly for logged in user with edit permission', async () => {
  const user = userEvent.setup();
  const onQueryChangeMock = jest.fn();
  const onPerspectiveChangeMock = jest.fn();
  const onSortChangeMock = jest.fn();

  jest.mocked(getAlmSettings).mockResolvedValueOnce([
    { alm: AlmKeys.Azure, key: 'azure', url: 'https://azure.foo' },
    { alm: AlmKeys.GitLab, key: 'gitlab', url: 'https://gitlab.foo' },
  ]);

  renderPageHeader(
    {
      total: 12,
      onQueryChange: onQueryChangeMock,
      onPerspectiveChange: onPerspectiveChangeMock,
      onSortChange: onSortChangeMock,
    },
    mockLoggedInUser({ permissions: { global: ['admin', 'provisioning', 'applicationcreator'] } }),
  );
  expect(getAlmSettings).toHaveBeenCalled();
  expect(ui.buttonAddProject.get()).toBeInTheDocument();
  expect(ui.buttonAddApplication.get()).toBeInTheDocument();
  expect(ui.searchBar.get()).toBeInTheDocument();
  expect(ui.selectPerspective.get()).toBeInTheDocument();
  expect(ui.selectSort.get()).toBeInTheDocument();
  expect(ui.buttonSortProject.get()).toBeInTheDocument();
  expect(ui.projectNumber.get()).toBeInTheDocument();
  expect(ui.projectsText.get()).toBeInTheDocument();
  await expect(ui.buttonHomePage.get()).toHaveATooltipWithContent('homepage.check');

  // project creation
  await user.click(ui.buttonAddProject.get());
  expect(ui.selectOptionAzure.get()).toHaveAttribute('href', '/projects/create?mode=azure');
  expect(ui.selectOptionAzureImage.get()).toBeInTheDocument();
  expect(ui.selectOptionGitlab.get()).toHaveAttribute('href', '/projects/create?mode=gitlab');
  expect(ui.selectOptionGitlabImage.get()).toBeInTheDocument();
  expect(ui.selectOptionManual.get()).toHaveAttribute('href', '/projects/create?mode=manual');
  expect(ui.selectOptionMore.get()).toHaveAttribute('href', '/projects/create');

  // search projects
  await user.click(ui.searchBar.get());
  await user.keyboard('2');
  expect(onQueryChangeMock).toHaveBeenCalledWith({ search: 'test2' });

  // perpective select
  await user.click(ui.selectPerspective.get());
  await user.click(ui.selectOptionNewCode.get());
  expect(onPerspectiveChangeMock).toHaveBeenCalledWith({ view: 'leak' });

  // sort ascending
  await user.click(ui.buttonSortProject.get());
  expect(onSortChangeMock).toHaveBeenCalledWith('size', true);

  // sort select
  await user.click(ui.selectSort.get());
  await user.click(ui.selectOptionAnalysisDate.get());
  expect(onSortChangeMock).toHaveBeenCalledWith('analysis_date', false);
});

it('should work correctly for logged in user without edit permission', async () => {
  renderPageHeader({ total: 12 }, mockLoggedInUser());
  expect(getAlmSettings).not.toHaveBeenCalled();
  expect(ui.buttonAddProject.query()).not.toBeInTheDocument();
  expect(ui.buttonAddApplication.query()).not.toBeInTheDocument();
  expect(ui.searchBar.get()).toBeInTheDocument();
  expect(ui.selectPerspective.get()).toBeInTheDocument();
  expect(ui.selectSort.get()).toBeInTheDocument();
  expect(ui.buttonSortProject.get()).toBeInTheDocument();
  expect(ui.projectNumber.get()).toBeInTheDocument();
  expect(ui.projectsText.get()).toBeInTheDocument();
  await expect(ui.buttonHomePage.get()).toHaveATooltipWithContent('homepage.check');
});

it('should work correctly for anonymous user', () => {
  renderPageHeader({ total: 12 }, mockCurrentUser());
  expect(getAlmSettings).not.toHaveBeenCalled();
  expect(ui.buttonAddProject.query()).not.toBeInTheDocument();
  expect(ui.buttonAddApplication.query()).not.toBeInTheDocument();
  expect(ui.searchBar.get()).toBeInTheDocument();
  expect(ui.selectPerspective.get()).toBeInTheDocument();
  expect(ui.selectSort.get()).toBeInTheDocument();
  expect(ui.buttonSortProject.get()).toBeInTheDocument();
  expect(ui.projectNumber.get()).toBeInTheDocument();
  expect(ui.projectsText.get()).toBeInTheDocument();
  expect(ui.buttonHomePage.query()).not.toBeInTheDocument();
});

it('should not render total if not defined', () => {
  renderPageHeader({ total: undefined }, mockCurrentUser());
  expect(ui.projectsText.query()).not.toBeInTheDocument();
});

it('should render alm correctly even with wrong data', async () => {
  const user = userEvent.setup();

  jest.mocked(getAlmSettings).mockResolvedValueOnce([
    { alm: AlmKeys.Azure, key: 'azure1' },
    { alm: AlmKeys.Azure, key: 'azure2' },
    { alm: AlmKeys.BitbucketServer, url: 'b1', key: 'bbs' },
    { alm: AlmKeys.BitbucketCloud, key: 'bbc' },
    { alm: AlmKeys.GitLab, key: 'gitlab', url: 'https://gitlab.foo' },
  ]);

  renderPageHeader(
    {},
    mockLoggedInUser({ permissions: { global: ['admin', 'provisioning', 'applicationcreator'] } }),
  );

  await user.click(ui.buttonAddProject.get());
  expect(ui.selectOptionAzure.query()).not.toBeInTheDocument();
  expect(ui.selectOptionGitlab.get()).toHaveAttribute('href', '/projects/create?mode=gitlab');
  expect(ui.selectOptionBitbucket.get()).toHaveAttribute('href', '/projects/create?mode=bitbucket');
  expect(ui.selectOptionBitbucketCloud.get()).toHaveAttribute(
    'href',
    '/projects/create?mode=bitbucketcloud',
  );
});

function renderPageHeader(
  props: Partial<FCProps<typeof PageHeader>> = {},
  currentUser: CurrentUser = mockLoggedInUser(),
) {
  return renderComponent(
    <CurrentUserContextProvider currentUser={currentUser}>
      <PageHeader
        currentUser={currentUser}
        onPerspectiveChange={jest.fn()}
        onQueryChange={jest.fn()}
        onSortChange={jest.fn()}
        query={{ search: 'test' }}
        selectedSort="size"
        view="overall"
        {...props}
      />
    </CurrentUserContextProvider>,
    '/',
    { appState: mockAppState({ qualifiers: [ComponentQualifier.Application] }) },
  );
}
