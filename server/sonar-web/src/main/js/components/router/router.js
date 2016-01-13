/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
let listener;


export const RouterMixin = {
  getDefaultProps() {
    return { urlRoot: '/' };
  },

  getInitialState() {
    return { route: this.getRoute() };
  },

  getRoute() {
    let path = window.location.pathname;
    if (path.indexOf(this.props.urlRoot) === 0) {
      return path.substr(this.props.urlRoot.length);
    } else {
      return null;
    }
  },

  componentDidMount () {
    listener = this;
    window.addEventListener('popstate', this.handleRouteChange);
  },

  componentWillUnmount() {
    window.removeEventListener('popstate', this.handleRouteChange);
  },

  handleRouteChange() {
    let route = this.getRoute();
    this.setState({ route });
  },

  navigate (route) {
    let url = this.props.urlRoot + route + window.location.search + window.location.hash;
    window.history.pushState({ route }, document.title, url);
    this.setState({ route });
  }
};


export function navigate (route) {
  if (listener) {
    listener.navigate(route);
  }
}
