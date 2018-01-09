/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import * as React from 'react';
import { shallow } from 'enzyme';
import TagsList from '../TagsList';

const tags = ['foo', 'bar'];

it('should render with a list of tag', () => {
  const taglist = shallow(<TagsList tags={tags} />);
  expect(taglist.text()).toBe(tags.join(', '));
  expect(taglist.find('i').length).toBe(1);
  expect(taglist.find('span.note').hasClass('text-ellipsis')).toBe(true);
});

it('should correctly handle a lot of tags', () => {
  const lotOfTags = [];
  for (let i = 0; i < 20; i++) {
    lotOfTags.push(String(tags));
  }
  const taglist = shallow(<TagsList tags={lotOfTags} />);
  expect(taglist.text()).toBe(lotOfTags.join(', '));
  expect(taglist.find('span.note').hasClass('text-ellipsis')).toBe(true);
});

it('should render with a caret on the right if update is allowed', () => {
  const taglist = shallow(<TagsList tags={tags} allowUpdate={true} />);
  expect(taglist.find('i').length).toBe(2);
});
