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
import BranchesServiceMock from '../../../../../api/mocks/BranchesServiceMock';
import { mockComponent } from '../../../../../helpers/mocks/component';
import { renderComponent } from '../../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../../helpers/testUtils';
import { ComponentQualifier } from '../../../../../types/component';
import { Feature } from '../../../../../types/features';
import { Menu } from '../Menu';

const handler = new BranchesServiceMock();

const BASE_COMPONENT = mockComponent({
  analysisDate: '2019-12-01',
  key: 'foo',
  name: 'foo',
});

beforeEach(() => handler.reset());

it('should render correctly', async () => {
  const user = userEvent.setup();
  const component = {
    ...BASE_COMPONENT,
    configuration: {
      showSettings: true,
      extensions: [
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' },
        { key: 'securityreport/foo', name: 'Foo' },
      ],
    },
    extensions: [
      { key: 'component-foo', name: 'ComponentFoo' },
      { key: 'component-bar', name: 'ComponentBar' },
      { key: 'securityreport/foo', name: 'Security Report' },
    ],
  };
  renderMenu({ component });

  // Security Report is rendered on its own, as is not part of the dropdown menu.
  expect(screen.getByRole('link', { name: 'layout.security_reports' })).toBeInTheDocument();

  // Check the dropdown.
  const button = screen.getByRole('button', { name: 'more' });
  expect(button).toBeInTheDocument();
  await user.click(button);
  expect(screen.getByRole('menuitem', { name: 'ComponentFoo' })).toBeInTheDocument();
  expect(screen.getByRole('menuitem', { name: 'ComponentBar' })).toBeInTheDocument();
});

it('should render correctly when on a Portofolio', () => {
  const component = {
    ...BASE_COMPONENT,
    configuration: {
      showSettings: true,
      extensions: [
        { key: 'foo', name: 'Foo' },
        { key: 'bar', name: 'Bar' },
      ],
    },
    qualifier: ComponentQualifier.Portfolio,
    extensions: [
      { key: 'governance/foo', name: 'governance foo' },
      { key: 'governance/bar', name: 'governance bar' },
    ],
  };
  renderMenu({ component });
  expect(screen.getByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'issues.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'layout.measures' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'portfolio_breakdown.page' })).toBeInTheDocument();
});

it('should render correctly when on a branch', async () => {
  renderMenu(
    {
      component: {
        ...BASE_COMPONENT,
        configuration: { showSettings: true },
        extensions: [{ key: 'component-foo', name: 'ComponentFoo' }],
      },
    },
    'branch=normal-branch',
  );

  expect(await screen.findByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'issues.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'layout.measures' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'project.info.title' })).toBeInTheDocument();
});

it('should render correctly when on a pull request', async () => {
  renderMenu(
    {
      component: {
        ...BASE_COMPONENT,
        configuration: { showSettings: true },
        extensions: [{ key: 'component-foo', name: 'ComponentFoo' }],
      },
    },
    'pullRequest=01',
  );

  expect(await screen.findByRole('link', { name: 'overview.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'issues.page' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'layout.measures' })).toBeInTheDocument();

  expect(
    screen.queryByRole('link', { name: `layout.settings.${ComponentQualifier.Project}` }),
  ).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'project.info.title' })).not.toBeInTheDocument();
});

it('should disable links if no analysis has been done', () => {
  renderMenu({
    component: {
      ...BASE_COMPONENT,
      analysisDate: undefined,
    },
  });

  expect(screen.queryByRole('link', { name: 'issues.page' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
  expect(screen.queryByRole('link', { name: 'layout.measures' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
  expect(screen.getByRole('link', { name: 'project.info.title' })).toBeInTheDocument();
});

it('should disable links if application has inaccessible projects', () => {
  renderMenu({
    component: {
      ...BASE_COMPONENT,
      qualifier: ComponentQualifier.Application,
      canBrowseAllChildProjects: false,
    },
  });
  expect(screen.queryByRole('link', { name: 'overview.page' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
  expect(screen.queryByRole('link', { name: 'issues.page' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
  expect(screen.queryByRole('link', { name: 'layout.measures' })).toHaveAttribute(
    'aria-disabled',
    'true',
  );
  expect(screen.queryByRole('button', { name: 'application.info.title' })).not.toBeInTheDocument();
});

function renderMenu(props: Partial<ComponentPropsType<typeof Menu>> = {}, params?: string) {
  return renderComponent(
    <Menu
      hasFeature={jest.fn().mockReturnValue(false)}
      component={BASE_COMPONENT}
      isInProgress={false}
      isPending={false}
      {...props}
    />,
    params ? `/?${params}` : '/',
    { featureList: [Feature.BranchSupport] },
  );
}
