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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import DocLink from '../../../components/common/DocLink';
import Link from '../../../components/common/Link';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { Component } from '../../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
}

export default function CleanAsYouCodeWarningOverCompliant({ component }: Props) {
  return (
    <>
      {component.qualityGate ? (
        <p className="big-spacer-bottom">
          <FormattedMessage
            id="overview.quality_gate.conditions.cayc_over_compliant.details_with_link"
            defaultMessage={translate(
              'overview.quality_gate.conditions.cayc_over_compliant.details_with_link'
            )}
            values={{
              link: (
                <Link to={getQualityGateUrl(component.qualityGate.key)}>
                  {translate('overview.quality_gate.conditions.cayc_over_compliant.warning.link')}
                </Link>
              ),
            }}
          />
        </p>
      ) : (
        <p className="big-spacer-bottom">
          {translate('overview.quality_gate.conditions.cayc_over_compliant.details')}
        </p>
      )}

      <DocLink to="/user-guide/clean-as-you-code/#potential-drawbacks">
        {translate('overview.quality_gate.conditions.cayc_over_compliant.link')}
      </DocLink>
    </>
  );
}
