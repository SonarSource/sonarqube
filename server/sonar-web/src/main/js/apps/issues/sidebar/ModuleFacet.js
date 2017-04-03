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
// @flow
import React from 'react';
import { sortBy, without } from 'lodash';
import FacetBox from './components/FacetBox';
import FacetHeader from './components/FacetHeader';
import FacetItem from './components/FacetItem';
import FacetItemsList from './components/FacetItemsList';
import type { ReferencedComponent } from '../utils';
import QualifierIcon from '../../../components/shared/QualifierIcon';
import { translate } from '../../../helpers/l10n';

type Props = {|
  facetMode: string,
  onChange: (changes: { [string]: Array<string> }) => void,
  onToggle: (property: string) => void,
  open: boolean,
  stats?: { [string]: number },
  referencedComponents: { [string]: ReferencedComponent },
  modules: Array<string>
|};

export default class ModuleFacet extends React.PureComponent {
  props: Props;

  static defaultProps = {
    open: true
  };

  property = 'modules';

  handleItemClick = (itemValue: string) => {
    const { modules } = this.props;
    const newValue = sortBy(
      modules.includes(itemValue) ? without(modules, itemValue) : [...modules, itemValue]
    );
    this.props.onChange({ [this.property]: newValue });
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  getStat(module: string): ?number {
    const { stats } = this.props;
    return stats ? stats[module] : null;
  }

  renderName(module: string): React.Element<*> | string {
    const { referencedComponents } = this.props;
    const name = referencedComponents[module] ? referencedComponents[module].name : module;
    return (
      <span>
        <QualifierIcon className="little-spacer-right" qualifier="BRC" />
        {name}
      </span>
    );
  }

  render() {
    const { stats } = this.props;

    if (!stats) {
      return null;
    }

    const modules = sortBy(Object.keys(stats), key => -stats[key]);

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          hasValue={this.props.modules.length > 0}
          name={translate('issues.facet', this.property)}
          onClick={this.handleHeaderClick}
          open={this.props.open}
        />

        {this.props.open &&
          <FacetItemsList>
            {modules.map(module => (
              <FacetItem
                active={this.props.modules.includes(module)}
                facetMode={this.props.facetMode}
                key={module}
                name={this.renderName(module)}
                onClick={this.handleItemClick}
                stat={this.getStat(module)}
                value={module}
              />
            ))}
          </FacetItemsList>}
      </FacetBox>
    );
  }
}
