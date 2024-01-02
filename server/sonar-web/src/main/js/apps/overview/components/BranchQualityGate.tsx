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
import { HelperHintIcon, LightPrimary, QualityGateIndicator, TextMuted } from 'design-system';
import React from 'react';
import { useIntl } from 'react-intl';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { BranchLike } from '../../../types/branch-like';
import { QualityGateStatusConditionEnhanced } from '../../../types/quality-gates';
import { Component, Status } from '../../../types/types';
import BranchQualityGateConditions from './BranchQualityGateConditions';

interface Props {
  status: Status;
  branchLike?: BranchLike;
  component: Pick<Component, 'key'>;
  failedConditions: QualityGateStatusConditionEnhanced[];
}

export default function BranchQualityGate(props: Readonly<Props>) {
  const { status, branchLike, component, failedConditions } = props;

  return (
    <>
      <BranchQGStatus status={status} />
      <BranchQualityGateConditions
        branchLike={branchLike}
        component={component}
        failedConditions={failedConditions}
      />
    </>
  );
}

function BranchQGStatus({ status }: Readonly<Pick<Props, 'status'>>) {
  const intl = useIntl();

  return (
    <div className="sw-flex sw-items-center sw-mb-5">
      <QualityGateIndicator
        status={status}
        className="sw-mr-2"
        size="xl"
        ariaLabel={intl.formatMessage(
          { id: 'overview.quality_gate_x' },
          { '0': intl.formatMessage({ id: `overview.gate.${status}` }) },
        )}
      />
      <div className="sw-flex sw-flex-col sw-justify-around">
        <div className="sw-flex sw-items-center">
          <TextMuted
            className="sw-body-sm"
            text={intl.formatMessage({ id: 'overview.quality_gate' })}
          />
          <HelpTooltip
            className="sw-ml-2"
            overlay={intl.formatMessage({ id: 'overview.quality_gate.help' })}
          >
            <HelperHintIcon aria-label="help-tooltip" />
          </HelpTooltip>
        </div>
        <div>
          <LightPrimary as="h1" className="sw-heading-xl">
            {intl.formatMessage({ id: `metric.level.${status}` })}
          </LightPrimary>
        </div>
      </div>
    </div>
  );
}
