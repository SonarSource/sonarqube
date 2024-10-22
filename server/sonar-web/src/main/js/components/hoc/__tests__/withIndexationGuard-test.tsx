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

import { render, screen } from '@testing-library/react';
import { IndexationContext } from '../../../app/components/indexation/IndexationContext';
import withIndexationGuard from '../withIndexationGuard';

describe('withIndexationGuard', () => {
  it('should render indexation message when showIndexationMessage returns true', () => {
    renderComponentWithIndexationGuard(() => true);
    expect(
      screen.getByText(/indexation\.page_unavailable\.description\.additional_information/),
    ).toBeInTheDocument();
  });

  it('should render children when showIndexationMessage returns false', () => {
    renderComponentWithIndexationGuard(() => false);
    expect(screen.getByText('TestComponent')).toBeInTheDocument();
  });
});

function renderComponentWithIndexationGuard(showIndexationMessage: () => boolean) {
  const TestComponentWithGuard = withIndexationGuard({
    Component: TestComponent,
    showIndexationMessage,
  });

  return render(
    <IndexationContext.Provider
      value={{
        status: { completedCount: 23, isCompleted: false, hasFailures: false, total: 42 },
      }}
    >
      <TestComponentWithGuard />
    </IndexationContext.Provider>,
  );
}

function TestComponent() {
  return <h1>TestComponent</h1>;
}
