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
import { translate } from '../../../helpers/l10n';
import { BranchLike } from '../../../types/branch-like';
import { ComponentQualifier } from '../../../types/component';
import { IssueType } from '../../../types/issues';
import { Component, MeasureEnhanced } from '../../../types/types';
import IssueLabel from '../components/IssueLabel';
import IssueRating from '../components/IssueRating';
import DebtValue from './DebtValue';
import SecurityHotspotsReviewed from './SecurityHotspotsReviewed';

export interface MeasuresPanelIssueMeasureRowProps {
  branchLike?: BranchLike;
  component: Component;
  isNewCodeTab: boolean;
  measures: MeasureEnhanced[];
  type: IssueType;
}

export default function MeasuresPanelIssueMeasureRow(props: MeasuresPanelIssueMeasureRowProps) {
  const { branchLike, component, isNewCodeTab, measures, type } = props;

  const isApp = component.qualifier === ComponentQualifier.Application;

  return (
    <div
      className="display-flex-row overview-measures-row"
      data-test={`overview__measures-${type.toString().toLowerCase()}`}
    >
      {type === IssueType.CodeSmell ? (
        <>
          <div className="overview-panel-big-padded flex-1 small display-flex-center big-spacer-left">
            <DebtValue
              branchLike={branchLike}
              component={component}
              measures={measures}
              useDiffMetric={isNewCodeTab}
            />
          </div>
          <div className="flex-1 small display-flex-center">
            <IssueLabel
              branchLike={branchLike}
              component={component}
              measures={measures}
              type={type}
              useDiffMetric={isNewCodeTab}
            />
          </div>
        </>
      ) : (
        <div className="overview-panel-big-padded flex-1 small display-flex-center big-spacer-left">
          <IssueLabel
            branchLike={branchLike}
            component={component}
            helpTooltip={
              type === IssueType.SecurityHotspot
                ? translate('metric.security_hotspots.full_description')
                : undefined
            }
            measures={measures}
            type={type}
            useDiffMetric={isNewCodeTab}
          />
        </div>
      )}
      {type === IssueType.SecurityHotspot && (
        <div className="flex-1 small display-flex-center">
          <SecurityHotspotsReviewed measures={measures} useDiffMetric={isNewCodeTab} />
        </div>
      )}
      {(!isApp || !isNewCodeTab) && (
        <div className="overview-panel-big-padded overview-measures-aside display-flex-center">
          <IssueRating
            branchLike={branchLike}
            component={component}
            measures={measures}
            type={type}
            useDiffMetric={isNewCodeTab}
          />
        </div>
      )}
    </div>
  );
}
