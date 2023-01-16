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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import Link from '../../../components/common/Link';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { getQualityGateUrl } from '../../../helpers/urls';
import { Component } from '../../../types/types';

interface Props {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
}

export default function CleanAsYouCodeWarning({ component }: Props) {
  return (
    <>
      <Alert variant="warning">
        {component.qualityGate ? (
          <FormattedMessage
            id="overview.quality_gate.conditions.cayc.warning"
            defaultMessage={translate('overview.quality_gate.conditions.cayc.warning')}
            values={{
              link: (
                <div
                  className="overview-quality-gate-alert-inline-link"
                  title={component.qualityGate.name}
                >
                  <Link to={getQualityGateUrl(component.qualityGate.key)}>
                    {component.qualityGate.name}
                  </Link>
                </div>
              ),
            }}
          />
        ) : (
          translate('overview.quality_gate.conditions.cayc.warning.no_link')
        )}
      </Alert>

      <p className="big-spacer-top big-spacer-bottom">
        {translate('overview.quality_gate.conditions.cayc.details')}
      </p>

      {component.qualityGate && (
        <div className="big-spacer-bottom">
          <Link className="button" to={getQualityGateUrl(component.qualityGate.key)}>
            {translate('overview.quality_gate.conditions.cayc.review')}
          </Link>
        </div>
      )}

      <Link
        target="_blank"
        to="https://docs.sonarqube.org/latest/user-guide/clean-as-you-code/#quality-gate"
      >
        {translate('overview.quality_gate.conditions.cayc.link')}
      </Link>
    </>
  );
}
