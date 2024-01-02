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
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import ListFooter, { ListFooterProps } from '../ListFooter';

describe('ListFooter', () => {
  describe('rendering', () => {
    it('should render correctly when loading', async () => {
      renderListFooter({ loading: true });
      expect(await screen.findByText('loading')).toBeInTheDocument();
    });

    it('should not render if there are neither loadmore nor reload props', () => {
      renderListFooter({ loadMore: undefined, reload: undefined });
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it.each([
      [undefined, 60, 30, true],
      [undefined, 45, 30, false],
      [undefined, 60, undefined, false],
      [60, 60, 30, false],
    ])(
      'should handle showing load more button based on total, count and pageSize',
      (total, count, pageSize, expected) => {
        renderListFooter({ total, count, pageSize });

        /* eslint-disable jest/no-conditional-in-test */
        /* eslint-disable jest/no-conditional-expect */
        if (expected) {
          expect(screen.getByRole('button')).toBeInTheDocument();
        } else {
          expect(screen.queryByRole('button')).not.toBeInTheDocument();
        }
        /* eslint-enable jest/no-conditional-in-test */
        /* eslint-enable jest/no-conditional-expect */
      },
    );
  });

  it('should properly call load more callback', async () => {
    const user = userEvent.setup();
    const loadMore = jest.fn();
    renderListFooter({ loadMore });

    await user.click(screen.getByRole('button'));
    expect(loadMore).toHaveBeenCalled();
  });

  it('should properly call reload callback', async () => {
    const user = userEvent.setup();
    const reload = jest.fn();
    renderListFooter({ needReload: true, reload });

    await user.click(screen.getByRole('button'));
    expect(reload).toHaveBeenCalled();
  });

  function renderListFooter(props: Partial<ListFooterProps> = {}) {
    return renderComponent(<ListFooter count={3} loadMore={jest.fn()} total={5} {...props} />);
  }
});

// Once the MIUI buttons become the norm, we can use only the above test "suite" and drop this one.
describe('ListFooter using MIUI buttons', () => {
  describe('rendering', () => {
    it('should render correctly when loading', async () => {
      renderListFooter({ loading: true });
      expect(await screen.findByText('loading')).toBeInTheDocument();
    });

    it('should not render if there are neither loadmore nor reload props', () => {
      renderListFooter({ loadMore: undefined, reload: undefined });
      expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it.each([
      [undefined, 60, 30, true],
      [undefined, 45, 30, false],
      [undefined, 60, undefined, false],
      [60, 60, 30, false],
    ])(
      'should handle showing load more button based on total, count and pageSize',
      (total, count, pageSize, expected) => {
        renderListFooter({ total, count, pageSize });

        /* eslint-disable jest/no-conditional-in-test */
        /* eslint-disable jest/no-conditional-expect */
        if (expected) {
          expect(screen.getByRole('button')).toBeInTheDocument();
        } else {
          expect(screen.queryByRole('button')).not.toBeInTheDocument();
        }
        /* eslint-enable jest/no-conditional-in-test */
        /* eslint-enable jest/no-conditional-expect */
      },
    );
  });

  it('should properly call load more callback', async () => {
    const user = userEvent.setup();
    const loadMore = jest.fn();
    renderListFooter({ loadMore });

    await user.click(screen.getByRole('button'));
    expect(loadMore).toHaveBeenCalled();
  });

  it('should properly call reload callback', async () => {
    const user = userEvent.setup();
    const reload = jest.fn();
    renderListFooter({ needReload: true, reload });

    await user.click(screen.getByRole('button'));
    expect(reload).toHaveBeenCalled();
  });

  function renderListFooter(props: Partial<ListFooterProps> = {}) {
    return renderComponent(
      <ListFooter count={3} loadMore={jest.fn()} total={5} useMIUIButtons {...props} />,
    );
  }
});
