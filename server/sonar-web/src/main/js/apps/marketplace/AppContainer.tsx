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
import { connect } from 'react-redux';
import App from './App';
import {
  getAppState,
  getGlobalSettingValue,
  getMarketplaceState,
  getMarketplaceEditions,
  getMarketplaceEditionStatus
} from '../../store/rootReducer';
import { Edition, EditionStatus } from '../../api/marketplace';
import { setEditionStatus } from '../../store/marketplace/actions';
import { RawQuery } from '../../helpers/query';

interface OwnProps {
  location: { pathname: string; query: RawQuery };
}

interface StateToProps {
  editions?: Edition[];
  editionsReadOnly: boolean;
  editionStatus?: EditionStatus;
  loadingEditions: boolean;
  standaloneMode: boolean;
  updateCenterActive: boolean;
}

interface DispatchToProps {
  setEditionStatus: (editionStatus: EditionStatus) => void;
}

const mapStateToProps = (state: any) => ({
  editions: getMarketplaceEditions(state),
  editionsReadOnly: getMarketplaceState(state).readOnly,
  editionStatus: getMarketplaceEditionStatus(state),
  loadingEditions: getMarketplaceState(state).loading,
  standaloneMode: getAppState(state).standalone,
  updateCenterActive:
    (getGlobalSettingValue(state, 'sonar.updatecenter.activate') || {}).value === 'true'
});

const mapDispatchToProps = { setEditionStatus };

export default connect<StateToProps, DispatchToProps, OwnProps>(
  mapStateToProps,
  mapDispatchToProps
)(App);
