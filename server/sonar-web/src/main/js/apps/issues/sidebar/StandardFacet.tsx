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
import { sortBy, without } from 'lodash';
import { Query, STANDARDS, formatFacetStat } from '../utils';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import { translate } from '../../../helpers/l10n';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import FacetItem from '../../../components/facet/FacetItem';
import Select from '../../../components/controls/Select';

export interface Props {
  cwe: string[];
  cweOpen: boolean;
  cweStats: { [x: string]: number } | undefined;
  loading?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  owaspTop10: string[];
  owaspTop10Open: boolean;
  owaspTop10Stats: { [x: string]: number } | undefined;
  sansTop25: string[];
  sansTop25Open: boolean;
  sansTop25Stats: { [x: string]: number } | undefined;
}

interface Standards {
  owaspTop10: { [x: string]: { title: string } };
  sansTop25: { [x: string]: { title: string } };
  cwe: { [x: string]: { title: string } };
}

interface State {
  standards: Standards;
}

export default class StandardFacet extends React.PureComponent<Props, State> {
  mounted = false;
  property = STANDARDS;
  state: State = { standards: { owaspTop10: {}, sansTop25: {}, cwe: {} } };

  componentDidMount() {
    this.mounted = true;
    if (this.props.open) {
      this.loadStandards();
    }
  }

  componentDidUpdate(prevProps: Props) {
    if (!prevProps.open && this.props.open) {
      this.loadStandards();
    }
  }

  componentWillUnmount() {
    this.mounted = false;
  }

  loadStandards = () => {
    import('../../../helpers/standards.json')
      .then(x => x.default)
      .then(
        ({ owaspTop10, sansTop25, cwe }: Standards) => {
          if (this.mounted) {
            this.setState({ standards: { owaspTop10, sansTop25, cwe } });
          }
        },
        () => {}
      );
  };

  getValues = () => {
    return [
      ...this.props.owaspTop10.map(this.renderOwaspTop10Category),
      ...this.props.sansTop25.map(this.renderSansTop25Category),
      ...this.props.cwe.map(this.renderCWECategory)
    ];
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleOwaspTop10HeaderClick = () => {
    this.props.onToggle('owaspTop10');
  };

  handleSansTop25HeaderClick = () => {
    this.props.onToggle('sansTop25');
  };

  handleCWEHeaderClick = () => {
    this.props.onToggle('cwe');
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [], owaspTop10: [], sansTop25: [], cwe: [] });
  };

  handleItemClick = (
    prop: 'owaspTop10' | 'sansTop25' | 'cwe',
    itemValue: string,
    multiple: boolean
  ) => {
    const items = this.props[prop];
    if (multiple) {
      const newValue = sortBy(
        items.includes(itemValue) ? without(items, itemValue) : [...items, itemValue]
      );
      this.props.onChange({ [prop]: newValue });
    } else {
      this.props.onChange({
        [prop]: items.includes(itemValue) && items.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleOwaspTop10ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('owaspTop10', itemValue, multiple);
  };

  handleCWEItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('cwe', itemValue, multiple);
  };

  handleSansTop25ItemClick = (itemValue: string, multiple: boolean) => {
    this.handleItemClick('sansTop25', itemValue, multiple);
  };

  handleCWESelect = ({ value }: { value: string }) => {
    this.handleItemClick('cwe', value, true);
  };

  renderOwaspTop10Category = (category: string) => {
    const record = this.state.standards.owaspTop10[category];
    if (!record) {
      return category.toUpperCase();
    } else if (category === 'unknown') {
      return record.title;
    } else {
      return `${category.toUpperCase()} - ${record.title}`;
    }
  };

  renderCWECategory = (category: string) => {
    const record = this.state.standards.cwe[category];
    if (!record) {
      return `CWE-${category}`;
    } else if (category === 'unknown') {
      return record.title;
    } else {
      return `CWE-${category} - ${record.title}`;
    }
  };

  renderSansTop25Category = (category: string) => {
    const record = this.state.standards.sansTop25[category];
    return record ? record.title : category;
  };

  renderList = (
    statsProp: 'owaspTop10Stats' | 'cweStats' | 'sansTop25Stats',
    valuesProp: 'owaspTop10' | 'cwe' | 'sansTop25',
    renderName: (category: string) => string,
    onClick: (x: string, multiple?: boolean) => void
  ) => {
    const stats = this.props[statsProp];
    const values = this.props[valuesProp];

    if (!stats) {
      return null;
    }

    const categories = sortBy(Object.keys(stats), key => -stats[key]);
    const getStat = (category: string) => {
      return stats ? stats[category] : undefined;
    };

    return (
      <FacetItemsList>
        {categories.map(category => (
          <FacetItem
            active={values.includes(category)}
            key={category}
            loading={this.props.loading}
            name={renderName(category)}
            onClick={onClick}
            stat={formatFacetStat(getStat(category))}
            tooltip={values.length === 1 && !values.includes(category)}
            value={category}
          />
        ))}
      </FacetItemsList>
    );
  };

  renderOwaspTop10List() {
    return this.renderList(
      'owaspTop10Stats',
      'owaspTop10',
      this.renderOwaspTop10Category,
      this.handleOwaspTop10ItemClick
    );
  }

  renderCWEList() {
    return this.renderList('cweStats', 'cwe', this.renderCWECategory, this.handleCWEItemClick);
  }

  renderCWESearch() {
    const options = Object.keys(this.state.standards.cwe).map(cwe => ({
      label: this.renderCWECategory(cwe),
      value: cwe
    }));
    return (
      <div className="search-navigator-facet-footer">
        <Select
          className="input-super-large"
          clearable={false}
          noResultsText={translate('select2.noMatches')}
          onChange={this.handleCWESelect}
          options={options}
          placeholder={translate('search.search_for_cwe')}
          searchable={true}
        />
      </div>
    );
  }

  renderSansTop25List() {
    return this.renderList(
      'sansTop25Stats',
      'sansTop25',
      this.renderSansTop25Category,
      this.handleSansTop25ItemClick
    );
  }

  renderSubFacets() {
    return (
      <>
        <FacetBox className="is-inner" property="owaspTop10">
          <FacetHeader
            name={translate('issues.facet.owaspTop10')}
            onClick={this.handleOwaspTop10HeaderClick}
            open={this.props.owaspTop10Open}
            values={this.props.owaspTop10.map(this.renderOwaspTop10Category)}
          />
          {this.props.owaspTop10Open && this.renderOwaspTop10List()}
        </FacetBox>
        <FacetBox className="is-inner" property="sansTop25">
          <FacetHeader
            name={translate('issues.facet.sansTop25')}
            onClick={this.handleSansTop25HeaderClick}
            open={this.props.sansTop25Open}
            values={this.props.sansTop25.map(this.renderSansTop25Category)}
          />
          {this.props.sansTop25Open && this.renderSansTop25List()}
        </FacetBox>
        <FacetBox className="is-inner" property="cwe">
          <FacetHeader
            name={translate('issues.facet.cwe')}
            onClick={this.handleCWEHeaderClick}
            open={this.props.cweOpen}
            values={this.props.cwe.map(this.renderCWECategory)}
          />
          {this.props.cweOpen && this.renderCWEList()}
          {this.props.cweOpen && this.renderCWESearch()}
        </FacetBox>
      </>
    );
  }

  render() {
    return (
      <FacetBox property={this.property}>
        <FacetHeader
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={this.getValues()}
        />

        {this.props.open && this.renderSubFacets()}
      </FacetBox>
    );
  }
}
