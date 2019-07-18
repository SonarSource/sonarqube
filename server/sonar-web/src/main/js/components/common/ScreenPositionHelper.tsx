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
import { debounce } from 'lodash';
import * as React from 'react';

interface Props {
  className?: string;
  children: (position: { top: number; left: number }) => React.ReactElement<any>;
}

export default class ScreenPositionHelper extends React.PureComponent<Props> {
  container?: HTMLDivElement;
  debouncedOnResize: () => void;

  constructor(props: Props) {
    super(props);
    this.debouncedOnResize = debounce(() => this.forceUpdate(), 250);
  }

  componentDidMount() {
    this.forceUpdate();
    window.addEventListener('resize', this.debouncedOnResize);
  }

  componentWillUnmount() {
    window.removeEventListener('resize', this.debouncedOnResize);
  }

  getPosition = () => {
    const containerPos = this.container && this.container.getBoundingClientRect();
    if (!containerPos) {
      return { top: 0, left: 0 };
    }
    return {
      top: window.pageYOffset + containerPos.top,
      left: window.pageXOffset + containerPos.left
    };
  };

  render() {
    return (
      <div
        className={this.props.className}
        ref={container => (this.container = container as HTMLDivElement)}>
        {this.props.children(this.getPosition())}
      </div>
    );
  }
}
