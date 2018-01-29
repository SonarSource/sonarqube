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
import { uniq } from 'lodash';
import Facet, { BasicProps } from './Facet';
import { getRuleTags } from '../../../api/rules';
import FacetFooter from '../../../components/facet/FacetFooter';

interface Props extends BasicProps {
  organization: string | undefined;
}

export default class TagFacet extends React.PureComponent<Props> {
  handleSearch = (query: string) =>
    getRuleTags({ organization: this.props.organization, ps: 50, q: query }).then(tags =>
      tags.map(tag => ({ label: tag, value: tag }))
    );

  handleSelect = (tag: string) => this.props.onChange({ tags: uniq([...this.props.values, tag]) });

  renderName = (tag: string) => (
    <>
      <i className="icon-tags icon-gray little-spacer-right" />
      {tag}
    </>
  );

  renderFooter = () => {
    if (!this.props.stats) {
      return null;
    }

    return <FacetFooter onSearch={this.handleSearch} onSelect={this.handleSelect} />;
  };

  render() {
    const { organization, ...facetProps } = this.props;
    return (
      <Facet
        {...facetProps}
        property="tags"
        renderFooter={this.renderFooter}
        renderName={this.renderName}
      />
    );
  }
}
