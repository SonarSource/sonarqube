/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import React from 'react';
import Conditions from './Conditions';
import Projects from './Projects';
import { translate } from '../../../helpers/l10n';

export default class DetailsContent extends React.PureComponent {
  render() {
    const { gate, metrics, organization } = this.props;
    const { onAddCondition, onDeleteCondition, onSaveCondition } = this.props;
    const conditions = gate.conditions || [];
    const actions = gate.actions || {};

    const defaultMessage = actions.associateProjects
      ? translate('quality_gates.projects_for_default.edit')
      : translate('quality_gates.projects_for_default');

    return (
      <div className="layout-page-main-inner">
        <Conditions
          qualityGate={gate}
          conditions={conditions}
          metrics={metrics}
          edit={actions.manageConditions}
          onAddCondition={onAddCondition}
          onSaveCondition={onSaveCondition}
          onDeleteCondition={onDeleteCondition}
          organization={organization}
        />

        <div id="quality-gate-projects" className="quality-gate-section">
          <h3 className="spacer-bottom">{translate('quality_gates.projects')}</h3>
          {gate.isDefault ? (
            defaultMessage
          ) : (
            <Projects
              qualityGate={gate}
              edit={actions.associateProjects}
              organization={organization}
            />
          )}
        </div>
      </div>
    );
  }
}
