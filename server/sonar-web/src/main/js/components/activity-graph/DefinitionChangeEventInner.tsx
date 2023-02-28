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
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { isMainBranch } from '../../helpers/branch-like';
import { translate } from '../../helpers/l10n';
import { limitComponentName } from '../../helpers/path';
import { getProjectUrl } from '../../helpers/urls';
import { BranchLike } from '../../types/branch-like';
import {
  AnalysisEvent,
  ApplicationAnalysisEventCategory,
  DefinitionChangeType,
} from '../../types/project-activity';
import Link from '../common/Link';
import { ButtonLink } from '../controls/buttons';
import ClickEventBoundary from '../controls/ClickEventBoundary';
import BranchIcon from '../icons/BranchIcon';
import DropdownIcon from '../icons/DropdownIcon';

export type DefinitionChangeEvent = AnalysisEvent &
  Required<Pick<AnalysisEvent, 'definitionChange'>>;

export function isDefinitionChangeEvent(event: AnalysisEvent): event is DefinitionChangeEvent {
  return (
    event.category === ApplicationAnalysisEventCategory.DefinitionChange &&
    event.definitionChange !== undefined
  );
}

interface Props {
  branchLike: BranchLike | undefined;
  event: DefinitionChangeEvent;
  readonly?: boolean;
}

interface State {
  expanded: boolean;
}

const NAME_MAX_LENGTH = 28;

export class DefinitionChangeEventInner extends React.PureComponent<Props, State> {
  state: State = { expanded: false };

  toggleProjectsList = () => {
    this.setState((state) => ({ expanded: !state.expanded }));
  };

  renderProjectLink = (project: { key: string; name: string }, branch: string | undefined) => (
    <ClickEventBoundary>
      <Link title={project.name} to={getProjectUrl(project.key, branch)}>
        {limitComponentName(project.name, NAME_MAX_LENGTH)}
      </Link>
    </ClickEventBoundary>
  );

  renderBranch = (branch = translate('branches.main_branch')) => (
    <span className="nowrap" title={branch}>
      <BranchIcon className="little-spacer-left text-text-top" />
      {branch}
    </span>
  );

  renderProjectChange(project: {
    changeType: DefinitionChangeType;
    key: string;
    name: string;
    branch?: string;
    newBranch?: string;
    oldBranch?: string;
  }) {
    const mainBranch = !this.props.branchLike || isMainBranch(this.props.branchLike);

    switch (project.changeType) {
      case DefinitionChangeType.Added: {
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
                branch: this.renderBranch(project.branch),
              }}
            />
          </div>
        );
      }

      case DefinitionChangeType.Removed: {
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
                branch: this.renderBranch(project.branch),
              }}
            />
          </div>
        );
      }

      case DefinitionChangeType.BranchChanged:
        return (
          <FormattedMessage
            defaultMessage={translate('event.definition_change.branch_replaced')}
            id="event.definition_change.branch_replaced"
            values={{
              project: this.renderProjectLink(project, project.newBranch),
              oldBranch: this.renderBranch(project.oldBranch),
              newBranch: this.renderBranch(project.newBranch),
            }}
          />
        );
    }
  }

  render() {
    const { event, readonly } = this.props;
    const { expanded } = this.state;
    return (
      <>
        <span className="note">
          {translate('event.category', event.category)}
          {!readonly && ':'}
        </span>

        {!readonly && (
          <div>
            <ButtonLink
              className="project-activity-event-inner-more-link"
              onClick={this.toggleProjectsList}
              stopPropagation={true}
            >
              {expanded ? translate('hide') : translate('more')}
              <DropdownIcon className="little-spacer-left" turned={expanded} />
            </ButtonLink>
          </div>
        )}

        {expanded && (
          <ul className="spacer-left spacer-top">
            {event.definitionChange.projects.map((project) => (
              <li className="display-flex-center spacer-top" key={project.key}>
                {this.renderProjectChange(project)}
              </li>
            ))}
          </ul>
        )}
      </>
    );
  }
}
