/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import { renderWithContext } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { SelectionCard } from '../SelectionCard';

it('should render option and be actionnable', async () => {
  const user = userEvent.setup();
  const onClick = jest.fn();
  renderSelectionCard({
    onClick,
    children: <>The Option</>,
  });

  // Click on content
  await user.click(screen.getByText(/The Option/));
  expect(onClick).toHaveBeenCalledTimes(1);

  // Click on radio button
  await user.click(screen.getByRole('radio'));
  expect(onClick).toHaveBeenCalledTimes(2);
});

it('should not be actionnable when disabled', async () => {
  const user = userEvent.setup();
  const onClick = jest.fn();
  renderSelectionCard({
    onClick,
    disabled: true,
    children: <>The Option</>,
  });

  // Clicking on content or radio button should not trigger click handler
  await user.click(screen.getByText(/The Option/));
  expect(onClick).not.toHaveBeenCalled();

  await user.click(screen.getByRole('radio'));
  expect(onClick).not.toHaveBeenCalled();
});

it('should not be actionnable when no click handler', () => {
  renderSelectionCard({
    children: <>The Option</>,
  });

  // Radio button should not be shown
  expect(screen.queryByRole('radio')).not.toBeInTheDocument();
});

function renderSelectionCard(props: Partial<FCProps<typeof SelectionCard>> = {}) {
  return renderWithContext(
    <SelectionCard
      recommended
      recommendedReason="Recommended for you"
      title="Selection Card"
      titleInfo="info"
      {...props}
    />,
  );
}
