/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import ComparisonResults from '../ComparisonResults';
import ComparisonEmpty from '../ComparisonEmpty';
import SeverityIcon from '../../../../components/shared/severity-icon';

it('should render ComparisonEmpty', () => {
  const output = shallow(
      <ComparisonResults
          left={{ name: 'left' }}
          right={{ name: 'right' }}
          inLeft={[]}
          inRight={[]}
          modified={[]}/>
  );
  expect(output.is(ComparisonEmpty)).toBe(true);
});

it('should compare', () => {
  const inLeft = [
    { key: 'rule1', name: 'rule1', severity: 'BLOCKER' }
  ];
  const inRight = [
    { key: 'rule2', name: 'rule2', severity: 'CRITICAL' },
    { key: 'rule3', name: 'rule3', severity: 'MAJOR' }
  ];
  const modified = [
    {
      key: 'rule4',
      name: 'rule4',
      left: {
        severity: 'BLOCKER',
        params: { foo: 'bar' }
      },
      right: {
        severity: 'INFO',
        params: { foo: 'qwe' }
      }
    }
  ];

  const output = shallow(
      <ComparisonResults
          left={{ name: 'left' }}
          right={{ name: 'right' }}
          inLeft={inLeft}
          inRight={inRight}
          modified={modified}/>
  );

  const leftDiffs = output.find('.js-comparison-in-left');
  expect(leftDiffs.length).toBe(1);
  expect(leftDiffs.find('a').length).toBe(1);
  expect(leftDiffs.find('a').prop('href')).toContain('rule_key=rule1');
  expect(leftDiffs.find('a').text()).toContain('rule1');
  expect(leftDiffs.find(SeverityIcon).length).toBe(1);
  expect(leftDiffs.find(SeverityIcon).prop('severity')).toBe('BLOCKER');

  const rightDiffs = output.find('.js-comparison-in-right');
  expect(rightDiffs.length).toBe(2);
  expect(rightDiffs.at(0).find('a').length).toBe(1);
  expect(rightDiffs.at(0).find('a').prop('href'))
      .toContain('rule_key=rule2');
  expect(rightDiffs.at(0).find('a').text()).toContain('rule2');
  expect(rightDiffs.at(0).find(SeverityIcon).length).toBe(1);
  expect(rightDiffs.at(0).find(SeverityIcon).prop('severity'))
      .toBe('CRITICAL');

  const modifiedDiffs = output.find('.js-comparison-modified');
  expect(modifiedDiffs.length).toBe(1);
  expect(modifiedDiffs.find('a').at(0).prop('href')).toContain('rule_key=rule4');
  expect(modifiedDiffs.find('a').at(0).text()).toContain('rule4');
  expect(modifiedDiffs.find(SeverityIcon).length).toBe(2);
  expect(modifiedDiffs.text()).toContain('bar');
  expect(modifiedDiffs.text()).toContain('qwe');
});
