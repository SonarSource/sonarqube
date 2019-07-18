/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import { translate } from 'sonar-ui-common/helpers/l10n';
import DocTooltip from '../../../components/docs/DocTooltip';
import Conditions from './Conditions';
import Projects from './Projects';

interface Props {
  isDefault?: boolean;
  metrics: T.Dict<T.Metric>;
  organization?: string;
  onAddCondition: (condition: T.Condition) => void;
  onRemoveCondition: (Condition: T.Condition) => void;
  onSaveCondition: (newCondition: T.Condition, oldCondition: T.Condition) => void;
  qualityGate: T.QualityGate;
}

export default class DetailsContent extends React.PureComponent<Props> {
  render() {
    const { isDefault, metrics, organization, qualityGate } = this.props;
    const conditions = qualityGate.conditions || [];
    const actions = qualityGate.actions || ({} as any);

    return (
      <div className="layout-page-main-inner">
        <Conditions
          canEdit={actions.manageConditions}
          conditions={conditions}
          metrics={metrics}
          onAddCondition={this.props.onAddCondition}
          onRemoveCondition={this.props.onRemoveCondition}
          onSaveCondition={this.props.onSaveCondition}
          organization={organization}
          qualityGate={qualityGate}
        />

        <div className="quality-gate-section" id="quality-gate-projects">
          <header className="display-flex-center spacer-bottom">
            <h3>{translate('quality_gates.projects')}</h3>
            <DocTooltip
              className="spacer-left"
              doc={import(/* webpackMode: "eager" */ 'Docs/tooltips/quality-gates/quality-gate-projects.md')}
            />
          </header>
          {isDefault ? (
            translate('quality_gates.projects_for_default')
          ) : (
            <Projects
              canEdit={actions.associateProjects}
              // pass unique key to re-mount the component when the quality gate changes
              key={qualityGate.id}
              organization={organization}
              qualityGate={qualityGate}
            />
          )}
        </div>
      </div>
    );
  }
}
