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

type State = {
  optionBarOpen: boolean
};

export default class App extends React.PureComponent {
  state: State = { optionBarOpen: false };

  componentDidMount() {
    const elem = document.querySelector('html');
    elem && elem.classList.add('dashboard-page');
  }

  componentWillUnmount() {
    const elem = document.querySelector('html');
    elem && elem.classList.remove('dashboard-page');
  }

  handleOptionBarToggle = (open: boolean) => this.setState({ optionBarOpen: open });

  render() {
    return (
      <div id="projects-page">
        {React.cloneElement(this.props.children, {
          optionBarOpen: this.state.optionBarOpen,
          optionBarToggle: this.handleOptionBarToggle
        })}
      </div>
    );
  }
}
