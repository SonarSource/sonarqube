/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import { MarkdownHeading } from '../@types/graphql-types';
import HeadingAnchor from './HeadingAnchor';

const HEADER_SCROLL_MARGIN = 100;

interface Props {
  headers: MarkdownHeading[];
}

interface State {
  activeIndex: number;
  headers: MarkdownHeading[];
}

export default class HeadingsLink extends React.PureComponent<Props, State> {
  skipScrollingHandler = false;

  constructor(props: Props) {
    super(props);
    this.state = {
      activeIndex: -1,
      headers: props.headers.filter(
        h => h.depth === 2 && h.value && h.value.toLowerCase() !== 'table of contents'
      )
    };
  }

  componentDidMount() {
    document.addEventListener('scroll', this.scrollHandler, true);
  }

  componentWillReceiveProps(nextProps: Props) {
    this.setState({
      activeIndex: -1,
      headers: nextProps.headers.filter(
        h => h.depth === 2 && h.value && h.value.toLowerCase() !== 'table of contents'
      )
    });
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.scrollHandler, true);
  }

  scrollHandler = () => {
    if (this.skipScrollingHandler) {
      this.skipScrollingHandler = false;
      return;
    }

    const scrollTop = window.pageYOffset || document.body.scrollTop;
    this.highlightHeading(scrollTop);
  };

  highlightHeading = (scrollTop: number) => {
    let headingIndex = 0;
    for (let i = 0; i < this.state.headers.length; i++) {
      const headerItem = document.querySelector<HTMLElement>(`#header-${i + 1}`);
      if (headerItem && headerItem.offsetTop > scrollTop + HEADER_SCROLL_MARGIN) {
        break;
      }
      headingIndex = i;
    }
    this.setState({ activeIndex: headingIndex });
    this.markH2(headingIndex + 1, false);
  };

  markH2 = (index: number, scrollTo: boolean) => {
    const previousNode = document.querySelector('.targetted-heading');
    if (previousNode) {
      previousNode.classList.remove('targetted-heading');
    }

    const node = document.querySelector<HTMLElement>('#header-' + index);
    if (node) {
      node.classList.add('targetted-heading');
      if (scrollTo) {
        this.skipScrollingHandler = true;
        window.scrollTo(0, node.offsetTop - HEADER_SCROLL_MARGIN);
        this.highlightHeading(node.offsetTop - HEADER_SCROLL_MARGIN);
      }
    }
  };

  clickHandler = (index: number) => {
    this.markH2(index, true);
  };

  render() {
    const { headers } = this.state;
    if (headers.length < 2) {
      return null;
    }

    return (
      <div className="headings-container">
        <div className="headings-container-fixed">
          <span>On this page</span>
          <ul>
            {headers.map((header, index) => {
              return (
                <HeadingAnchor
                  active={this.state.activeIndex === index}
                  clickHandler={this.clickHandler}
                  index={index + 1}
                  key={index}>
                  {header.value}
                </HeadingAnchor>
              );
            })}
          </ul>
        </div>
      </div>
    );
  }
}
