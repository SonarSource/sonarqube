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
import { findDOMNode } from 'react-dom';
import { getWrappedDisplayName } from './utils';

export interface WithScrollToProps {
  selected?: boolean;
}

const TOP_OFFSET = 200;
const BOTTOM_OFFSET = 10;

export function withScrollTo<P>(WrappedComponent: React.ComponentClass<P>) {
  return class Wrapper extends React.Component<P & Partial<WithScrollToProps>> {
    componentRef?: React.Component | null;
    node?: Element | Text | null;

    static displayName = getWrappedDisplayName(WrappedComponent, 'withScrollTo');

    componentDidMount() {
      if (this.componentRef) {
        // eslint-disable-next-line react/no-find-dom-node
        this.node = findDOMNode(this.componentRef);
        this.handleUpdate();
      }
    }

    componentDidUpdate() {
      this.handleUpdate();
    }

    handleUpdate() {
      const { selected } = this.props;

      if (selected) {
        setTimeout(() => {
          this.handleScroll();
        }, 0);
      }
    }

    handleScroll() {
      if (this.node && this.node instanceof Element) {
        const position = this.node.getBoundingClientRect();
        const { top, bottom } = position;
        if (bottom > window.innerHeight - BOTTOM_OFFSET) {
          window.scrollTo(0, bottom - window.innerHeight + window.pageYOffset + BOTTOM_OFFSET);
        } else if (top < TOP_OFFSET) {
          window.scrollTo(0, top + window.pageYOffset - TOP_OFFSET);
        }
      }
    }

    render() {
      return (
        <WrappedComponent
          {...this.props}
          ref={ref => {
            this.componentRef = ref;
          }}
        />
      );
    }
  };
}
