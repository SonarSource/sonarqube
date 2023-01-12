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
import classNames from 'classnames';
import { flatMap } from 'lodash';
import * as React from 'react';
import Link from '../../../components/common/Link';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import { Alert } from '../../../components/ui/Alert';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import { getBranchLikeQuery } from '../../../helpers/branch-like';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import { getProjectQueryUrl } from '../../../helpers/urls';
import { ComponentQualifier, isApplication } from '../../../types/component';
import { QualityGateStatus } from '../../../types/quality-gates';
import { Component } from '../../../types/types';
import SonarLintPromotion from '../components/SonarLintPromotion';
import QualityGatePanelSection from './QualityGatePanelSection';

export interface QualityGatePanelProps {
  component: Pick<Component, 'key' | 'qualifier' | 'qualityGate'>;
  loading?: boolean;
  qgStatuses?: QualityGateStatus[];
}

export function QualityGatePanel(props: QualityGatePanelProps) {
  const { component, loading, qgStatuses = [] } = props;

  if (qgStatuses === undefined) {
    return null;
  }

  const overallLevel = qgStatuses.map((s) => s.status).includes('ERROR') ? 'ERROR' : 'OK';
  const success = overallLevel === 'OK';

  const overallFailedConditionsCount = qgStatuses.reduce(
    (acc, qgStatus) => acc + qgStatus.failedConditions.length,
    0
  );

  const nonCaycProjectsInApp = isApplication(component.qualifier)
    ? qgStatuses
        .filter(({ isCaycCompliant }) => !isCaycCompliant)
        .sort(({ name: a }, { name: b }) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
    : [];

  const showIgnoredConditionWarning =
    component.qualifier === ComponentQualifier.Project &&
    qgStatuses.some((p) => Boolean(p.ignoredConditions));

  return (
    <div className="overview-panel" data-test="overview__quality-gate-panel">
      <div className="display-flex-center spacer-bottom">
        <h2 className="overview-panel-title null-spacer-bottom">
          {translate('overview.quality_gate')}{' '}
        </h2>
        <HelpTooltip
          className="little-spacer-left"
          overlay={
            <div className="big-padded-top big-padded-bottom">
              {translate('overview.quality_gate.help')}
            </div>
          }
        />
      </div>
      {showIgnoredConditionWarning && (
        <Alert className="big-spacer-bottom" display="inline" variant="info">
          <span className="text-middle">
            {translate('overview.quality_gate.ignored_conditions')}
          </span>
          <HelpTooltip
            className="spacer-left"
            overlay={translate('overview.quality_gate.ignored_conditions.tooltip')}
          />
        </Alert>
      )}

      <div>
        {loading ? (
          <div className="overview-panel-big-padded">
            <DeferredSpinner loading={loading} />
          </div>
        ) : (
          <>
            <div
              className={classNames('overview-quality-gate-badge-large', {
                failed: !success,
                success,
              })}
            >
              <h3 className="big-spacer-bottom huge">{translate('metric.level', overallLevel)}</h3>

              <span className="small">
                {overallFailedConditionsCount > 0
                  ? translateWithParameters(
                      'overview.X_conditions_failed',
                      overallFailedConditionsCount
                    )
                  : translate('overview.quality_gate_all_conditions_passed')}
              </span>
            </div>

            {(overallFailedConditionsCount > 0 ||
              qgStatuses.some(({ isCaycCompliant }) => !isCaycCompliant)) && (
              <div data-test="overview__quality-gate-conditions">
                {qgStatuses.map((qgStatus) => (
                  <QualityGatePanelSection
                    component={component}
                    key={qgStatus.key}
                    qgStatus={qgStatus}
                  />
                ))}
              </div>
            )}

            {nonCaycProjectsInApp.length > 0 && (
              <div className="overview-quality-gate-conditions-list padded big-spacer-top">
                <Alert variant="warning">
                  {translateWithParameters(
                    'overview.quality_gate.application.non_cayc.projects_x',
                    nonCaycProjectsInApp.length
                  )}
                </Alert>
                <div className="spacer big-spacer-bottom big-spacer-top">
                  <Link
                    target="_blank"
                    to="https://docs.sonarqube.org/latest/user-guide/clean-as-you-code/#quality-gate"
                  >
                    {translate('overview.quality_gate.conditions.cayc.link')}
                  </Link>
                </div>
                <hr className="big-spacer-top big-spacer-bottom" />
                <ul className="spacer-left spacer-bottom">
                  {nonCaycProjectsInApp.map(({ key, name, branchLike }) => (
                    <li key={key} className="text-ellipsis spacer-bottom" title={name}>
                      <Link
                        className="link-no-underline"
                        to={getProjectQueryUrl(key, getBranchLikeQuery(branchLike))}
                      >
                        <QualifierIcon
                          className="little-spacer-right"
                          qualifier={ComponentQualifier.Project}
                        />
                        {name}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </>
        )}
      </div>
      <SonarLintPromotion
        qgConditions={flatMap(qgStatuses, (qgStatus) => qgStatus.failedConditions)}
      />
    </div>
  );
}

export default React.memo(QualityGatePanel);
