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
import * as React from 'react';
import { mockBranch } from '../../../helpers/mocks/branch-like';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { GraphType } from '../../../types/project-activity';
import ActivityLink, { ActivityLinkProps } from '../ActivityLink';

it('renders correctly without props', () => {
  renderActivityLink();
  expect(screen.getByText('portfolio.activity_link')).toBeInTheDocument();
});

it('renders correctly with props label and branch', () => {
  renderActivityLink({ label: 'Foo', branchLike: mockBranch() });
  expect(screen.getByText('Foo')).toBeInTheDocument();
});

it('renders correctly with graph', () => {
  renderActivityLink({ graph: GraphType.coverage });
  const anchorElement = screen.getByRole('link');

  expect(anchorElement).toBeInTheDocument();
  expect(anchorElement).toHaveAttribute('href', '/project/activity?id=foo&graph=coverage');
});

it('renders correctly with graph and metric', () => {
  renderActivityLink({ graph: GraphType.custom, metric: 'new_bugs,bugs' });
  const anchorElement = screen.getByRole('link');

  expect(anchorElement).toBeInTheDocument();
  expect(anchorElement).toHaveAttribute(
    'href',
    '/project/activity?id=foo&graph=custom&custom_metrics=new_bugs%2Cbugs',
  );
});

function renderActivityLink(props: Partial<ActivityLinkProps> = {}) {
  return renderComponent(<ActivityLink component="foo" {...props} />);
}
