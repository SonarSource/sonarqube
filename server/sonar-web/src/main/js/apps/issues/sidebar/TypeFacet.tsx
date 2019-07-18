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
import { orderBy, without } from 'lodash';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import IssueTypeIcon from 'sonar-ui-common/components/icons/IssueTypeIcon';
import NewsBox from 'sonar-ui-common/components/ui/NewsBox';
import { translate } from 'sonar-ui-common/helpers/l10n';
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import { ISSUE_TYPES } from '../../../helpers/constants';
import { getCurrentUser, getCurrentUserSetting, Store } from '../../../store/rootReducer';
import { setCurrentUserSetting } from '../../../store/users';
import { formatFacetStat, Query } from '../utils';

interface Props {
  fetching: boolean;
  newsBoxDismissHotspots?: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  setCurrentUserSetting: (setting: T.CurrentUserSetting) => void;
  stats: T.Dict<number> | undefined;
  types: string[];
}

export class TypeFacet extends React.PureComponent<Props> {
  property = 'types';

  static defaultProps = {
    open: true
  };

  handleItemClick = (itemValue: string, multiple: boolean) => {
    const { types } = this.props;
    if (multiple) {
      const newValue = orderBy(
        types.includes(itemValue) ? without(types, itemValue) : [...types, itemValue]
      );
      this.props.onChange({ [this.property]: newValue });
    } else {
      this.props.onChange({
        [this.property]: types.includes(itemValue) && types.length < 2 ? [] : [itemValue]
      });
    }
  };

  handleHeaderClick = () => {
    this.props.onToggle(this.property);
  };

  handleClear = () => {
    this.props.onChange({ [this.property]: [] });
  };

  handleDismiss = () => {
    this.props.setCurrentUserSetting({ key: 'newsbox.dismiss.hotspots', value: 'true' });
  };

  getStat(type: string) {
    const { stats } = this.props;
    return stats ? stats[type] : undefined;
  }

  isFacetItemActive(type: string) {
    return this.props.types.includes(type);
  }

  stopPropagation = (event: React.MouseEvent<HTMLAnchorElement>) => {
    event.stopPropagation();
  };

  renderItem = (type: string) => {
    const active = this.isFacetItemActive(type);
    const stat = this.getStat(type);

    return (
      <FacetItem
        active={active}
        disabled={stat === 0 && !active}
        key={type}
        name={
          <span className="display-flex-center">
            <IssueTypeIcon className="little-spacer-right" query={type} />{' '}
            {translate('issue.type', type)}
            {type === 'SECURITY_HOTSPOT' && this.props.newsBoxDismissHotspots && (
              <HelpTooltip
                className="little-spacer-left"
                overlay={
                  <>
                    <p>{translate('issues.hotspots.helper')}</p>
                    <hr className="spacer-top spacer-bottom" />
                    <Link
                      onClick={this.stopPropagation}
                      target="_blank"
                      to="/documentation/user-guide/security-hotspots/">
                      {translate('learn_more')}
                    </Link>
                  </>
                }
              />
            )}
          </span>
        }
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        value={type}
      />
    );
  };

  render() {
    const { newsBoxDismissHotspots, types, stats = {} } = this.props;
    const values = types.map(type => translate('issue.type', type));

    const showHotspotNewsBox =
      types.includes('SECURITY_HOTSPOT') || (types.length === 0 && stats['SECURITY_HOTSPOT'] > 0);

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          clearLabel="reset_verb"
          fetching={this.props.fetching}
          name={translate('issues.facet', this.property)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && (
          <>
            <FacetItemsList>{ISSUE_TYPES.map(this.renderItem)}</FacetItemsList>
            {!newsBoxDismissHotspots && showHotspotNewsBox && (
              <NewsBox
                onClose={this.handleDismiss}
                title={translate('issue.type.SECURITY_HOTSPOT.plural')}>
                <p>{translate('issues.hotspots.helper')}</p>
                <p className="text-right spacer-top">
                  <Link target="_blank" to="/documentation/user-guide/security-hotspots/">
                    {translate('learn_more')}
                  </Link>
                </p>
              </NewsBox>
            )}
            <MultipleSelectionHint options={Object.keys(stats).length} values={types.length} />
          </>
        )}
      </FacetBox>
    );
  }
}

const mapStateToProps = (state: Store) => ({
  newsBoxDismissHotspots:
    !getCurrentUser(state).isLoggedIn ||
    getCurrentUserSetting(state, 'newsbox.dismiss.hotspots') === 'true'
});

const mapDispatchToProps = {
  setCurrentUserSetting
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(TypeFacet);
