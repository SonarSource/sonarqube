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
import { sortBy, uniq, without } from 'lodash';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetFooter from '../../../components/facet/FacetFooter';
import Organization from '../../../components/shared/Organization';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import { searchProjects, getTree } from '../../../api/components';
import { translate } from '../../../helpers/l10n';
import { formatFacetStat } from '../utils';
/*:: import type { ReferencedComponent, Component } from '../utils'; */

/*::
type Props = {|
  component?: Component,
  facetMode: string,
  onChange: (changes: { [string]: Array<string> }) => void,
  onToggle: (property: string) => void,
  open: boolean,
  organization?: { key: string },
  stats?: { [string]: number },
  referencedComponents: { [string]: ReferencedComponent },
  projects: Array<string>
|};
*/

export default class ProjectFacet extends React.PureComponent {
  /*:: props: Props; */

  property = 'projects';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue /*: string */) => {
    const { projects } = this.props;
    const newValue = sortBy(
      projects.includes(itemValue) ? without(projects, itemValue) : [...projects, itemValue]
    );
    this.props.onChange({ [this.property]: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  handleSearch = (query /*: string */) => {
    const { component, organization } = this.props;
    if (component != null && ['VW', 'SVW', 'APP'].includes(component.qualifier)) {
      return getTree(component.key, { ps: 50, q: query, qualifiers: 'TRK' }).then(response =>
        response.components.map(component => ({
          label: component.name,
          organization: component.organization,
          value: component.refId
        }))
      );
    }

    return searchProjects({
      ps: 50,
      filter: query ? `query = "${query}"` : '',
      organization: organization && organization.key
    }).then(response =>
      response.components.map(component => ({
        label: component.name,
        organization: component.organization,
        value: component.id
      }))
    );
  };

  handleSelect = (rule /*: string */) => {
    const { projects } = this.props;
    this.props.onChange({ [this.property]: uniq([...projects, rule]) });
  };

  getStat(project /*: string */) /*: ?number */ {
    const { stats } = this.props;
    return stats ? stats[project] : null;
  }

  getProjectName(project /*: string */) {
    const { referencedComponents } = this.props;
    return referencedComponents[project] ? referencedComponents[project].name : project;
  }

  renderName(project /*: string */) /*: React.Element<*> | string */ {
    const { organization, referencedComponents } = this.props;
    return referencedComponents[project] ? (
      <span>
        <QualifierIcon className="little-spacer-right" qualifier="TRK" />
        {!organization && (
          <Organization link={false} organizationKey={referencedComponents[project].organization} />
        )}
        {referencedComponents[project].name}
      </span>
    ) : (
      <span>
        <QualifierIcon className="little-spacer-right" qualifier="TRK" />
        {project}
      </span>
    );
  }

  renderOption = (option /*: { label: string, organization: string } */) => {
    return (
      <span>
        <Organization link={false} organizationKey={option.organization} />
        {option.label}
      </span>
    );
  };

  renderList() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const projects = sortBy(Object.keys(stats), key => -stats[key]);

    return (
      <FacetItemsList>
        {projects.map(project => (
          <FacetItem
            active={this.props.projects.includes(project)}
            key={project}
            name={this.renderName(project)}
            onClick={this.handleItemClick}
            stat={formatFacetStat(this.getStat(project), this.props.facetMode)}
            value={project}
          />
        ))}
      </FacetItemsList>
    );
  }

  renderFooter() {
    if (!this.props.stats) {
      return null;
    }

    return (
      <FacetFooter
        minimumQueryLength={3}
        onSearch={this.handleSearch}
        onSelect={this.handleSelect}
        renderOption={this.renderOption}
      />
    );
  }

  render() {
    const values = this.props.projects.map(project => this.getProjectName(project));
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && this.renderList()}
        {this.props.open && this.renderFooter()}
      </FacetBox>
    );
  }
}
