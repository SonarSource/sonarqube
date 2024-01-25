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
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { AutoSizerProps } from 'react-virtualized';
import { ProjectsServiceMock } from '../../../../api/mocks/ProjectsServiceMock';
import { save } from '../../../../helpers/storage';
import { mockAppState, mockLoggedInUser } from '../../../../helpers/testMocks';
import { renderAppRoutes } from '../../../../helpers/testReactTestingUtils';
import { byLabelText, byRole, byText } from '../../../../helpers/testSelector';
import { ComponentQualifier } from '../../../../types/component';
import { MetricKey } from '../../../../types/metrics';
import { Dict } from '../../../../types/types';
import projectRoutes from '../../routes';
import { LS_PROJECTS_SORT, LS_PROJECTS_VIEW } from '../AllProjects';

/* Mock the Autosizer to always render the whole list */
jest.mock('react-virtualized/dist/commonjs/AutoSizer', () => {
  function AutoSizer(props: AutoSizerProps) {
    return <>{props.children({ height: 10000, width: 1000 })}</>;
  }

  return { AutoSizer };
});

jest.mock('../../../../api/components');
jest.mock('../../../../api/measures');
jest.mock('../../../../api/favorites');

jest.mock('../../../../helpers/storage', () => {
  const fakeStorage: Dict<string> = {
    'sonarqube.projects.default': 'all',
  };

  return {
    get: jest.fn((key: string) => fakeStorage[key]),
    save: jest.fn((key: string, value: string) => {
      fakeStorage[key] = value;
    }),
  };
});

// eslint-disable-next-line local-rules/use-metrickey-enum
const BASE_PATH = 'projects';

const projectHandler = new ProjectsServiceMock();

beforeEach(() => {
  jest.clearAllMocks();
  projectHandler.reset();
});

it('renders correctly', async () => {
  renderProjects(`${BASE_PATH}?gate=OK`);

  expect(await ui.projects.findAll()).toHaveLength(20);
});

it('changes sort and perspective', async () => {
  const user = userEvent.setup();
  renderProjects();

  await user.click(ui.sortSelect.get());
  await user.click(screen.getByText('projects.sorting.size'));

  const projects = ui.projects.getAll();

  expect(await within(projects[0]).findByRole('link')).toHaveTextContent(
    'sonarlint-omnisharp-dotnet',
  );

  // Change perspective
  await user.click(ui.perspectiveSelect.get());
  await user.click(screen.getByText('projects.view.new_code'));

  // each project should show "new bugs" instead of "bugs"
  expect(await screen.findAllByText(`metric.${MetricKey.new_violations}.description`)).toHaveLength(
    20,
  );

  expect(save).toHaveBeenCalledWith(LS_PROJECTS_VIEW, 'leak');
  // sort should also be updated
  expect(save).toHaveBeenCalledWith(LS_PROJECTS_SORT, MetricKey.new_lines);
});

it('handles showing favorite projects on load', async () => {
  const user = userEvent.setup();
  renderProjects(`${BASE_PATH}/favorite`);

  expect(ui.myFavoritesToggleOption.get()).toHaveAttribute('aria-current', 'true');
  expect(await ui.projects.findAll()).toHaveLength(2);

  await user.click(ui.allToggleOption.get());

  expect(ui.projects.getAll()).toHaveLength(20);
});

function renderProjects(navigateTo?: string) {
  return renderAppRoutes(BASE_PATH, projectRoutes, {
    appState: mockAppState({
      qualifiers: [ComponentQualifier.Project, ComponentQualifier.Application],
    }),
    currentUser: mockLoggedInUser({ dismissedNotices: {} }),
    navigateTo,
  });
}

const ui = {
  loading: byText('loading'),
  myFavoritesToggleOption: byRole('radio', { name: 'my_favorites' }),
  allToggleOption: byRole('radio', { name: 'all' }),
  projects: byRole('row'),
  perspectiveSelect: byLabelText('projects.perspective'),
  sortSelect: byLabelText('projects.sort_by'),
};
