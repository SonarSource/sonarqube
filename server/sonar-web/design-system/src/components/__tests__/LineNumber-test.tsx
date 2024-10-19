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
import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';
import { LineNumber } from '../code-line/LineNumber';

it('should a popup when clicked', async () => {
  const { user } = setupWithProps();

  expect(screen.getByRole('button', { name: 'aria-label' })).toBeVisible();

  await user.click(screen.getByRole('button', { name: 'aria-label' }));
  expect(screen.getByText('Popup')).toBeVisible();
});

function setupWithProps(props: Partial<FCProps<typeof LineNumber>> = {}) {
  return render(
    <table>
      <tbody>
        <tr>
          <LineNumber
            ariaLabel="aria-label"
            displayOptions
            firstLineNumber={1}
            lineNumber={16}
            popup={<div>Popup</div>}
            {...props}
          />
        </tr>
      </tbody>
    </table>,
  );
}
