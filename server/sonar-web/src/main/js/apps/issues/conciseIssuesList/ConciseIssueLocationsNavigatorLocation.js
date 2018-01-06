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
// @flow
import React from 'react';
import LocationIndex from '../../../components/common/LocationIndex';
import LocationMessage from '../../../components/common/LocationMessage';

/*::
type Props = {
  index: number,
  message: string,
  onClick: number => void,
  scroll: HTMLElement => void,
  selected: boolean
};
*/

export default class ConciseIssueLocationsNavigatorLocation extends React.PureComponent {
  /*:: node: HTMLElement; */
  /*:: props: Props; */

  componentDidMount() {
    if (this.props.selected) {
      this.props.scroll(this.node);
    }
  }

  componentDidUpdate(prevProps /*: Props */) {
    if (this.props.selected && prevProps.selected !== this.props.selected) {
      this.props.scroll(this.node);
    }
  }

  handleClick = (event /*: Event */) => {
    event.preventDefault();
    this.props.onClick(this.props.index);
  };

  render() {
    return (
      <div className="little-spacer-top" ref={node => (this.node = node)}>
        <a
          className="consice-issue-locations-navigator-location"
          href="#"
          onClick={this.handleClick}>
          <LocationIndex selected={this.props.selected}>{this.props.index + 1}</LocationIndex>
          <LocationMessage selected={this.props.selected}>{this.props.message}</LocationMessage>
        </a>
      </div>
    );
  }
}
