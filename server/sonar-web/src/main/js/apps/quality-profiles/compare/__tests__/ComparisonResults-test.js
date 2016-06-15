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
import { expect } from 'chai';
import { shallow } from 'enzyme';
import React from 'react';
import ComparisonResults from '../ComparisonResults';
import ComparisonEmpty from '../ComparisonEmpty';
import SeverityIcon from '../../../../components/shared/severity-icon';

describe('Quality Profiles :: ComparisonResults', () => {
  it('should render ComparisonEmpty', () => {
    const output = shallow(
        <ComparisonResults
            left={{ name: 'left' }}
            right={{ name: 'right' }}
            inLeft={[]}
            inRight={[]}
            modified={[]}/>
    );
    expect(output.is(ComparisonEmpty)).to.equal(true);
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
    expect(leftDiffs).to.have.length(1);
    expect(leftDiffs.find('a')).to.have.length(1);
    expect(leftDiffs.find('a').prop('href')).to.include('rule_key=rule1');
    expect(leftDiffs.find('a').text()).to.include('rule1');
    expect(leftDiffs.find(SeverityIcon)).to.have.length(1);
    expect(leftDiffs.find(SeverityIcon).prop('severity')).to.equal('BLOCKER');

    const rightDiffs = output.find('.js-comparison-in-right');
    expect(rightDiffs).to.have.length(2);
    expect(rightDiffs.at(0).find('a')).to.have.length(1);
    expect(rightDiffs.at(0).find('a').prop('href'))
        .to.include('rule_key=rule2');
    expect(rightDiffs.at(0).find('a').text()).to.include('rule2');
    expect(rightDiffs.at(0).find(SeverityIcon)).to.have.length(1);
    expect(rightDiffs.at(0).find(SeverityIcon).prop('severity'))
        .to.equal('CRITICAL');

    const modifiedDiffs = output.find('.js-comparison-modified');
    expect(modifiedDiffs).to.have.length(1);
    expect(modifiedDiffs.find('a').at(0).prop('href')).to.include('rule_key=rule4');
    expect(modifiedDiffs.find('a').at(0).text()).to.include('rule4');
    expect(modifiedDiffs.find(SeverityIcon)).to.have.length(2);
    expect(modifiedDiffs.text()).to.include('bar');
    expect(modifiedDiffs.text()).to.include('qwe');
  });
});
