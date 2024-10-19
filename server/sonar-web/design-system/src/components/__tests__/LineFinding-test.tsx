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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { LineFinding } from '../code-line/LineFinding';

it('should render correctly as button', async () => {
  const user = userEvent.setup();
  const { container } = setupWithProps();
  await user.click(screen.getByRole('button'));
  expect(container).toMatchSnapshot();
});

it('should render as non-button', () => {
  setupWithProps({ as: 'div', onIssueSelect: undefined });
  expect(screen.queryByRole('button')).not.toBeInTheDocument();
});

it('should be clickable when onIssueSelect is provided', async () => {
  const mockClick = jest.fn();
  const user = userEvent.setup();

  setupWithProps({ onIssueSelect: mockClick });
  await user.click(screen.getByRole('button'));
  expect(mockClick).toHaveBeenCalled();
});

function setupWithProps(props?: Partial<FCProps<typeof LineFinding>>) {
  return render(
    <LineFinding issueKey="key" message="message" onIssueSelect={jest.fn()} {...props} />,
  );
}
