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

import { fireEvent, screen } from '@testing-library/react';
import { render } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { FileInput } from '../FileInput';

it('should correclty choose a file and reset it', async () => {
  const file = new File([''], 'file.txt', { type: 'text/plain' });
  const onFileSelected = jest.fn();
  const { user } = setupWithProps({ onFileSelected });

  expect(screen.getByRole('button')).toHaveTextContent('Choose');
  expect(screen.getByText('No file selected')).toBeVisible();

  await user.click(screen.getByRole('button'));
  fireEvent.change(screen.getByTestId('file-input'), {
    target: { files: [file] },
  });
  expect(onFileSelected).toHaveBeenCalledWith(file);
  expect(screen.getByText('file.txt')).toBeVisible();
  expect(screen.getByRole('button')).toHaveTextContent('Clear');

  await user.click(screen.getByRole('button'));
  expect(screen.getByText('No file selected')).toBeVisible();
  expect(onFileSelected).toHaveBeenCalledWith(undefined);
});

function setupWithProps(props: Partial<FCProps<typeof FileInput>> = {}) {
  return render(
    <FileInput chooseLabel="Choose" clearLabel="Clear" noFileLabel="No file selected" {...props} />,
  );
}
