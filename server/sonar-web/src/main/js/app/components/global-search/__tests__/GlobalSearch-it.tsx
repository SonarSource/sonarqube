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
import React from 'react';
import { byRole, byText } from '~sonar-aligned/helpers/testSelector';
import { getSuggestions } from '../../../../api/components';
import { mockRouter } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import GlobalSearch, { GlobalSearch as GlobalSearchWithoutRouter } from '../GlobalSearch';

jest.mock('../../../../api/components', () => ({
  getSuggestions: jest.fn().mockResolvedValue({
    results: [
      {
        q: 'TRK',
        more: 1,
        items: [
          {
            isFavorite: true,
            isRecentlyBrowsed: true,
            key: 'sonarqube',
            match: 'SonarQube',
            name: 'SonarQube',
            project: '',
          },
          {
            isFavorite: false,
            isRecentlyBrowsed: false,
            key: 'sonarcloud',
            match: 'Sonarcloud',
            name: 'Sonarcloud',
            project: '',
          },
        ],
      },
    ],
  }),
}));

const ui = {
  searchButton: byRole('button', { name: 'search_verb' }),
  searchInput: byRole('searchbox'),
  searchItemListWrapper: byRole('menu'),
  searchItem: byRole('menuitem'),
  showMoreButton: byRole('menuitem', { name: 'show_more' }),
  tooShortWarning: byText('select2.tooShort.2'),
  noResultTextABCD: byText('no_results_for_x.abcd'),
};

it('should show the input when user click on the search icon', async () => {
  const user = userEvent.setup();
  renderGlobalSearch();

  expect(ui.searchButton.get()).toBeInTheDocument();
  await user.click(ui.searchButton.get());
  expect(ui.searchInput.get()).toBeVisible();
  expect(ui.searchItemListWrapper.get()).toBeVisible();

  await user.click(document.body);
  expect(ui.searchInput.query()).not.toBeInTheDocument();
  expect(ui.searchItemListWrapper.query()).not.toBeInTheDocument();
});

it('selects the results', async () => {
  const user = userEvent.setup();
  renderGlobalSearch();
  await user.click(ui.searchButton.get());

  await user.click(ui.searchInput.get());
  await user.keyboard('son');
  expect(ui.searchItem.getAll()[1]).toHaveClass('active');
  expect(ui.searchItem.getAll()[1]).toHaveTextContent('SonarQubesonarqube');

  await user.keyboard('{arrowdown}');
  expect(ui.searchItem.getAll()[2]).toHaveClass('active');
  expect(ui.searchItem.getAll()[2]).toHaveTextContent('Sonarcloudsonarcloud');

  await user.keyboard('{arrowdown}');
  expect(ui.searchItem.getAll()[3]).toHaveClass('active');
  expect(ui.searchItem.getAll()[3]).toHaveTextContent('show_more');

  await user.keyboard('{arrowup}');
  expect(ui.searchItem.getAll()[2]).toHaveClass('active');
  expect(ui.searchItem.getAll()[2]).toHaveTextContent('Sonarcloudsonarcloud');

  await user.hover(ui.searchItem.getAll()[1]);
  expect(ui.searchItem.getAll()[1]).toHaveClass('active');

  await user.keyboard('{Escape}');
  expect(ui.searchInput.query()).not.toBeInTheDocument();
});

it('load more results', async () => {
  const user = userEvent.setup();
  renderGlobalSearch();
  await user.click(ui.searchButton.get());
  expect(getSuggestions).toHaveBeenCalledWith('', []);

  await user.click(ui.searchInput.get());
  await user.keyboard('foo');
  expect(getSuggestions).toHaveBeenLastCalledWith('foo', []);

  jest.mocked(getSuggestions).mockResolvedValueOnce({
    projects: [],
    results: [
      {
        items: [
          {
            isFavorite: false,
            isRecentlyBrowsed: false,
            key: 'bar',
            match: '<mark>Bar</mark>',
            name: 'Bar',
            project: 'bar',
          },
        ],
        more: 0,
        q: 'TRK',
      },
    ],
  });

  await user.click(ui.showMoreButton.get());
  expect(getSuggestions).toHaveBeenLastCalledWith('foo', [], 'TRK');
  expect(ui.searchItem.getAll()[3]).toHaveTextContent('Barbar');
});

it('shows warning about short input', async () => {
  const user = userEvent.setup();
  renderGlobalSearch();
  await user.click(ui.searchButton.get());

  await user.click(ui.searchInput.get());
  await user.keyboard('s');
  expect(ui.tooShortWarning.get()).toBeVisible();

  await user.keyboard('abc');
  expect(ui.tooShortWarning.query()).not.toBeInTheDocument();
});

it('should display no results message', async () => {
  const user = userEvent.setup();
  renderGlobalSearch();
  (getSuggestions as jest.Mock).mockResolvedValue({
    results: [
      {
        items: [],
        more: 0,
        q: 'TRK',
      },
    ],
  });

  await user.click(ui.searchButton.get());

  await user.click(ui.searchInput.get());
  await user.keyboard('abcd');

  expect(ui.noResultTextABCD.get()).toBeVisible();
});

it('should open selected', async () => {
  (getSuggestions as jest.Mock).mockResolvedValueOnce({
    results: [
      {
        items: [
          {
            isFavorite: true,
            isRecentlyBrowsed: true,
            key: 'sonarqube',
            match: 'SonarQube',
            name: 'SonarQube',
            project: '',
          },
        ],
        more: 0,
        q: 'TRK',
      },
    ],
  });
  const user = userEvent.setup();
  const router = mockRouter();
  renderComponent(<GlobalSearchWithoutRouter router={router} />);
  await user.click(ui.searchButton.get());

  await user.click(ui.searchInput.get());
  await user.keyboard('{arrowdown}');
  await user.keyboard('{enter}');
  expect(router.push).toHaveBeenCalledWith({
    pathname: '/dashboard',
    search: '?id=sonarqube',
  });
});

function renderGlobalSearch() {
  return renderComponent(<GlobalSearch />);
}
