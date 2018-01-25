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
import FacetBox from '../../../components/facet/FacetBox';
import FacetHeader from '../../../components/facet/FacetHeader';
import FacetItem from '../../../components/facet/FacetItem';
import FacetItemsList from '../../../components/facet/FacetItemsList';
import { translate } from '../../../helpers/l10n';

/*::
type Props = {|
  facetMode: string,
  onChange: (changes: {}) => void
|};
*/

export default class FacetMode extends React.PureComponent {
  /*:: props: Props; */

  property = 'facetMode';

  handleItemClick = (itemValue /*: string */) => {
    this.props.onChange({ [this.property]: itemValue });
  };

  render() {
    const { facetMode } = this.props;
    const modes = ['count', 'effort'];

    return (
      <FacetBox property={this.property}>
        <FacetHeader name={translate('issues.facet.mode')} />

        <FacetItemsList>
          {modes.map(mode => (
            <FacetItem
              active={facetMode === mode}
              halfWidth={true}
              key={mode}
              name={translate('issues.facet.mode', mode)}
              onClick={this.handleItemClick}
              value={mode}
            />
          ))}
        </FacetItemsList>
      </FacetBox>
    );
  }
}
