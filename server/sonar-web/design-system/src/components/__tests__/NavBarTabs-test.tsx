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
import { renderWithRouter } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { DisabledTabLink, NavBarTabLink, NavBarTabs } from '../NavBarTabs';

describe('NewNavBarTabs', () => {
  it('should render correctly', () => {
    setup();

    expect(screen.getByRole('list')).toBeInTheDocument();
    expect(screen.getByRole('listitem')).toBeInTheDocument();
    expect(screen.getByRole('link')).toBeInTheDocument();
    expect(screen.getByRole('link')).toHaveTextContent('test');
  });

  function setup() {
    return renderWithRouter(
      <NavBarTabs>
        <NavBarTabLink text="test" to="/summary/new_code" />
      </NavBarTabs>,
    );
  }
});

describe('NewNavBarTabLink', () => {
  it('should not be active when on different url', () => {
    setupWithProps();

    expect(screen.getByRole('link')).not.toHaveClass('active');
  });

  it('should be active when on same url', () => {
    setupWithProps({ to: '/' });

    expect(screen.getByRole('link')).toHaveClass('active');
  });

  it('should be active when active prop is set regardless of the url', () => {
    setupWithProps({ active: true, withChevron: true });

    expect(screen.getByRole('link')).toHaveClass('active');
  });

  it('should not be active when active prop is false regardless of the url', () => {
    setupWithProps({ active: false, to: '/' });

    expect(screen.getByRole('link')).not.toHaveClass('active');
  });

  function setupWithProps(props: Partial<FCProps<typeof NavBarTabLink>> = {}) {
    return renderWithRouter(<NavBarTabLink text="test" to="/summary/new_code" {...props} />);
  }
});

describe('DisabledTabLink', () => {
  it('should render correctly', () => {
    renderWithRouter(<DisabledTabLink label="label" overlay={<span>Overlay</span>} />);
    expect(screen.getByRole('link')).toHaveClass('disabled-link');
  });
});
