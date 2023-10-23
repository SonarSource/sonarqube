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
import userEvent from '@testing-library/user-event';
import * as React from 'react';
import { renderComponent } from '../../../helpers/testReactTestingUtils';
import { byRole } from '../../../helpers/testSelector';
import { FCProps } from '../../../types/misc';
import RadioCard from '../RadioCard';

describe('RadioCard component', () => {
  it('renders & handles selection', async () => {
    const user = userEvent.setup();
    const onClick = jest.fn();
    renderRadioCard({ onClick });

    const card = byRole('radio').get();

    expect(card).toBeInTheDocument();
    expect(byRole('heading', { name: 'body' }).get()).toBeInTheDocument();

    // Keyboard selection
    await user.keyboard('{Tab}');
    await user.keyboard('{Enter}');
    expect(onClick).toHaveBeenCalledTimes(1);

    // Mouse selection
    await user.click(card);
    expect(onClick).toHaveBeenCalledTimes(2);
  });
});

function renderRadioCard(overrides: Partial<FCProps<typeof RadioCard>>) {
  return renderComponent(
    <RadioCard title="Radio Card" {...overrides}>
      <h3>body</h3>
    </RadioCard>,
  );
}
