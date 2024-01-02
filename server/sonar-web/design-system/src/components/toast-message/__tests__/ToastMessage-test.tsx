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
import { ToastContainer } from 'react-toastify';
import { TOAST_AUTOCLOSE_DELAY } from '../../../helpers/constants';
import { render } from '../../../helpers/testUtils';
import { ToastMessageContainer } from '../ToastMessage';

jest.mock('react-toastify', () => ({
  Slide: 'mock slide',
  ToastContainer: jest.fn(() => null),
  toast: { POSITION: { TOP_RIGHT: 'mock top right' } },
}));

it('should render the ToastMessageContainer', () => {
  setupWithProps();

  expect(ToastContainer).toHaveBeenCalledWith(
    {
      autoClose: TOAST_AUTOCLOSE_DELAY,
      // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
      className: expect.any(String),
      closeOnClick: true,
      draggable: true,
      hideProgressBar: true,
      limit: 0,
      newestOnTop: false,
      pauseOnFocusLoss: true,
      pauseOnHover: true,
      position: 'mock top right',
      rtl: false,
      transition: 'mock slide',
    },
    {},
  );
});

function setupWithProps() {
  return render(<ToastMessageContainer />);
}
