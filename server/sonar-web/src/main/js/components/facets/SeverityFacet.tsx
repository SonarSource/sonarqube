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

import { Popover } from '@sonarsource/echoes-react';
import { BareButton, HelperHintIcon } from 'design-system';
import * as React from 'react';
import { useIntl } from 'react-intl';
import { IMPACT_SEVERITIES } from '../../helpers/constants';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import DocumentationLink from '../common/DocumentationLink';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';
import Facet, { BasicProps } from './Facet';

export default function SeverityFacet(props: Readonly<BasicProps>) {
  const intl = useIntl();
  const renderName = React.useCallback(
    (severity: string, disabled: boolean) => (
      <div className="sw-flex sw-items-center">
        <SoftwareImpactSeverityIcon severity={severity} disabled={disabled} />
        <span className="sw-ml-1">{translate('severity_impact', severity)}</span>
      </div>
    ),
    [],
  );

  const renderTextName = React.useCallback(
    (severity: string) => translate('severity_impact', severity),
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
        <Popover
          title={intl.formatMessage({ id: 'severity_impact.levels' })}
          description={
            <>
              <p>{intl.formatMessage({ id: 'severity_impact.help.line1' })}</p>
              <p className="sw-mt-2">{intl.formatMessage({ id: 'severity_impact.help.line2' })}</p>
            </>
          }
          footer={
            <DocumentationLink to={DocLink.CleanCodeIntroduction}>
              {intl.formatMessage({ id: 'learn_more' })}
            </DocumentationLink>
          }
        >
          <BareButton aria-label={intl.formatMessage({ id: 'more_information' })}>
            <HelperHintIcon />
          </BareButton>
        </Popover>
      }
    />
  );
}
