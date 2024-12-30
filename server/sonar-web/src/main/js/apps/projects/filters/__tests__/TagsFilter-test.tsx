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
import { searchProjectTags } from '../../../../api/components';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import TagsFacet from '../TagsFilter';

jest.mock('../../../../api/components', () => ({
  searchProjectTags: jest.fn(),
}));

it('renders language names', () => {
  renderTagsFacet();
  expect(screen.getByText('style')).toBeInTheDocument();
  expect(screen.getByText('custom1')).toBeInTheDocument();
  expect(screen.getByText('cheese')).toBeInTheDocument();
});

it('filters options', async () => {
  const user = userEvent.setup();

  jest.mocked(searchProjectTags).mockResolvedValueOnce({ tags: ['style', 'stunt'] });
  const loadSearchResultCount = jest.fn().mockResolvedValueOnce({ style: 3, stunt: 0 });

  renderTagsFacet({ loadSearchResultCount });

  await user.click(screen.getByLabelText('search_verb'));

  await user.keyboard('st');

  expect(screen.getByTitle('style')).toBeInTheDocument();
  expect(screen.getByTitle('stunt')).toBeInTheDocument();
  expect(screen.queryByTitle('cheese')).not.toBeInTheDocument();
  expect(screen.queryByTitle('custom1')).not.toBeInTheDocument();
});

it('updates the filter query', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderTagsFacet({ onQueryChange });

  await user.click(screen.getByText('style'));

  expect(onQueryChange).toHaveBeenCalledWith({ tags: 'style' });
});

it('handles multiselection', async () => {
  const user = userEvent.setup();

  const onQueryChange = jest.fn();

  renderTagsFacet({ onQueryChange });

  await user.keyboard('{Control>}');
  await user.click(screen.getByText('style'));
  await user.keyboard('{/Control}');

  expect(onQueryChange).toHaveBeenCalledWith({ tags: 'custom1,style' });
});

function renderTagsFacet(props: Partial<TagsFacet['props']> = {}) {
  renderComponent(
    <TagsFacet
      loadSearchResultCount={jest.fn()}
      onQueryChange={jest.fn()}
      query={{}}
      facet={{ cheese: 5, style: 3, custom1: 1 }}
      value={['custom1']}
      {...props}
    />,
  );
}
