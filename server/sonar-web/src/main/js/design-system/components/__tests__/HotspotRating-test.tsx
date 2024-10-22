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
import { HotspotRatingEnum } from '../../types/measures';
import { HotspotRating } from '../HotspotRating';

it('should render HotspotRating with default LOW rating', () => {
  renderHotspotRating(undefined, 'label');
  expect(screen.getByLabelText('label')).toMatchSnapshot();
});

it.each(Object.values(HotspotRatingEnum))(
  'should render HotspotRating with %s rating',
  (rating) => {
    renderHotspotRating(rating, 'label');
    expect(screen.getByLabelText('label')).toMatchSnapshot();
  },
);

function renderHotspotRating(rating?: HotspotRatingEnum, label?: string) {
  return render(<HotspotRating aria-label={label} rating={rating} />);
}
