/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
import { mockMainBranch } from '../../../../helpers/mocks/branch-like';
import { mockComponent } from '../../../../helpers/mocks/component';
import { mockPeriod } from '../../../../helpers/testMocks';
import { ComponentQualifier } from '../../../../types/component';
import MeasuresPanelNoNewCode, { MeasuresPanelNoNewCodeProps } from '../MeasuresPanelNoNewCode';

it('should render the default message', () => {
  const defaultMessage = `
  <div
    className="display-flex-center display-flex-justify-center"
    style={
      Object {
        "height": 500,
      }
    }
  >
    <img
      alt=""
      className="spacer-right"
      height={52}
      src="/images/source-code.svg"
    />
    <div
      className="big-spacer-left text-muted"
      style={
        Object {
          "maxWidth": 500,
        }
      }
    >
      <p
        className="spacer-bottom big-spacer-top big"
      >
        overview.measures.empty_explanation
      </p>
      <p>
        <FormattedMessage
          defaultMessage="overview.measures.empty_link"
          id="overview.measures.empty_link"
          values={
            Object {
              "learn_more_link": <Link
                onlyActiveOnIndex={false}
                style={Object {}}
                to="/documentation/user-guide/clean-as-you-code/"
              >
                learn_more
              </Link>,
            }
          }
        />
      </p>
    </div>
  </div>
`;

  expect(shallowRender()).toMatchInlineSnapshot(defaultMessage);
  expect(
    shallowRender({ component: mockComponent({ qualifier: ComponentQualifier.Application }) })
  ).toMatchInlineSnapshot(defaultMessage);
  expect(
    shallowRender({ period: mockPeriod({ date: '2018-05-23', mode: 'REFERENCE_BRANCH' }) })
  ).toMatchInlineSnapshot(defaultMessage);
  expect(
    shallowRender({ period: mockPeriod({ date: '2018-05-23', mode: 'PREVIOUS_VERSION' }) })
  ).toMatchInlineSnapshot(defaultMessage);
  expect(
    shallowRender({
      period: mockPeriod({ date: undefined, mode: 'REFERENCE_BRANCH', parameter: 'master' })
    })
  ).toMatchSnapshot();
  expect(
    shallowRender({
      period: mockPeriod({ date: undefined, mode: 'REFERENCE_BRANCH', parameter: 'notsame' })
    })
  ).toMatchSnapshot();
});

it('should render "bad code setting" explanation', () => {
  const period = mockPeriod({ date: undefined, mode: 'REFERENCE_BRANCH' });
  expect(shallowRender({ period })).toMatchSnapshot('no link');
  expect(
    shallowRender({ component: mockComponent({ configuration: { showSettings: true } }), period })
  ).toMatchSnapshot('with link');
});

function shallowRender(props: Partial<MeasuresPanelNoNewCodeProps> = {}) {
  return shallow<MeasuresPanelNoNewCodeProps>(
    <MeasuresPanelNoNewCode branch={mockMainBranch()} component={mockComponent()} {...props} />
  );
}
