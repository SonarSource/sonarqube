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

/* eslint-disable import/no-extraneous-dependencies */

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { renderWithContext } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { MultiSelector } from '../MultiSelector';
import { Tags } from '../Tags';
import { Tooltip } from '../Tooltip';

it('should display "no tags"', () => {
  renderTags({ tags: [] });

  expect(screen.getByText('no tags')).toBeInTheDocument();
});

it('should display tags', () => {
  const tags = ['tag1', 'tag2'];
  renderTags({ tags });

  expect(screen.getByText('tag1')).toBeInTheDocument();
  expect(screen.getByText('tag2')).toBeInTheDocument();
});

it('should handle more tags than the max to display', () => {
  const tags = ['tag1', 'tag2', 'tag3'];
  renderTags({ tags, tagsToDisplay: 2 });

  expect(screen.getByText('tag1')).toBeInTheDocument();
  expect(screen.getByText('tag2')).toBeInTheDocument();
  expect(screen.queryByText('tag3')).not.toBeInTheDocument();
  expect(screen.getByText('...')).toBeInTheDocument();
});

it('should allow editing tags', async () => {
  const user = userEvent.setup();
  renderTags({ allowUpdate: true });

  const plusButton = screen.getByText('+');
  expect(plusButton).toBeInTheDocument();
  await user.click(plusButton);

  expect(await screen.findByLabelText('search')).toBeInTheDocument();
  await user.click(screen.getByText('tag3'));
  await user.keyboard('{Escape}');

  expect(screen.getByText('tag1')).toBeInTheDocument();
  expect(screen.getByText('tag3')).toBeInTheDocument();

  await user.click(plusButton);
  await user.click(screen.getByRole('checkbox', { name: 'tag1' }));
  await user.keyboard('{Escape}');

  expect(screen.queryByText('tag1')).not.toBeInTheDocument();
  expect(screen.getByText('tag3')).toBeInTheDocument();
});

function renderTags(overrides: Partial<FCProps<typeof Tags>> = {}) {
  renderWithContext(<Wrapper {...overrides} />);
}

function Wrapper(overrides: Partial<FCProps<typeof Tags>> = {}) {
  const [selectedTags, setSelectedTags] = useState<string[]>(overrides.tags ?? ['tag1']);

  const overlay = (
    <MultiSelector
      createElementLabel="create new tag"
      elements={['tag1', 'tag2', 'tag3']}
      headerLabel="edit tags"
      noResultsLabel="no results"
      onSearch={jest.fn().mockResolvedValue(undefined)}
      onSelect={(tag) => {
        setSelectedTags([...selectedTags, tag]);
      }}
      onUnselect={(tag) => {
        const i = selectedTags.indexOf(tag);
        if (i > -1) {
          setSelectedTags([...selectedTags.slice(0, i), ...selectedTags.slice(i + 1)]);
        }
      }}
      searchInputAriaLabel="search"
      selectedElements={selectedTags}
    />
  );

  return (
    <Tags
      ariaTagsListLabel="list"
      emptyText="no tags"
      overlay={overlay}
      tags={selectedTags}
      tooltip={Tooltip}
      {...overrides}
    />
  );
}
