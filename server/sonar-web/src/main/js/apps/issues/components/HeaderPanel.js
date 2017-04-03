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
// @flow
import React from 'react';
import { css, media } from 'glamor';
import { clearfix } from 'glamor/utils';
import { throttle } from 'lodash';

type Props = {|
  border: boolean,
  children?: React.Element<*>,
  top?: number
|};

type State = {
  scrolled: boolean
};

export default class HeaderPanel extends React.PureComponent {
  props: Props;
  state: State;

  constructor(props: Props) {
    super(props);
    this.state = { scrolled: this.isScrolled() };
    this.handleScroll = throttle(this.handleScroll, 50);
  }

  componentDidMount() {
    if (this.props.top != null) {
      window.addEventListener('scroll', this.handleScroll);
    }
  }

  componentWillUnmount() {
    if (this.props.top != null) {
      window.removeEventListener('scroll', this.handleScroll);
    }
  }

  isScrolled = () => window.scrollY > 10;

  handleScroll = () => {
    this.setState({ scrolled: this.isScrolled() });
  };

  render() {
    const commonStyles = {
      height: 56,
      lineHeight: '24px',
      padding: '16px 20px',
      boxSizing: 'border-box',
      borderBottom: this.props.border ? '1px solid #e6e6e6' : undefined,
      backgroundColor: '#f3f3f3'
    };

    const inner = this.props.top
      ? <div
          className={css(
            commonStyles,
            {
              position: 'fixed',
              zIndex: 30,
              top: this.props.top,
              left: 'calc(50vw - 360px + 1px)',
              right: 0,
              boxShadow: this.state.scrolled ? '0 2px 4px rgba(0, 0, 0, .125)' : 'none',
              transition: 'box-shadow 0.3s ease'
            },
            media('(max-width: 1320px)', { left: 301 })
          )}>
          {this.props.children}
        </div>
      : this.props.children;

    return (
      <div
        className={css(clearfix(), commonStyles, {
          marginTop: -20,
          marginBottom: 20,
          marginLeft: -20,
          marginRight: -20,
          '& .component-name': { lineHeight: '24px' }
        })}>
        {inner}
      </div>
    );
  }
}
