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
import { shallow } from 'enzyme';
import * as React from 'react';
import { EditionKey } from '../../../../types/editions';
import { RiskConsent } from '../../../../types/plugins';
import PluginRiskConsentBox, { PluginRiskConsentBoxProps } from '../PluginRiskConsentBox';

it.each([[undefined], [RiskConsent.Accepted], [RiskConsent.NotAccepted], [RiskConsent.Required]])(
  'should render correctly for risk consent %s',
  (riskConsent?: RiskConsent) => {
    expect(shallowRender({ riskConsent })).toMatchSnapshot();
  }
);

it('should render correctly for community edition', () => {
  expect(shallowRender({ currentEdition: EditionKey.community })).toMatchSnapshot();
});

function shallowRender(props: Partial<PluginRiskConsentBoxProps> = {}) {
  return shallow(<PluginRiskConsentBox acknowledgeRisk={jest.fn()} {...props} />);
}
