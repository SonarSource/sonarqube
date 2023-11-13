/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import {
  CardWithPrimaryBackground,
  CheckIcon,
  LightLabel,
  SubHeadingHighlight,
} from 'design-system';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import DocumentationLink from '../../../components/common/DocumentationLink';
import { translate } from '../../../helpers/l10n';
import { OPTIMIZED_CAYC_CONDITIONS } from '../utils';

export default function CaycCompliantBanner() {
  return (
    <CardWithPrimaryBackground className="sw-mb-9 sw-p-8">
      <SubHeadingHighlight className="sw-mb-2">
        {translate('quality_gates.cayc.banner.title')}
      </SubHeadingHighlight>

      <div>
        <FormattedMessage
          id="quality_gates.cayc.banner.description1"
          values={{
            cayc_link: (
              <DocumentationLink to="/user-guide/clean-as-you-code/">
                {translate('quality_gates.cayc')}
              </DocumentationLink>
            ),
          }}
        />
      </div>
      <div className="sw-my-2">{translate('quality_gates.cayc.banner.description2')}</div>
      <ul className="sw-body-sm sw-flex sw-flex-col sw-gap-2">
        {Object.values(OPTIMIZED_CAYC_CONDITIONS).map((condition) => (
          <li key={condition.metric}>
            <CheckIcon className="sw-mr-1 sw-pt-1/2" />
            <LightLabel>{translate(`metric.${condition.metric}.description.positive`)}</LightLabel>
          </li>
        ))}
      </ul>
    </CardWithPrimaryBackground>
  );
}
