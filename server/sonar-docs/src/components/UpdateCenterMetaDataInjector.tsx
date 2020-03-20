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
import { createPortal } from 'react-dom';
import MetaData from 'sonar-ui-common/components/ui/update-center/MetaData';
import { Dict } from '../@types/types';

interface Props {
  location: Pick<Location, 'pathname'>;
}

interface State {
  wrappers: Dict<HTMLDivElement>;
}

export default class UpdateCenterMetaDataInjector extends React.Component<Props, State> {
  state: State = {
    wrappers: {}
  };

  componentDidMount() {
    this.searchForMetaData();
  }

  componentDidUpdate({ location }: Props) {
    if (location.pathname !== this.props.location.pathname) {
      this.clearMetaData();
      this.searchForMetaData();
    }
  }

  componentWillUnmount() {
    this.clearMetaData();
  }

  clearMetaData = () => {
    const { wrappers } = this.state;

    Object.keys(wrappers).forEach(key => {
      const node = wrappers[key];
      const { parentNode } = node;
      if (parentNode) {
        parentNode.removeChild(node);
      }

      delete wrappers[key];
    });

    this.setState({ wrappers: {} });
  };

  searchForMetaData = () => {
    const pageContainer = document.querySelector('.page-container');

    if (!pageContainer) {
      return;
    }

    // The following uses an older syntax for createNodeIterator() in order
    // to support IE11
    // - IE doesn't support the new { acceptNode: (node: Node) => number }
    //   format for the 3rd parameter, and instead expects to get it passed
    //   the function directly. Modern browsers support both paradigms as a
    //   fallback, so we fallback to the old one.
    // - IE11 requires the 4th argument.
    // @ts-ignore: tsc requires an additional comment at the function call.
    const iterator = document.createNodeIterator(
      pageContainer,
      NodeFilter.SHOW_COMMENT,
      // @ts-ignore: IE11 doesn't support the { acceptNode: () => number } format.
      (_: Node) => NodeFilter.FILTER_ACCEPT,
      // @ts-ignore: IE11 requires the 4th argument.
      false
    );

    const wrappers: Dict<HTMLDivElement> = {};
    let node = iterator.nextNode();

    while (node) {
      if (node.nodeValue && node.parentNode && /update_center\s*:/.test(node.nodeValue)) {
        let [, key] = node.nodeValue.split(':');
        key = key.trim();

        const wrapper = document.createElement('div');
        wrappers[key] = wrapper;
        node.parentNode.insertBefore(wrapper, node);
      }

      node = iterator.nextNode();
    }

    this.setState({ wrappers });
  };

  render() {
    const { wrappers } = this.state;
    const keys = Object.keys(wrappers);

    if (keys.length === 0) {
      return null;
    }

    return (
      <div>
        {keys.map(key => {
          if (wrappers[key]) {
            return createPortal(<MetaData updateCenterKey={key} />, wrappers[key]);
          } else {
            return null;
          }
        })}
      </div>
    );
  }
}
