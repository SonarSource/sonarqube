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
import * as classNames from 'classnames';
import OpenCloseIcon from '../icons-components/OpenCloseIcon';

interface Props {
  children: React.ReactNode;
  className?: string;
  data?: string;
  onClick: (data?: string) => void;
  open: boolean;
  renderHeader?: () => React.ReactNode;
  title: React.ReactNode;
}

interface State {
  hoveringInner: boolean;
}

export default class BoxedGroupAccordion extends React.PureComponent<Props, State> {
  state: State = { hoveringInner: false };

  handleClick = () => {
    this.props.onClick(this.props.data);
  };

  onDetailEnter = () => {
    this.setState({ hoveringInner: true });
  };

  onDetailLeave = () => {
    this.setState({ hoveringInner: false });
  };

  render() {
    const { className, open, renderHeader, title } = this.props;
    return (
      <div
        className={classNames('boxed-group boxed-group-accordion', className, {
          'no-hover': this.state.hoveringInner
        })}>
        <div className="boxed-group-header" onClick={this.handleClick} role="listitem">
          <span className="boxed-group-accordion-title">
            <OpenCloseIcon className="little-spacer-right" open={open} />
            {title}
          </span>
          {renderHeader && renderHeader()}
        </div>
        {open && (
          <div
            className="boxed-group-inner"
            onMouseEnter={this.onDetailEnter}
            onMouseLeave={this.onDetailLeave}>
            {this.props.children}
          </div>
        )}
      </div>
    );
  }
}
