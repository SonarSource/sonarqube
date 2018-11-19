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
import * as ReactRedux from 'react-redux';
import * as ReactRouter from 'react-router';
import Select from 'react-select';
import Modal from 'react-modal';
import throwGlobalError from './throwGlobalError';
import * as measures from '../../helpers/measures';
import * as request from '../../helpers/request';
import * as icons from '../../components/icons-components/icons';
import DateFromNow from '../../components/intl/DateFromNow';
import DateFormatter from '../../components/intl/DateFormatter';
import DateTimeFormatter from '../../components/intl/DateTimeFormatter';
import FavoriteContainer from '../../components/controls/FavoriteContainer';
import LicenseEditionSet from '../../apps/marketplace/components/LicenseEditionSet';
import ListFooter from '../../components/controls/ListFooter';
import Tooltip from '../../components/controls/Tooltip';
import ModalForm from '../../components/common/modal-form';
import SelectList from '../../components/SelectList';
import CoverageRating from '../../components/ui/CoverageRating';
import DuplicationsRating from '../../components/ui/DuplicationsRating';
import Level from '../../components/ui/Level';

const exposeLibraries = () => {
  window.ReactRedux = ReactRedux;
  window.ReactRouter = ReactRouter;
  window.SonarIcons = icons;
  window.SonarMeasures = measures;
  window.SonarRequest = { ...request, throwGlobalError };
  window.SonarComponents = {
    DateFromNow,
    DateFormatter,
    DateTimeFormatter,
    FavoriteContainer,
    LicenseEditionSet,
    ListFooter,
    Modal,
    Tooltip,
    Select,
    CoverageRating,
    DuplicationsRating,
    Level,
    // deprecated, used in Governance
    ModalForm_deprecated: ModalForm,
    SelectList
  };
};

export default exposeLibraries;
