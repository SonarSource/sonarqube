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
import DocMarkdownBlock from '../DocMarkdownBlock';
import { isSonarCloud } from '../../../helpers/system';

// mock `remark` and `remark-react` to work around the issue with cjs imports
jest.mock('remark', () => {
  const remark = require.requireActual('remark');
  return { default: remark };
});

jest.mock('remark-react', () => {
  const remarkReact = require.requireActual('remark-react');
  return { default: remarkReact };
});

jest.mock('../../../helpers/system', () => ({
  getInstance: jest.fn(),
  isSonarCloud: jest.fn()
}));

it('should render simple markdown', () => {
  expect(shallow(<DocMarkdownBlock content="this is *bold* text" />)).toMatchSnapshot();
});

it('should use custom component for links', () => {
  expect(
    shallow(<DocMarkdownBlock content="some [link](/quality-profiles)" />).find('withChildProps')
  ).toMatchSnapshot();
});

it('should cut sonarqube/sonarcloud/static content', () => {
  const content = `
some

<!-- sonarqube -->
sonarqube
<!-- /sonarqube -->

<!-- sonarcloud -->
sonarcloud
<!-- /sonarcloud -->

<!-- static -->
static
<!-- /static -->

<!-- sonarqube -->
  long

  multiline
<!-- /sonarqube -->

text`;

  (isSonarCloud as jest.Mock).mockImplementation(() => false);
  expect(shallow(<DocMarkdownBlock content={content} />)).toMatchSnapshot();

  (isSonarCloud as jest.Mock).mockImplementation(() => true);
  expect(shallow(<DocMarkdownBlock content={content} />)).toMatchSnapshot();
});

it('should render with custom props for links', () => {
  expect(
    shallow(
      <DocMarkdownBlock
        childProps={{ foo: 'bar' }}
        content="some [link](#quality-profiles)"
        isTooltip={true}
      />
    ).find('withChildProps')
  ).toMatchSnapshot();
});
