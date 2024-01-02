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
import { DiscreetLink, FlagMessage, Link } from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { useDocUrl } from '../../../helpers/docs';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { Component } from '../../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
}

export default function CleanAsYouCodeWarning({ component }: Props) {
  const caycUrl = useDocUrl('/user-guide/clean-as-you-code/');

  return (
    <>
      <FlagMessage variant="warning">
        {translate('overview.quality_gate.conditions.cayc.warning')}
      </FlagMessage>
      {component.qualityGate ? (
        <p className="sw-my-4">
          <FormattedMessage
            id="overview.quality_gate.conditions.cayc.details_with_link"
            defaultMessage={translate('overview.quality_gate.conditions.cayc.details_with_link')}
            values={{
              link: (
                <DiscreetLink to={getQualityGateUrl(component.qualityGate.name)}>
                  {translate('overview.quality_gate.conditions.non_cayc.warning.link')}
                </DiscreetLink>
              ),
            }}
          />
        </p>
      ) : (
        <p className="sw-my-4">{translate('overview.quality_gate.conditions.cayc.details')}</p>
      )}

      <Link to={caycUrl}>{translate('overview.quality_gate.conditions.cayc.link')}</Link>
    </>
  );
}
