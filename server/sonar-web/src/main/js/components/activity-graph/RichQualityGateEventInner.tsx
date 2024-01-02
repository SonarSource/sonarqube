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
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getProjectUrl } from '../../helpers/urls';
import { AnalysisEvent } from '../../types/project-activity';
import Link from '../common/Link';
import { ResetButtonLink } from '../controls/buttons';
import DropdownIcon from '../icons/DropdownIcon';
import Level from '../ui/Level';

export type RichQualityGateEvent = AnalysisEvent & Required<Pick<AnalysisEvent, 'qualityGate'>>;

export function isRichQualityGateEvent(event: AnalysisEvent): event is RichQualityGateEvent {
  return event.category === 'QUALITY_GATE' && event.qualityGate !== undefined;
}

interface Props {
  event: RichQualityGateEvent;
  readonly?: boolean;
}

interface State {
  expanded: boolean;
}

export class RichQualityGateEventInner extends React.PureComponent<Props, State> {
  state: State = { expanded: false };

  stopPropagation = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.stopPropagation();
  };

  toggleProjectsList = () => {
    this.setState((state) => ({ expanded: !state.expanded }));
  };

  render() {
    const { event, readonly } = this.props;
    const { expanded } = this.state;
    return (
      <>
        <span className="note spacer-right">{translate('event.category', event.category)}:</span>
        {event.qualityGate.stillFailing ? (
          <FormattedMessage
            defaultMessage={translate('event.quality_gate.still_x')}
            id="event.quality_gate.still_x"
            values={{ status: <Level level={event.qualityGate.status} small={true} /> }}
          />
        ) : (
          <Level level={event.qualityGate.status} small={true} />
        )}

        <div>
          {!readonly && event.qualityGate.failing.length > 0 && (
            <ResetButtonLink
              className="project-activity-event-inner-more-link"
              onClick={this.toggleProjectsList}
              stopPropagation={true}
            >
              {expanded ? translate('hide') : translate('more')}
              <DropdownIcon className="little-spacer-left" turned={expanded} />
            </ResetButtonLink>
          )}
        </div>

        {expanded && (
          <ul className="spacer-left spacer-top">
            {event.qualityGate.failing.map((project) => (
              <li className="display-flex-center spacer-top" key={project.key}>
                <Level
                  aria-label={translate('quality_gates.status')}
                  className="spacer-right"
                  level={event.qualityGate.status}
                  small={true}
                />
                <div className="flex-1 text-ellipsis">
                  <Link
                    onClick={this.stopPropagation}
                    title={project.name}
                    to={getProjectUrl(project.key, project.branch)}
                  >
                    <span aria-label={translateWithParameters('project_x', project.name)}>
                      {project.name}
                    </span>
                  </Link>
                </div>
              </li>
            ))}
          </ul>
        )}
      </>
    );
  }
}
