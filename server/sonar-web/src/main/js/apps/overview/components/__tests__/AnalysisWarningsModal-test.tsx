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
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockTaskWarning } from '../../../../helpers/mocks/tasks';
import { mockCurrentUser } from '../../../../helpers/testMocks';
import { renderComponent } from '../../../../helpers/testReactTestingUtils';
import { ComponentPropsType } from '../../../../helpers/testUtils';
import { AnalysisWarningsModal } from '../AnalysisWarningsModal';

jest.mock('../../../../api/ce', () => ({
  dismissAnalysisWarning: jest.fn().mockResolvedValue(null),
  getTask: jest.fn().mockResolvedValue({
    warnings: ['message foo', 'message-bar', 'multiline message\nsecondline\n  third line'],
  }),
}));

beforeEach(jest.clearAllMocks);

describe('should render correctly', () => {
  it('should not show dismiss buttons for non-dismissable warnings', () => {
    renderAnalysisWarningsModal();

    expect(screen.getByText('warning 1')).toBeInTheDocument();
    expect(screen.getByText('warning 2')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'dismiss_permanently' })).not.toBeInTheDocument();
  });

  it('should show a dismiss button for dismissable warnings', () => {
    renderAnalysisWarningsModal({ warnings: [mockTaskWarning({ dismissable: true })] });

    expect(screen.getByRole('button', { name: 'dismiss_permanently' })).toBeInTheDocument();
  });

  it('should not show dismiss buttons if not logged in', () => {
    renderAnalysisWarningsModal({
      currentUser: mockCurrentUser({ isLoggedIn: false }),
      warnings: [mockTaskWarning({ dismissable: true })],
    });

    expect(screen.queryByRole('button', { name: 'dismiss_permanently' })).not.toBeInTheDocument();
  });
});

function renderAnalysisWarningsModal(
  props: Partial<ComponentPropsType<typeof AnalysisWarningsModal>> = {},
) {
  return renderComponent(
    <AnalysisWarningsModal
      component={mockComponent()}
      currentUser={mockCurrentUser({ isLoggedIn: true })}
      onClose={jest.fn()}
      warnings={[
        mockTaskWarning({ message: 'warning 1' }),
        mockTaskWarning({ message: 'warning 2' }),
      ]}
      {...props}
    />,
  );
}
