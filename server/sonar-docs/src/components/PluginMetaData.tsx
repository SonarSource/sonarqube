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
import { createPortal } from 'react-dom';
import { Dict, PluginMetaDataInfo } from '../@types/types';
import PluginVersionMetaData from './PluginVersionMetaData';
import { getPluginMetaData } from './utils';

interface Props {
  location: Pick<Location, 'pathname'>;
}

interface State {
  data: Dict<PluginMetaDataInfo>;
  wrappers: Dict<HTMLDivElement>;
}

export default class PluginMetaData extends React.Component<Props, State> {
  state: State = {
    data: {},
    wrappers: {}
  };

  componentDidMount() {
    this.searchForCommentNodes();
  }

  componentDidUpdate({ location }: Props) {
    if (location.pathname !== this.props.location.pathname) {
      this.clearWrapperNodes();
      this.searchForCommentNodes();
    }
  }

  clearWrapperNodes = () => {
    const { wrappers } = this.state;

    Object.keys(wrappers).forEach(key => {
      const node = wrappers[key];
      const { parentNode } = node;
      if (parentNode) {
        parentNode.removeChild(node);
      }

      delete wrappers[key];
    });

    this.setState({ data: {}, wrappers: {} });
  };

  fetchAndRender = () => {
    const { wrappers } = this.state;

    Object.keys(wrappers).forEach(key => {
      getPluginMetaData(key).then(
        (payload: PluginMetaDataInfo) => {
          this.setState(({ data }) => ({ data: { ...data, [key]: payload } }));
        },
        () => {}
      );
    });
  };

  searchForCommentNodes = () => {
    const pageContainer = document.querySelector('.page-container');

    if (pageContainer) {
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

      let node;
      const wrappers: Dict<HTMLDivElement> = {};
      while ((node = iterator.nextNode())) {
        if (node.nodeValue && /update_center\s*:/.test(node.nodeValue)) {
          let [, key] = node.nodeValue.split(':');
          key = key.trim();

          const wrapper = document.createElement('div');
          wrapper.className = 'plugin-meta-data-wrapper';
          wrappers[key] = wrapper;

          node.parentNode!.insertBefore(wrapper, node);
        }
      }
      this.setState({ wrappers }, this.fetchAndRender);
    }
  };

  renderMetaData = ({
    isSonarSourceCommercial,
    issueTrackerURL,
    license,
    organization,
    versions
  }: PluginMetaDataInfo) => {
    let vendor;
    if (organization) {
      vendor = organization.name;
      if (organization.url) {
        vendor = (
          <a href={organization.url} rel="noopener noreferrer" target="_blank">
            {vendor}
          </a>
        );
      }
    }
    return (
      <div className="plugin-meta-data">
        <div className="plugin-meta-data-header">
          {vendor && <span className="plugin-meta-data-vendor">By {vendor}</span>}
          {license && <span className="plugin-meta-data-license">{license}</span>}
          {issueTrackerURL && (
            <span className="plugin-meta-data-issue-tracker">
              <a href={issueTrackerURL} rel="noopener noreferrer" target="_blank">
                Issue Tracker
              </a>
            </span>
          )}
          {isSonarSourceCommercial && (
            <span className="plugin-meta-data-supported">Supported by SonarSource</span>
          )}
        </div>
        {versions && versions.length > 0 && <PluginVersionMetaData versions={versions} />}
      </div>
    );
  };

  render() {
    const { data, wrappers } = this.state;
    const keys = Object.keys(data);

    if (keys.length === 0) {
      return null;
    }

    return keys.map(key => {
      if (wrappers[key] !== undefined && data[key] !== undefined) {
        return createPortal(this.renderMetaData(data[key]), wrappers[key]);
      } else {
        return null;
      }
    });
  }
}
