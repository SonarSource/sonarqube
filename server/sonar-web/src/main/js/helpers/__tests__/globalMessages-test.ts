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
import { MessageLevel } from '../../types/globalMessages';
import {
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
  registerListener,
} from '../globalMessages';

it('should work as expected', () => {
  const listener1 = jest.fn();
  registerListener(listener1);

  addGlobalErrorMessage('test');

  expect(listener1).toHaveBeenCalledWith(
    expect.objectContaining({ text: 'test', level: MessageLevel.Error })
  );

  listener1.mockClear();
  const listener2 = jest.fn();
  registerListener(listener2);

  addGlobalSuccessMessage('test');

  expect(listener1).toHaveBeenCalledWith(
    expect.objectContaining({ text: 'test', level: MessageLevel.Success })
  );
  expect(listener2).toHaveBeenCalledWith(
    expect.objectContaining({ text: 'test', level: MessageLevel.Success })
  );
});
