/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import MultipleSelectionHint from '../../../components/facet/MultipleSelectionHint';
import IssueTypeIcon from '../../../components/icons/IssueTypeIcon';
import { translate } from '../../../helpers/l10n';
import { ISSUE_CHARACTERISTIC_TO_FIT_FOR, IssueCharacteristic } from '../../../types/issues';
import { Dict } from '../../../types/types';
import { Query, formatFacetStat } from '../utils';

interface Props {
  fetching: boolean;
  onChange: (changes: Partial<Query>) => void;
  onToggle: (property: string) => void;
  open: boolean;
  stats: Dict<number> | undefined;
  fitFor: string;
  characteristics: IssueCharacteristic[];
}

export default class CharacteristicFacet extends React.PureComponent<Props> {
  property = 'characteristics';

  static defaultProps = {
    open: true,
  };

  handleItemClick = (itemValue: IssueCharacteristic, multiple: boolean) => {
    const { characteristics } = this.props;
    if (multiple) {
      const newValue = orderBy(
        characteristics.includes(itemValue)
          ? without(characteristics, itemValue)
          : [...characteristics, itemValue]
      );
      this.props.onChange({ [this.property]: newValue });
      return;
    }

    // Append if there is no characteristic selected yet in this fitFor
    const selectedFitFor = characteristics.filter(
      (characteristic) => ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic] === this.props.fitFor
    );
    if (selectedFitFor.length === 0) {
      this.props.onChange({ [this.property]: [...characteristics, itemValue] });
      return;
    }

    // If clicking on the only selected characteristic, clear it
    if (selectedFitFor.length === 1 && selectedFitFor[0] === itemValue) {
      this.props.onChange({
        [this.property]: characteristics.filter(
          (characteristic) => ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic] !== this.props.fitFor
        ),
      });
      return;
    }

    // If there is already a selection for this fitFor, replace it
    this.props.onChange({
      [this.property]: characteristics
        .filter(
          (characteristic) => ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic] !== this.props.fitFor
        )
        .concat([itemValue]),
    });
  };

  handleHeaderClick = () => {
    this.props.onToggle(`${this.property}.${this.props.fitFor}`);
  };

  handleClear = () => {
    // Clear characteristics for this fitFor
    this.props.onChange({
      [this.property]: this.props.characteristics.filter(
        (characteristic) => ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic] !== this.props.fitFor
      ),
    });
  };

  getStat(characteristic: string) {
    const { stats } = this.props;
    return stats ? stats[characteristic] : undefined;
  }

  isFacetItemActive(characteristic: IssueCharacteristic) {
    return this.props.characteristics.includes(characteristic);
  }

  renderItem = (characteristic: IssueCharacteristic) => {
    const active = this.isFacetItemActive(characteristic);
    const stat = this.getStat(characteristic);

    return (
      <FacetItem
        active={active}
        key={characteristic}
        name={
          <span className="display-flex-center">
            <IssueTypeIcon className="little-spacer-right" query={characteristic} />{' '}
            {translate('issue.characteristic', characteristic)}
          </span>
        }
        onClick={this.handleItemClick}
        stat={formatFacetStat(stat)}
        value={characteristic}
      />
    );
  };

  render() {
    const { characteristics, fitFor } = this.props;
    const values = characteristics
      .filter((characteristic) => ISSUE_CHARACTERISTIC_TO_FIT_FOR[characteristic] === fitFor)
      .map((characteristic) => translate('issue.characteristic', characteristic));

    const availableCharacteristics = Object.entries(ISSUE_CHARACTERISTIC_TO_FIT_FOR)
      .filter(([, value]) => value === fitFor)
      .map(([key]) => key as IssueCharacteristic);

    return (
      <FacetBox property={this.property}>
        <FacetHeader
          fetching={this.props.fetching}
          name={translate('issues.facet.characteristics', fitFor)}
          onClear={this.handleClear}
          onClick={this.handleHeaderClick}
          open={this.props.open}
          values={values}
        />

        {this.props.open && (
          <>
            <FacetItemsList>{availableCharacteristics.map(this.renderItem)}</FacetItemsList>
            <MultipleSelectionHint
              options={Object.keys(availableCharacteristics).length}
              values={values.length}
            />
          </>
        )}
      </FacetBox>
    );
  }
}
