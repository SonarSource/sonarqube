/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import moment from 'moment';
import * as ReactRedux from 'react-redux';
import * as ReactRouter from 'react-router';
import Select from 'react-select';
import Modal from 'react-modal';
import * as measures from '../../helpers/measures';
import * as request from '../../helpers/request';
import FavoriteContainer from '../../components/controls/FavoriteContainer';
import ListFooter from '../../components/controls/ListFooter';
import Tooltip from '../../components/controls/Tooltip';
import ModalForm from '../../components/common/modal-form';

const exposeLibraries = () => {
  window.moment = moment;
  window.ReactRedux = ReactRedux;
  window.ReactRouter = ReactRouter;
  window.SonarMeasures = measures;
  window.SonarRequest = request;
  window.SonarComponents = {
    FavoriteContainer,
    ListFooter,
    Modal,
    Tooltip,
    Select,
    // deprecated, used in Governance
    ModalForm_deprecated: ModalForm
  };
};

export default exposeLibraries;
