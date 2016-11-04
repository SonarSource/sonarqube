/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import React from 'react';
import { Route } from 'react-router';
import Deletion from './deletion/Deletion';
import QualityProfiles from './quality-profiles/QualityProfiles';
import QualityGate from './quality-gate/QualityGate';
import Links from './links/Links';
import Key from './key/Key';

export default [
  <Route key="deletion" path="deletion" component={Deletion}/>,
  <Route key="quality_profiles" path="quality_profiles" component={QualityProfiles}/>,
  <Route key="quality_gate" path="quality_gate" component={QualityGate}/>,
  <Route key="links" path="links" component={Links}/>,
  <Route key="key" path="key" component={Key}/>
];
