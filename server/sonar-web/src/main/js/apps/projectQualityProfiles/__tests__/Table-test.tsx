/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { shallow } from 'enzyme';
import * as React from 'react';
import Table from '../Table';

it('renders', () => {
  const fooJava = randomProfile('foo-java', 'java');
  const fooJs = randomProfile('foo-js', 'js');
  const allProfiles = [
    fooJava,
    randomProfile('bar-java', 'java'),
    randomProfile('baz-java', 'java'),
    fooJs
  ];
  const profiles = [fooJava, fooJs];
  expect(
    shallow(<Table allProfiles={allProfiles} onChangeProfile={jest.fn()} profiles={profiles} />)
  ).toMatchSnapshot();
});

function randomProfile(key: string, language: string) {
  return {
    activeRuleCount: 17,
    activeDeprecatedRuleCount: 0,
    key,
    name: key,
    language,
    languageName: language,
    organization: 'org'
  };
}
