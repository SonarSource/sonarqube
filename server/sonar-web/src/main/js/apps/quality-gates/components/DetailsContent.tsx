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
import HelpTooltip from '../../../components/controls/HelpTooltip';
import { Alert } from '../../../components/ui/Alert';
import { translate } from '../../../helpers/l10n';
import { Condition, QualityGate } from '../../../types/types';
import Conditions from './Conditions';
import Projects from './Projects';
import QualityGatePermissions from './QualityGatePermissions';

export interface DetailsContentProps {
  isDefault?: boolean;
  onAddCondition: (condition: Condition) => void;
  onRemoveCondition: (Condition: Condition) => void;
  onSaveCondition: (newCondition: Condition, oldCondition: Condition) => void;
  qualityGate: QualityGate;
  updatedConditionId?: string;
}

export function DetailsContent(props: DetailsContentProps) {
  const { isDefault, qualityGate, updatedConditionId } = props;
  const actions = qualityGate.actions || {};

  return (
    <div className="layout-page-main-inner">
      {isDefault &&
        (qualityGate.conditions === undefined || qualityGate.conditions.length === 0) && (
          <Alert className="big-spacer-bottom" variant="warning">
            {translate('quality_gates.is_default_no_conditions')}
          </Alert>
        )}

      <Conditions
        onAddCondition={props.onAddCondition}
        onRemoveCondition={props.onRemoveCondition}
        onSaveCondition={props.onSaveCondition}
        qualityGate={qualityGate}
        updatedConditionId={updatedConditionId}
      />

      <div className="display-flex-row huge-spacer-top">
        <div className="quality-gate-section width-50 big-padded-right" id="quality-gate-projects">
          <header className="display-flex-center spacer-bottom">
            <h3>{translate('quality_gates.projects')}</h3>
            <HelpTooltip
              className="spacer-left"
              overlay={
                <div className="big-padded-top big-padded-bottom">
                  {translate('quality_gates.projects.help')}
                </div>
              }
            />
          </header>
          {isDefault ? (
            translate('quality_gates.projects_for_default')
          ) : (
            <Projects
              canEdit={actions.associateProjects}
              // pass unique key to re-mount the component when the quality gate changes
              key={qualityGate.id}
              qualityGate={qualityGate}
            />
          )}
        </div>
        {actions.delegate && (
          <div className="width-50 big-padded-left">
            <QualityGatePermissions qualityGate={qualityGate} />
          </div>
        )}
      </div>
    </div>
  );
}

export default React.memo(DetailsContent);
