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
import {
  deleteQualityGate,
  showQualityGate,
  renameQualityGate,
  copyQualityGate,
  setQualityGateAsDefault,
  addCondition,
  deleteCondition,
  saveCondition
} from '../store/actions';
import Details from '../components/Details';
import { getMetrics, getQualityGatesAppState } from '../../../store/rootReducer';
import { fetchMetrics } from '../../../store/rootActions';

const mapStateToProps = state => ({
  ...getQualityGatesAppState(state),
  metrics: getMetrics(state)
});

const mapDispatchToProps = dispatch => ({
  onShow: qualityGate => dispatch(showQualityGate(qualityGate)),
  onDelete: qualityGate => dispatch(deleteQualityGate(qualityGate)),
  onRename: (qualityGate, newName) => dispatch(renameQualityGate(qualityGate, newName)),
  onCopy: qualityGate => dispatch(copyQualityGate(qualityGate)),
  onSetAsDefault: qualityGate => dispatch(setQualityGateAsDefault(qualityGate)),
  onAddCondition: metric => dispatch(addCondition(metric)),
  onSaveCondition: (oldCondition, newCondition) =>
    dispatch(saveCondition(oldCondition, newCondition)),
  onDeleteCondition: condition => dispatch(deleteCondition(condition)),
  fetchMetrics: () => dispatch(fetchMetrics())
});

export default connect(mapStateToProps, mapDispatchToProps)(Details);
