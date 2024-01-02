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
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { CurrentUserContext } from '../../../../app/components/current-user/CurrentUserContext';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { CurrentUser } from '../../../../types/users';
import PageSidebar, { PageSidebarProps } from '../PageSidebar';

it('should render the right facets for overview', () => {
  renderPageSidebar({
    query: { size: '3' },
  });

  expect(screen.getByRole('heading', { level: 3, name: 'metric_domain.Size' })).toBeInTheDocument();

  expect(
    screen.getByRole('heading', { level: 3, name: 'projects.facets.qualifier' })
  ).toBeInTheDocument();

  expect(
    screen.queryByRole('heading', { level: 3, name: 'projects.facets.new_lines' })
  ).not.toBeInTheDocument();
});

it('should not show the qualifier facet with no applications', () => {
  renderPageSidebar({
    applicationsEnabled: false,
    query: { size: '3' },
  });

  expect(
    screen.queryByRole('heading', { level: 3, name: 'projects.facets.qualifier' })
  ).not.toBeInTheDocument();
});

it('should show "new lines" instead of "size" when in `leak` view', () => {
  renderPageSidebar({
    query: { view: 'leak' },
    view: 'leak',
  });

  expect(
    screen.queryByRole('heading', { level: 3, name: 'metric_domain.Size' })
  ).not.toBeInTheDocument();

  expect(
    screen.getByRole('heading', { level: 3, name: 'projects.facets.new_lines' })
  ).toBeInTheDocument();
});

it('should allow to clear all filters', async () => {
  const user = userEvent.setup();
  const onClearAll = jest.fn();
  renderPageSidebar({
    onClearAll,
    applicationsEnabled: false,
    query: { size: '3', reliability: '2' },
  });

  const clearAllButton = screen.getByRole('button', { name: 'clear_all_filters' });
  expect(clearAllButton).toBeInTheDocument();

  await user.click(clearAllButton);

  expect(onClearAll).toHaveBeenCalled();

  expect(screen.getByRole('heading', { level: 2, name: 'filters' })).toHaveFocus();
});

function renderPageSidebar(overrides: Partial<PageSidebarProps> = {}, currentUser?: CurrentUser) {
  return renderComponent(
    <CurrentUserContext.Provider
      value={{
        currentUser: currentUser ?? mockCurrentUser(),
        updateCurrentUserHomepage: jest.fn(),
        updateDismissedNotices: jest.fn(),
      }}
    >
      <PageSidebar
        applicationsEnabled={true}
        onClearAll={jest.fn()}
        onQueryChange={jest.fn()}
        query={{ view: 'overall' }}
        view="overall"
        {...overrides}
      />
    </CurrentUserContext.Provider>
  ).container;
}
