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
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { FCProps } from '../../../../../types/misc';
import AlmRepoItem from '../AlmRepoItem';

it('render the component correctly when sqProjectKey is not present', () => {
  renderAlmRepoItem();
  expect(screen.getByText('test1')).toBeInTheDocument();
  expect(screen.getByText('url text')).toHaveAttribute('href', '/url');
  expect(
    screen.getByRole('button', { name: 'onboarding.create_project.import' }),
  ).toBeInTheDocument();
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
});

it('render the component correctly when sqProjectKey is present', () => {
  renderAlmRepoItem({ sqProjectKey: 'sqkey' });
  expect(screen.getByText('test1')).toBeInTheDocument();
  expect(screen.getByText('url text')).toHaveAttribute('href', '/url');
  expect(screen.getByText('onboarding.create_project.repository_imported')).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'onboarding.create_project.import' }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
});

it('render the component correctly with checkboxes when sqProjectKey is not present', () => {
  renderAlmRepoItem(undefined, true);
  expect(screen.getByText('test1')).toBeInTheDocument();
  expect(screen.getByText('url text')).toHaveAttribute('href', '/url');
  expect(
    screen.queryByText('onboarding.create_project.repository_imported'),
  ).not.toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'onboarding.create_project.import' }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole('checkbox')).toBeInTheDocument();
  expect(screen.getByRole('checkbox')).not.toBeChecked();
  expect(screen.getByRole('checkbox')).toBeEnabled();
});

it('render the component correctly with checkboxes when sqProjectKey is present', () => {
  renderAlmRepoItem({ sqProjectKey: 'sqkey' }, true);
  expect(screen.getByText('test1')).toBeInTheDocument();
  expect(screen.getByText('url text')).toHaveAttribute('href', '/url');
  expect(screen.getByText('onboarding.create_project.repository_imported')).toBeInTheDocument();
  expect(
    screen.queryByRole('button', { name: 'onboarding.create_project.import' }),
  ).not.toBeInTheDocument();
  expect(screen.getByRole('checkbox')).toBeInTheDocument();
  expect(screen.getByRole('checkbox')).toBeChecked();
  expect(screen.getByRole('checkbox')).toBeDisabled();
});

function renderAlmRepoItem(
  props?: Omit<
    Partial<FCProps<typeof AlmRepoItem>>,
    'multiple' | 'onCheck' | 'onImport' | 'selected'
  >,
  multiple?: boolean,
) {
  const commonProps = {
    primaryTextNode: 'test1',
    almKey: 'key',
    almUrl: 'url',
    almUrlText: 'url text',
    almIconSrc: 'src',
  };
  return renderComponent(
    multiple ? (
      <AlmRepoItem {...commonProps} multiple onCheck={jest.fn()} selected={false} {...props} />
    ) : (
      <AlmRepoItem {...commonProps} onImport={jest.fn()} {...props} />
    ),
  );
}
