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
import ProjectEventIcon from '../../../components/icons-components/ProjectEventIcon';

export interface Option {
  label: string;
  value: string;
}

interface Props {
  option: Option;
  children?: Element | Text;
  className?: string;
  isFocused?: boolean;
  onFocus: (option: Option, event: React.MouseEvent<HTMLDivElement>) => void;
  onSelect: (option: Option, event: React.MouseEvent<HTMLDivElement>) => void;
}

export default class ProjectActivityEventSelectOption extends React.PureComponent<Props> {
  handleMouseDown = (event: React.MouseEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.stopPropagation();
    this.props.onSelect(this.props.option, event);
  };

  handleMouseEnter = (event: React.MouseEvent<HTMLDivElement>) => {
    this.props.onFocus(this.props.option, event);
  };

  handleMouseMove = (event: React.MouseEvent<HTMLDivElement>) => {
    if (this.props.isFocused) {
      return;
    }
    this.props.onFocus(this.props.option, event);
  };

  render() {
    const { option } = this.props;
    return (
      <div
        className={this.props.className}
        onMouseDown={this.handleMouseDown}
        onMouseEnter={this.handleMouseEnter}
        onMouseMove={this.handleMouseMove}
        role="link"
        tabIndex={0}
        title={option.label}>
        <ProjectEventIcon className={'project-activity-event-icon ' + option.value} />
        <span className="little-spacer-left">{this.props.children}</span>
      </div>
    );
  }
}
