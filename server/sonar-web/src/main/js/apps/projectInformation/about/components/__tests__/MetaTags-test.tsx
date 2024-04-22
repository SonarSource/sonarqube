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
import { setApplicationTags, setProjectTags } from '../../../../../api/components';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentQualifier } from '../../../../../types/component';
import { Component } from '../../../../../types/types';
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
  const component = mockComponent({
    key: 'my-second-project',
    tags: ['foo', 'bar'],
    configuration: {
      showSettings: true,
    },
    name: 'MySecondProject',
  });

  renderMetaTags(component);

  expect(await screen.findByText('foo, bar')).toBeInTheDocument();
  expect(screen.getByRole('button')).toBeInTheDocument();

  await user.click(screen.getByRole('button', { name: 'foo bar +' }));

  expect(await screen.findByRole('checkbox', { name: 'best' })).toBeInTheDocument();

  await user.click(screen.getByRole('checkbox', { name: 'best' }));
  expect(await screen.findByRole('button', { name: 'foo bar ... +' })).toBeInTheDocument();

  await user.click(screen.getByRole('checkbox', { name: 'bar' }));

  expect(await screen.findByRole('button', { name: 'foo best +' })).toBeInTheDocument();

  expect(setProjectTags).toHaveBeenCalled();
  expect(setApplicationTags).not.toHaveBeenCalled();
  await user.click(document.body);
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
});

it('should set tags for an app', async () => {
  const user = userEvent.setup();

  renderMetaTags(
    mockComponent({
      configuration: {
        showSettings: true,
      },
      qualifier: ComponentQualifier.Application,
    }),
  );

  await user.click(screen.getByRole('button', { name: 'no_tags +' }));

  await user.click(await screen.findByRole('checkbox', { name: 'best' }));

  expect(setProjectTags).not.toHaveBeenCalled();
  expect(setApplicationTags).toHaveBeenCalled();
});

function renderMetaTags(componentOverride: Partial<Component> = {}) {
  function Component(componentOverride: Partial<Parameters<typeof MetaTags>[0]>) {
    const [component, setComponent] = React.useState(
      mockComponent({
        configuration: {
          showSettings: false,
        },
        ...componentOverride,
      }),
    );

    const handleComponentChange = ({ tags }: { tags: string[] }) => {
      setComponent((c) => {
        return { ...c, tags };
      });
    };

    return <MetaTags component={component} onComponentChange={handleComponentChange} />;
  }

  return renderComponent(<Component {...componentOverride} />);
}
