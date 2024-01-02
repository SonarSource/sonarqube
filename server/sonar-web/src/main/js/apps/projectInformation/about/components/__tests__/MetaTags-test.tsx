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
import { act, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { setApplicationTags, setProjectTags } from '../../../../../api/components';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import MetaTags from '../MetaTags';

jest.mock('../../../../../api/components', () => ({
  setApplicationTags: jest.fn().mockResolvedValue(true),
  setProjectTags: jest.fn().mockResolvedValue(true),
  searchProjectTags: jest.fn().mockResolvedValue({ tags: ['best', 'useless'] }),
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

  await act(() => user.click(screen.getByRole('button', { name: 'foo bar +' })));

  expect(await screen.findByRole('checkbox', { name: 'best' })).toBeInTheDocument();

  await user.click(screen.getByRole('checkbox', { name: 'best' }));
  expect(onComponentChange).toHaveBeenCalledWith({ tags: ['foo', 'bar', 'best'] });

  onComponentChange.mockClear();

  /*
   * Since we're not actually updating the tags, we're back to having the foo, bar only
   */
  await user.click(screen.getByRole('checkbox', { name: 'bar' }));
  expect(onComponentChange).toHaveBeenCalledWith({ tags: ['foo'] });

  expect(setProjectTags).toHaveBeenCalled();
  expect(setApplicationTags).not.toHaveBeenCalled();
  await user.click(document.body);
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
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

  await act(() => user.click(screen.getByRole('button', { name: 'no_tags +' })));

  await user.click(await screen.findByRole('checkbox', { name: 'best' }));

  expect(setProjectTags).not.toHaveBeenCalled();
  expect(setApplicationTags).toHaveBeenCalled();
});

function renderMetaTags(overrides: Partial<Parameters<typeof MetaTags>[0]> = {}) {
  const component = mockComponent({
    configuration: {
      showSettings: false,
    },
  });

  return renderComponent(
    <MetaTags component={component} onComponentChange={jest.fn()} {...overrides} />,
  );
}
