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

export default class HeadingsLink extends React.Component {
  componentDidMount() {
    document.addEventListener('scroll', this.scrollHandler, true);
  }

  componentWillUnmount() {
    document.removeEventListener('scroll', this.scrollHandler, true);
  }

  highlightHeading = (index, scrollTo) => {
    const previousNode = document.querySelector('.targetted-heading');
    if (previousNode) {
      previousNode.classList.remove('targetted-heading');
    }

    const node = document.querySelector('#header-' + index);
    if (node) {
      node.classList.add('targetted-heading');
      if (scrollTo) {
        window.scrollTo(0, node.offsetTop - 30);
      }
    }
  };

  scrollHandler = () => {
    const headings = Array.from(document.querySelectorAll('.headings-container ul li a'));
    const scrollTop = window.pageYOffset | document.body.scrollTop;
    let headingIndex = 0;
    for (let i = 0; i < headings.length; i++) {
      if (document.querySelector('#header-' + (i + 1)).offsetTop > scrollTop + 40) {
        break;
      }
      headingIndex = i;
    }
    headings.forEach(h => h.classList.remove('active'));
    headings[headingIndex].classList.add('active');
    this.highlightHeading(headingIndex + 1, false);
  };

  clickHandler = target => {
    return event => {
      event.stopPropagation();
      event.preventDefault();
      this.highlightHeading(target, true);
    };
  };

  render() {
    const headers = this.props.headers.filter(
      h => h.depth === 2 && h.value.toLowerCase() !== 'table of contents'
    );
    if (headers.length < 1) {
      return null;
    }

    return (
      <div className="headings-container">
        <ul>
          {headers.map((header, index) => {
            return (
              <li key={index + 1}>
                <a onClick={this.clickHandler(index + 1)} href={'#header-' + (index + 1)}>
                  {header.value}
                </a>
              </li>
            );
          })}
        </ul>
      </div>
    );
  }
}
