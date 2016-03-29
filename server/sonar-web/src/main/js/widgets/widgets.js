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
import './old/base';

import './old/bubble-chart';
import './old/histogram';
import './old/pie-chart';
import './old/stack-area';
import './old/tag-cloud';
import './old/timeline';
import './old/treemap';
import './old/word-cloud';

import './old/widget';

import IssueFilterWidget from './issue-filter/widget';
import ComplexityDistribution from './complexity';
import TimeMachine from './timeMachine';

window.IssueFilterWidget = IssueFilterWidget;
window.ComplexityDistribution = ComplexityDistribution;
window.TimeMachineWidget = TimeMachine;
