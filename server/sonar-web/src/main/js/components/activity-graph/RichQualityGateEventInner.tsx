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
  BareButton,
  ChevronDownIcon,
  Note,
  QualityGateIndicator,
  StandoutLink,
} from 'design-system';
import * as React from 'react';
import { FormattedMessage } from 'react-intl';
import { translate, translateWithParameters } from '../../helpers/l10n';
import { getProjectUrl } from '../../helpers/urls';
import { AnalysisEvent } from '../../types/project-activity';
import ClickEventBoundary from '../controls/ClickEventBoundary';

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

  toggleProjectsList = () => {
    this.setState((state) => ({ expanded: !state.expanded }));
  };

  render() {
    const { event, readonly } = this.props;
    const { expanded } = this.state;
    return (
      <div className="sw-w-full sw-typo-default sw-py-1/2">
        <div className="sw-flex sw-justify-between">
          <div className="sw-flex sw-items-center">
            <Note className="sw-mr-1 sw-typo-semibold">
              {translate('event.category', event.category)}
            </Note>

            <div className="sw-ml-2 sw-flex sw-items-center">
              {event.qualityGate.stillFailing ? (
                <FormattedMessage
                  defaultMessage={translate('event.quality_gate.still_x')}
                  id="event.quality_gate.still_x"
                  values={{
                    status: <QualityGateIndicator status={event.qualityGate.status} size="sm" />,
                  }}
                />
              ) : (
                <QualityGateIndicator status={event.qualityGate.status} size="sm" />
              )}

              <span className="sw-ml-1">
                {translate(`event.quality_gate.${event.qualityGate.status}`)}
              </span>
            </div>
          </div>

          {!readonly && event.qualityGate.failing.length > 0 && (
            <ClickEventBoundary>
              <BareButton
                className="sw-flex sw-items-center sw-ml-2"
                onClick={this.toggleProjectsList}
              >
                {expanded ? translate('hide') : translate('more')}
                <ChevronDownIcon transform={expanded ? 'rotate(180)' : undefined} />
              </BareButton>
            </ClickEventBoundary>
          )}
        </div>

        {expanded && (
          <ul className="sw-mt-2">
            {event.qualityGate.failing.map((project) => (
              <li className="sw-flex sw-justify-between sw-p-1" key={project.key}>
                <div className="sw-truncate">
                  <ClickEventBoundary>
                    <StandoutLink
                      title={project.name}
                      to={getProjectUrl(project.key, project.branch)}
                    >
                      <span aria-label={translateWithParameters('project_x', project.name)}>
                        {project.name}
                      </span>
                    </StandoutLink>
                  </ClickEventBoundary>
                </div>
                <div className="sw-flex sw-items-center sw-ml-2">
                  <QualityGateIndicator status={event.qualityGate.status} size="sm" />
                  <span className="sw-ml-2">
                    {translate(`event.quality_gate.${event.qualityGate.status}`)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>
    );
  }
}
