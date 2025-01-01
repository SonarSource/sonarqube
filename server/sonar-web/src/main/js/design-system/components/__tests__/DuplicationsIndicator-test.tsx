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

import { render } from '../../helpers/testUtils';
import { FCProps } from '../../types/misc';

import { DuplicationLabel } from '../../types/measures';
import { DuplicationsIndicator } from '../DuplicationsIndicator';

it('should display DuplicationsIndicator without rating', () => {
  const wrapper = setupWithProps();
  expect(wrapper.baseElement).toMatchSnapshot();
});

it.each(['A', 'B', 'C', 'D', 'E', 'F'])(
  'should display DuplicationsIndicator with rating',
  (variant: DuplicationLabel) => {
    const wrapper = setupWithProps({ rating: variant });
    expect(wrapper.baseElement).toMatchSnapshot();
  },
);

function setupWithProps(props: Partial<FCProps<typeof DuplicationsIndicator>> = {}) {
  return render(<DuplicationsIndicator {...props} />);
}
