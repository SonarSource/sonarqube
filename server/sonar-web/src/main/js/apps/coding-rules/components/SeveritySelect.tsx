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

import { HelperText, Select } from '@sonarsource/echoes-react';
import { isEmpty } from 'lodash';
import { FormattedMessage, useIntl } from 'react-intl';
import SoftwareImpactSeverityIcon from '../../../components/icon-mappers/SoftwareImpactSeverityIcon';
import { SEVERITIES } from '../../../helpers/constants';
import { SoftwareImpactSeverity } from '../../../types/clean-code-taxonomy';

export interface SeveritySelectProps {
  id: string;
  impactSeverity?: boolean;
  isDisabled: boolean;
  onChange: (value: string) => void;
  recommendedSeverity: string;
  severity: string;
}

export function SeveritySelect(props: SeveritySelectProps) {
  const { isDisabled, severity, recommendedSeverity, impactSeverity, id } = props;
  const intl = useIntl();
  const getSeverityTranslation = (severity: string) =>
    impactSeverity
      ? intl.formatMessage({ id: `severity_impact.${severity}` })
      : intl.formatMessage({ id: `severity.${severity}` });
  const severityOption = (impactSeverity ? Object.values(SoftwareImpactSeverity) : SEVERITIES).map(
    (severity) => ({
      label:
        severity === recommendedSeverity
          ? intl.formatMessage(
              { id: 'coding_rules.custom_severity.severity_with_recommended' },
              { severity: getSeverityTranslation(severity) },
            )
          : getSeverityTranslation(severity),
      value: severity,
      prefix: <SoftwareImpactSeverityIcon severity={severity} aria-hidden />,
    }),
  );

  return (
    <>
      <Select
        id={id}
        isDisabled={isDisabled}
        onChange={props.onChange}
        data={severityOption}
        isSearchable={false}
        isNotClearable
        placeholder={
          isDisabled && !isEmpty(severity) ? intl.formatMessage({ id: 'not_impacted' }) : undefined
        }
        value={severity}
        valueIcon={<SoftwareImpactSeverityIcon severity={severity} aria-hidden />}
      />
      {severity !== recommendedSeverity && (
        <HelperText className="sw-mt-2">
          <FormattedMessage
            id="coding_rules.custom_severity.not_recommended"
            values={{
              recommended: (
                <b className="sw-lowercase">{getSeverityTranslation(recommendedSeverity)}</b>
              ),
            }}
          />
        </HelperText>
      )}
    </>
  );
}
