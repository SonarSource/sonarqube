/*
 * Sonar UI Common
 * Copyright (C) 2019-2020 SonarSource SA
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
import { throttle } from 'lodash';
import * as React from 'react';
import './NavBar.css';

interface Props extends React.HTMLProps<HTMLDivElement> {
  children?: React.ReactNode;
  className?: string;
  height: number;
  limited?: boolean;
  top?: number;
  notif?: React.ReactNode;
}

interface State {
  left: number;
}

export default class NavBar extends React.PureComponent<Props, State> {
  throttledFollowHorizontalScroll: () => void;

  constructor(props: Props) {
    super(props);
    this.state = { left: 0 };
    this.throttledFollowHorizontalScroll = throttle(this.followHorizontalScroll, 10);
  }

  componentDidMount() {
    document.addEventListener('scroll', this.throttledFollowHorizontalScroll);
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.throttledFollowHorizontalScroll);
  }

  followHorizontalScroll = () => {
    if (document.documentElement) {
      this.setState({ left: -document.documentElement.scrollLeft });
    }
  };

  render() {
    const { children, className, height, limited = true, top, notif, ...other } = this.props;
    return (
      <nav {...other} className={classNames('navbar', className)} style={{ height, top }}>
        <div
          className={classNames('navbar-inner', { 'navbar-inner-with-notif': notif != null })}
          style={{ height, left: this.state.left }}>
          <div className={classNames('clearfix', { 'navbar-limited': limited })}>{children}</div>
          {notif}
        </div>
      </nav>
    );
  }
}
