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
import { toast } from 'react-toastify';
import { FlagErrorIcon, FlagSuccessIcon } from '../../icons';
import {
  addGlobalErrorMessage,
  addGlobalSuccessMessage,
  dismissAllGlobalMessages,
} from '../toast-utils';

jest.mock('react-toastify', () => ({
  toast: jest.fn(),
}));

it('should call react-toastify with the right args', () => {
  addGlobalErrorMessage(<span>error</span>, { position: 'top-left' });

  expect(toast).toHaveBeenCalledWith(
    <div className="fs-mask sw-body-sm sw-p-3 sw-pb-4" data-test="global-message__ERROR">
      <span>error</span>
    </div>,
    { icon: <FlagErrorIcon />, type: 'error', position: 'top-left' },
  );

  addGlobalSuccessMessage('it worked');

  expect(toast).toHaveBeenCalledWith(
    <div className="fs-mask sw-body-sm sw-p-3 sw-pb-4" data-test="global-message__SUCCESS">
      it worked
    </div>,
    { icon: <FlagSuccessIcon />, type: 'success' },
  );

  toast.dismiss = jest.fn();

  dismissAllGlobalMessages();

  expect(toast.dismiss).toHaveBeenCalled();
});
