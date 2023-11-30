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

import { HelperHintIcon } from 'design-system';
import * as React from 'react';
import DocumentationTooltip from '../../../components/common/DocumentationTooltip';
import SoftwareImpactSeverityIcon from '../../../components/icons/SoftwareImpactSeverityIcon';
import { IMPACT_SEVERITIES } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import Facet, { BasicProps } from './Facet';

export default function SeverityFacet(props: BasicProps) {
  const renderName = React.useCallback(
    (severity: string, disabled: boolean) => (
      <div className="sw-flex sw-items-center">
        <SoftwareImpactSeverityIcon severity={severity} disabled={disabled} />
        <span className="sw-ml-1">{translate('severity', severity)}</span>
      </div>
    ),
    [],
  );

  const renderTextName = React.useCallback(
    (severity: string) => translate('severity', severity),
    [],
  );

  return (
    <Facet
      {...props}
      options={IMPACT_SEVERITIES}
      property="impactSeverities"
      renderName={renderName}
      renderTextName={renderTextName}
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
              href: '/user-guide/clean-code',
              label: translate('learn_more'),
            },
          ]}
        >
          <HelperHintIcon />
        </DocumentationTooltip>
      }
    />
  );
}
