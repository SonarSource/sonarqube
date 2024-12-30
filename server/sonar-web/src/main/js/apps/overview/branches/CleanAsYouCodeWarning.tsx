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

import { Link } from '@sonarsource/echoes-react';
import { FormattedMessage } from 'react-intl';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { Component, QualityGate } from '../../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate' | 'organization'>;
  qualityGate?: QualityGate;
}

export default function CleanAsYouCodeWarning({ component }: Props) {
  return (
    <>
      <p className="sw-mb-4 sw-font-bold">
        <FormattedMessage
          id={`overview.quality_gate.conditions.cayc.warning.title.${component.qualifier}`}
        />
      </p>
      {component.qualityGate ? (
        <p>
          <FormattedMessage
            id="overview.quality_gate.conditions.cayc.details_with_link"
            defaultMessage={translate('overview.quality_gate.conditions.cayc.details_with_link')}
            values={{
              link: (
                <Link to={getQualityGateUrl(component.organization, component.qualityGate.name)}>
                  {translate('overview.quality_gate.conditions.non_cayc.warning.link')}
                </Link>
              ),
            }}
          />
        </p>
      ) : (
        <p>
          <FormattedMessage
            id={`overview.quality_gate.conditions.cayc.details.${component.qualifier}`}
          />
        </p>
      )}
    </>
  );
}
