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

import {
  DropdownMenu,
  DropdownMenuAlign,
  Popover,
  Spinner,
  Tooltip,
} from '@sonarsource/echoes-react';
import classNames from 'classnames';
import { noop } from 'lodash';
import { useState } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { Pill, PillVariant } from '~design-system';
import { IMPACT_SEVERITIES } from '../../helpers/constants';
import { DocLink } from '../../helpers/doc-links';
import { translate } from '../../helpers/l10n';
import { SoftwareImpactSeverity, SoftwareQuality } from '../../types/clean-code-taxonomy';
import DocumentationLink from '../common/DocumentationLink';
import SoftwareImpactSeverityIcon from '../icon-mappers/SoftwareImpactSeverityIcon';

export interface Props {
  className?: string;
  onSetSeverity?: (severity: SoftwareImpactSeverity, quality: SoftwareQuality) => Promise<void>;
  severity: SoftwareImpactSeverity;
  softwareQuality: SoftwareQuality;
  type?: 'issue' | 'rule';
}

export default function SoftwareImpactPill(props: Props) {
  const { className, severity, softwareQuality, type = 'issue', onSetSeverity } = props;
  const intl = useIntl();
  const quality = getQualityLabel(softwareQuality);
  const [updatingSeverity, setUpdatingSeverity] = useState(false);

  const variant = {
    [SoftwareImpactSeverity.Blocker]: PillVariant.Critical,
    [SoftwareImpactSeverity.High]: PillVariant.Danger,
    [SoftwareImpactSeverity.Medium]: PillVariant.Warning,
    [SoftwareImpactSeverity.Low]: PillVariant.Caution,
    [SoftwareImpactSeverity.Info]: PillVariant.Info,
  }[severity];

  const pill = (
    <Pill
      className={classNames('sw-flex sw-gap-1 sw-items-center', className)}
      onClick={noop}
      variant={variant}
    >
      {quality}
      <Spinner isLoading={updatingSeverity} className="sw-ml-1/2">
        <SoftwareImpactSeverityIcon
          width={14}
          height={14}
          severity={severity}
          data-guiding-id="issue-3"
        />
      </Spinner>
    </Pill>
  );

  const handleSetSeverity = async (severity: SoftwareImpactSeverity, quality: SoftwareQuality) => {
    setUpdatingSeverity(true);
    await onSetSeverity?.(severity, quality);
    setUpdatingSeverity(false);
  };

  if (onSetSeverity && type === 'issue') {
    return (
      <DropdownMenu.Root
        align={DropdownMenuAlign.Start}
        items={IMPACT_SEVERITIES.map((impactSeverity) => (
          <DropdownMenu.ItemButtonCheckable
            key={impactSeverity}
            isDisabled={impactSeverity === severity}
            isChecked={impactSeverity === severity}
            onClick={() => handleSetSeverity(impactSeverity, softwareQuality)}
          >
            <div className="sw-flex sw-items-center sw-gap-2">
              <SoftwareImpactSeverityIcon width={14} height={14} severity={impactSeverity} />
              {translate('severity_impact', impactSeverity)}
            </div>
          </DropdownMenu.ItemButtonCheckable>
        ))}
      >
        <Tooltip
          content={intl.formatMessage(
            {
              id: `issue.type.tooltip_with_change`,
            },
            {
              severity: intl.formatMessage({ id: `severity_impact.${severity}` }),
            },
          )}
        >
          {pill}
        </Tooltip>
      </DropdownMenu.Root>
    );
  }

  return (
    <Popover
      title={intl.formatMessage(
        { id: 'severity_impact.title' },
        { x: translate('severity_impact', severity) },
      )}
      description={
        <>
          <FormattedMessage
            id={`${type}.impact.severity.tooltip`}
            values={{
              severity: translate('severity_impact', severity).toLowerCase(),
              quality: quality.toLowerCase(),
            }}
          />
          <div className="sw-mt-2">
            {intl.formatMessage(
              { id: `severity_impact.help.description` },
              {
                p1: (text) => <p>{text}</p>,
                p: (text) => (type === 'issue' ? <p className="sw-mt-2">{text}</p> : ''),
              },
            )}
          </div>
        </>
      }
      footer={
        <DocumentationLink shouldOpenInNewTab standalone to={DocLink.MQRSeverity}>
          {translate('severity_impact.help.link')}
        </DocumentationLink>
      }
    >
      <Tooltip
        content={intl.formatMessage(
          {
            id: `issue.type.tooltip`,
          },
          {
            severity: intl.formatMessage({ id: `severity_impact.${severity}` }),
          },
        )}
      >
        {pill}
      </Tooltip>
    </Popover>
  );
}

const getQualityLabel = (quality: SoftwareQuality) => translate('software_quality', quality);
