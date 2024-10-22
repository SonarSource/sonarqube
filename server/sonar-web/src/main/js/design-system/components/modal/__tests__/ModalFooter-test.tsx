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

import { renderWithContext } from '../../../helpers/testUtils';
import { FCProps } from '../../../types/misc';
import { ModalFooter } from '../ModalFooter';

it('should render with secondary button', () => {
  const { container } = setupWithProps({
    secondaryButton: (
      <button onClick={() => {}} type="button">
        Close
      </button>
    ),
  });

  expect(container).toMatchSnapshot();
});

it('should render with primary and secondary buttons', () => {
  const { container } = setupWithProps({
    primaryButton: (
      <button onClick={undefined} type="button">
        Primary
      </button>
    ),
    secondaryButton: (
      <button onClick={undefined} type="reset">
        Reset
      </button>
    ),
  });

  expect(container).toMatchSnapshot();
});

function setupWithProps(props: Partial<FCProps<typeof ModalFooter>> = {}) {
  return renderWithContext(<ModalFooter secondaryButton={<div />} {...props} />);
}
