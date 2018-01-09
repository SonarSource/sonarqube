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
import Modal from '../../components/controls/Modal';
import SearchBox from '../../components/controls/SearchBox';
import Select from '../../components/controls/Select';
import Tooltip from '../../components/controls/Tooltip';
import ModalForm from '../../components/common/modal-form';
import SelectList from '../../components/SelectList';
import CoverageRating from '../../components/ui/CoverageRating';
import DuplicationsRating from '../../components/ui/DuplicationsRating';
import Level from '../../components/ui/Level';
import { EditButton } from '../../components/ui/buttons';

const exposeLibraries = () => {
  const global = window as any;

  global.ReactRedux = ReactRedux;
  global.ReactRouter = ReactRouter;
  global.SonarIcons = icons;
  global.SonarMeasures = measures;
  global.SonarRequest = { ...request, throwGlobalError };
  global.SonarComponents = {
    CoverageRating,
    DateFromNow,
    DateFormatter,
    DateTimeFormatter,
    DuplicationsRating,
    EditButton,
    FavoriteContainer,
    Level,
    LicenseEditionSet,
    ListFooter,
    Modal,
    Tooltip,
    Select,
    SelectList,
    SearchBox,
    // deprecated, used in Governance
    ModalForm_deprecated: ModalForm
  };
};

export default exposeLibraries;
