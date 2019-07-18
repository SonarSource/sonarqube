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
import * as classNames from 'classnames';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { Link } from 'react-router';
import { ResetButtonLink } from 'sonar-ui-common/components/controls/buttons';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import LongLivingBranchIcon from 'sonar-ui-common/components/icons/LongLivingBranchIcon';
import ProjectEventIcon from 'sonar-ui-common/components/icons/ProjectEventIcon';
import { translate } from 'sonar-ui-common/helpers/l10n';
import { limitComponentName } from 'sonar-ui-common/helpers/path';
import { isMainBranch } from '../../../helpers/branches';
import { getProjectUrl } from '../../../helpers/urls';

export type DefinitionChangeEvent = T.AnalysisEvent &
  Required<Pick<T.AnalysisEvent, 'definitionChange'>>;

export function isDefinitionChangeEvent(event: T.AnalysisEvent): event is DefinitionChangeEvent {
  return event.category === 'DEFINITION_CHANGE' && event.definitionChange !== undefined;
}

interface Props {
  branchLike: T.BranchLike | undefined;
  event: DefinitionChangeEvent;
}

interface State {
  expanded: boolean;
}

export class DefinitionChangeEventInner extends React.PureComponent<Props, State> {
  state: State = { expanded: false };

  stopPropagation = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.stopPropagation();
  };

  toggleProjectsList = () => {
    this.setState(state => ({ expanded: !state.expanded }));
  };

  renderProjectLink = (project: { key: string; name: string }, branch: string | undefined) => (
    <Link
      onClick={this.stopPropagation}
      title={project.name}
      to={getProjectUrl(project.key, branch)}>
      {limitComponentName(project.name, 28)}
    </Link>
  );

  renderBranch = (branch = translate('branches.main_branch')) => (
    <span className="nowrap" title={branch}>
      <LongLivingBranchIcon className="little-spacer-left text-text-top" />
      {branch}
    </span>
  );

  renderProjectChange(project: {
    changeType: string;
    key: string;
    name: string;
    branch?: string;
    newBranch?: string;
    oldBranch?: string;
  }) {
    const mainBranch = !this.props.branchLike || isMainBranch(this.props.branchLike);

    if (project.changeType === 'ADDED') {
      const message = mainBranch
        ? 'event.definition_change.added'
        : 'event.definition_change.branch_added';
      return (
        <div className="text-ellipsis">
          <FormattedMessage
            defaultMessage={translate(message)}
            id={message}
            values={{
              project: this.renderProjectLink(project, project.branch),
              branch: this.renderBranch(project.branch)
            }}
          />
        </div>
      );
    } else if (project.changeType === 'REMOVED') {
      const message = mainBranch
        ? 'event.definition_change.removed'
        : 'event.definition_change.branch_removed';
      return (
        <div className="text-ellipsis">
          <FormattedMessage
            defaultMessage={translate(message)}
            id={message}
            values={{
              project: this.renderProjectLink(project, project.branch),
              branch: this.renderBranch(project.branch)
            }}
          />
        </div>
      );
    } else if (project.changeType === 'BRANCH_CHANGED') {
      return (
        <FormattedMessage
          defaultMessage={translate('event.definition_change.branch_replaced')}
          id="event.definition_change.branch_replaced"
          values={{
            project: this.renderProjectLink(project, project.newBranch),
            oldBranch: this.renderBranch(project.oldBranch),
            newBranch: this.renderBranch(project.newBranch)
          }}
        />
      );
    }
    return null;
  }

  render() {
    const { event } = this.props;
    const { expanded } = this.state;
    return (
      <div className="project-activity-event-inner">
        <div className="project-activity-event-inner-main">
          <ProjectEventIcon
            className={classNames(
              'project-activity-event-icon',
              'little-spacer-right',
              event.category
            )}
          />

          <div className="project-activity-event-inner-text flex-1">
            <span className="note little-spacer-right">
              {translate('event.category', event.category)}
            </span>
          </div>

          <ResetButtonLink
            className="project-activity-event-inner-more-link"
            onClick={this.toggleProjectsList}
            stopPropagation={true}>
            {expanded ? translate('hide') : translate('more')}
            <DropdownIcon className="little-spacer-left" turned={expanded} />
          </ResetButtonLink>
        </div>

        {expanded && (
          <ul className="project-activity-event-inner-more-content">
            {event.definitionChange.projects.map(project => (
              <li className="display-flex-center little-spacer-top" key={project.key}>
                {this.renderProjectChange(project)}
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }
}
