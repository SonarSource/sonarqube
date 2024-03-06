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
import { HelperHintIcon } from 'design-system';
import * as React from 'react';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import SoftwareImpactSeverityIcon from '../../../components/icon-mappers/SoftwareImpactSeverityIcon';
import { IMPACT_SEVERITIES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { SoftwareImpactSeverity } from '../../../types/clean-code-taxonomy';
import { CommonProps, SimpleListStyleFacet } from './SimpleListStyleFacet';

interface Props extends CommonProps {
  severities: SoftwareImpactSeverity[];
}

export function SeverityFacet(props: Props) {
  const { severities = [], ...rest } = props;

  return (
    <SimpleListStyleFacet
      property="impactSeverities"
      itemNamePrefix="severity"
      listItems={IMPACT_SEVERITIES}
      selectedItems={severities}
      renderIcon={(severity: string, disabled: boolean) => (
        <SoftwareImpactSeverityIcon severity={severity} disabled={disabled} />
      )}
      help={
        <DocumentationTooltip
          placement="right"
          content={
            <>
              <p>{translate('issues.facet.impactSeverities.help.line1')}</p>
              <p className="sw-mt-2">{translate('issues.facet.impactSeverities.help.line2')}</p>
            </>
          }
          links={[
            {
              href: '/user-guide/clean-code/introduction',
              label: translate('learn_more'),
            },
          ]}
        >
          <HelperHintIcon />
        </DocumentationTooltip>
      }
      {...rest}
    />
  );
}
