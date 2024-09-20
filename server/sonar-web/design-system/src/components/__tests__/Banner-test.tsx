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
import { renderWithContext } from '../../helpers/testUtils';
import { Note } from '../../sonar-aligned';
import { FCProps } from '../../types/misc';
import { Banner } from '../Banner';

it('should render with close button', async () => {
  const onDismiss = jest.fn();
  const { user } = setupWithProps({ onDismiss });
  expect(
    screen.getByRole('button', {
      name: 'dismiss',
    }),
  ).toBeVisible();

  await user.click(
    screen.getByRole('button', {
      name: 'dismiss',
    }),
  );

  expect(onDismiss).toHaveBeenCalledTimes(1);
});

function setupWithProps(props: Partial<FCProps<typeof Banner>> = {}) {
  return renderWithContext(
    <Banner {...props} variant="warning">
      <Note className="sw-typo-default">{props.children ?? 'Test Message'}</Note>
    </Banner>,
  );
}
