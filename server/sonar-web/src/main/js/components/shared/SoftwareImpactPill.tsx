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
import classNames from 'classnames';
import { Pill, PillVariant } from 'design-system';
import { noop } from 'lodash';
import React from 'react';
import { FormattedMessage } from 'react-intl';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { SoftwareImpactSeverity } from '../../types/clean-code-taxonomy';
import DocumentationLink from '../common/DocumentationLink';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';

export interface Props {
  className?: string;
  quality: string;
  severity: SoftwareImpactSeverity;
  type?: 'issue' | 'rule';
}

export default function SoftwareImpactPill(props: Props) {
  const { className, severity, quality, type = 'issue' } = props;

  const variant = {
    [SoftwareImpactSeverity.Blocker]: PillVariant.Critical,
    [SoftwareImpactSeverity.High]: PillVariant.Danger,
    [SoftwareImpactSeverity.Medium]: PillVariant.Warning,
    [SoftwareImpactSeverity.Low]: PillVariant.Caution,
    [SoftwareImpactSeverity.Info]: PillVariant.Info,
  }[severity];

  return (
    <Popover
      title={translate('severity_impact.title')}
      description={
        <>
          <FormattedMessage
            id={`${type}.impact.severity.tooltip`}
            values={{
              severity: translate('severity_impact', severity).toLowerCase(),
              quality: quality.toLowerCase(),
            }}
          />
          <p className="sw-mt-2">
            <span className="sw-mr-1">{translate('severity_impact.help.line1')}</span>
            {translate('severity_impact.help.line2')}
          </p>
        </>
      }
      footer={
        <DocumentationLink to={DocLink.CleanCodeIntroduction}>
          {translate('learn_more')}
        </DocumentationLink>
      }
    >
      <Pill
        className={classNames('sw-flex sw-gap-1 sw-items-center', className)}
        onClick={noop}
        variant={variant}
      >
        {quality}
        <SoftwareImpactSeverityIcon
          width={14}
          height={14}
          severity={severity}
          data-guiding-id="issue-3"
        />
      </Pill>
    </Popover>
  );
}
