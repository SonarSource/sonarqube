/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  searchProjectTags,
  setApplicationTags,
  setProjectTags,
} from '../../../../../../../api/components';
import { mockComponent } from '../../../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../../../types/component';
import MetaTags from '../MetaTags';

jest.mock('../../../../../../../api/components', () => ({
  setApplicationTags: jest.fn().mockResolvedValue(true),
  setProjectTags: jest.fn().mockResolvedValue(true),
  searchProjectTags: jest.fn(),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

it('should render without tags and admin rights', async () => {
  renderMetaTags();

  expect(await screen.findByText('no_tags')).toBeInTheDocument();
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should allow to edit tags for a project', async () => {
  const user = userEvent.setup();
  jest.mocked(searchProjectTags).mockResolvedValue({ tags: ['best', 'useless'] });

  const onComponentChange = jest.fn();
  const component = mockComponent({
    key: 'my-second-project',
    tags: ['foo', 'bar'],
    configuration: {
      showSettings: true,
    },
    name: 'MySecondProject',
  });

  renderMetaTags({ component, onComponentChange });

  expect(await screen.findByText('foo, bar')).toBeInTheDocument();
  expect(screen.getByRole('button')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'tags_list_x.foo, bar' }));

  expect(await screen.findByText('best')).toBeInTheDocument();

  await user.click(screen.getByText('best'));
  expect(onComponentChange).toHaveBeenCalledWith({ tags: ['foo', 'bar', 'best'] });

  onComponentChange.mockClear();

  /*
   * Since we're not actually updating the tags, we're back to having the foo, bar only
   */
  await user.click(screen.getByText('bar'));
  expect(onComponentChange).toHaveBeenCalledWith({ tags: ['foo'] });

  expect(setProjectTags).toHaveBeenCalled();
  expect(setApplicationTags).not.toHaveBeenCalled();
});

it('should set tags for an app', async () => {
  const user = userEvent.setup();

  renderMetaTags({
    component: mockComponent({
      configuration: {
        showSettings: true,
      },
      qualifier: ComponentQualifier.Application,
    }),
  });

  await user.click(screen.getByRole('button', { name: 'tags_list_x.no_tags' }));

  await user.click(screen.getByText('best'));

  expect(setProjectTags).not.toHaveBeenCalled();
  expect(setApplicationTags).toHaveBeenCalled();
});

function renderMetaTags(overrides: Partial<MetaTags['props']> = {}) {
  const component = mockComponent({
    configuration: {
      showSettings: false,
    },
  });

  return renderComponent(
    <MetaTags component={component} onComponentChange={jest.fn()} {...overrides} />
  );
}
