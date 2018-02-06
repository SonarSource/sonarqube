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
import * as React from 'react';
import * as classNames from 'classnames';
import { map } from 'lodash';
import HealthItem from './HealthItem';
import Section from './Section';
import OpenCloseIcon from '../../../../components/icons-components/OpenCloseIcon';
import { HealthType, SysValueObject } from '../../../../api/system';
import { LOGS_LEVELS, groupSections, getLogsLevel } from '../../utils';
import { translate } from '../../../../helpers/l10n';

interface Props {
  biggerHealth?: boolean;
  health?: HealthType;
  healthCauses?: string[];
  onClick: (toggledCard: string) => void;
  open: boolean;
  name: string;
  sysInfoData: SysValueObject;
}

interface State {
  hoveringDetail: boolean;
}

export default class HealthCard extends React.PureComponent<Props, State> {
  state: State = { hoveringDetail: false };

  handleClick = () => this.props.onClick(this.props.name);
  onDetailEnter = () => this.setState({ hoveringDetail: true });
  onDetailLeave = () => this.setState({ hoveringDetail: false });

  render() {
    const { health, open, sysInfoData } = this.props;
    const { mainSection, sections } = groupSections(sysInfoData);
    const showFields = open && mainSection && Object.keys(mainSection).length > 0;
    const showSections = open && sections;
    const logLevel = getLogsLevel(sysInfoData);
    const showLogLevelWarning = logLevel && logLevel !== LOGS_LEVELS[0];
    return (
      <li
        className={classNames('boxed-group system-info-health-card', {
          'no-hover': this.state.hoveringDetail
        })}>
        <div className="boxed-group-header" onClick={this.handleClick}>
          <span className="system-info-health-card-title">
            <OpenCloseIcon className="little-spacer-right" open={open} />
            {this.props.name}
          </span>
          {showLogLevelWarning && (
            <span className="alert alert-danger spacer-left">
              {translate('system.log_level.warning.short')}
            </span>
          )}
          {health && (
            <HealthItem
              biggerHealth={this.props.biggerHealth}
              className="pull-right"
              health={health}
              healthCauses={this.props.healthCauses}
              name={this.props.name}
            />
          )}
        </div>
        {open && (
          <div
            className="boxed-group-inner"
            onMouseEnter={this.onDetailEnter}
            onMouseLeave={this.onDetailLeave}>
            {showFields && <Section items={mainSection} />}
            {showSections &&
              map(sections, (section, name) => <Section key={name} items={section} name={name} />)}
          </div>
        )}
      </li>
    );
  }
}
