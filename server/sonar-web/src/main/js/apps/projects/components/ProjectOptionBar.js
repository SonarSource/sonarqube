/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
//@flow
import React from 'react';
import classNames from 'classnames';
import CloseIcon from '../../../components/icons-components/CloseIcon';
import PerspectiveSelect from './PerspectiveSelect';

type Props = {
  onPerspectiveChange: ({ view: string, visualization?: string }) => void,
  onToggleOptionBar: boolean => void,
  open: boolean,
  view: string,
  visualization?: string
};

export default class ProjectOptionBar extends React.PureComponent {
  props: Props;

  closeBar = (evt: Event & { currentTarget: HTMLElement }) => {
    evt.currentTarget.blur();
    evt.preventDefault();
    this.props.onToggleOptionBar(false);
  };

  render() {
    const { open } = this.props;
    return (
      <div className="projects-topbar">
        <div className={classNames('projects-topbar-actions', { open })}>
          <a
            className="projects-topbar-button projects-topbar-button-close"
            href="#"
            onClick={this.closeBar}>
            <CloseIcon />
          </a>
          <div className="projects-topbar-actions-inner">
            <PerspectiveSelect
              onChange={this.props.onPerspectiveChange}
              view={this.props.view}
              visualization={this.props.visualization}
            />
          </div>
        </div>
      </div>
    );
  }
}
