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
import * as ReactRedux from 'react-redux';
import * as ReactRouter from 'react-router';
import { FormattedMessage } from 'react-intl';
import throwGlobalError from '../../utils/throwGlobalError';
import addGlobalSuccessMessage from '../../utils/addGlobalSuccessMessage';
import Suggestions from '../embed-docs-modal/Suggestions';
import * as measures from '../../../helpers/measures';
import * as request from '../../../helpers/request';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Favorite from '../../../components/controls/Favorite';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import ListFooter from '../../../components/controls/ListFooter';
import Modal from '../../../components/controls/Modal';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import SearchBox from '../../../components/controls/SearchBox';
import Select from '../../../components/controls/Select';
import Tooltip from '../../../components/controls/Tooltip';
import SelectList from '../../../components/SelectList/SelectList';
import CoverageRating from '../../../components/ui/CoverageRating';
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import Level from '../../../components/ui/Level';
import { EditButton, Button, SubmitButton, ResetButtonLink } from '../../../components/ui/buttons';
import Checkbox from '../../../components/controls/Checkbox';
import DeferredSpinner from '../../../components/common/DeferredSpinner';
import Dropdown from '../../../components/controls/Dropdown';
import ReloadButton from '../../../components/controls/ReloadButton';
import AlertErrorIcon from '../../../components/icons-components/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons-components/AlertSuccessIcon';
import AlertWarnIcon from '../../../components/icons-components/AlertWarnIcon';
import CheckIcon from '../../../components/icons-components/CheckIcon';
import ClearIcon from '../../../components/icons-components/ClearIcon';
import DropdownIcon from '../../../components/icons-components/DropdownIcon';
import HelpIcon from '../../../components/icons-components/HelpIcon';
import LockIcon from '../../../components/icons-components/LockIcon';
import QualifierIcon from '../../../components/icons-components/QualifierIcon';
import Rating from '../../../components/ui/Rating';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import LongLivingBranchIcon from '../../../components/icons-components/LongLivingBranchIcon';
import PullRequestIcon from '../../../components/icons-components/PullRequestIcon';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import SimpleModal from '../../../components/controls/SimpleModal';
import SearchSelect from '../../../components/controls/SearchSelect';
import RadioToggle from '../../../components/controls/RadioToggle';
import A11ySkipTarget from '../a11y/A11ySkipTarget';
import { Alert } from '../../../components/ui/Alert';

const exposeLibraries = () => {
  const global = window as any;

  global.ReactRedux = ReactRedux;
  global.ReactRouter = ReactRouter;
  global.SonarMeasures = measures;
  global.SonarRequest = { ...request, throwGlobalError, addGlobalSuccessMessage };
  global.SonarComponents = {
    A11ySkipTarget,
    ActionsDropdown,
    ActionsDropdownItem,
    Alert,
    AlertErrorIcon,
    AlertSuccessIcon,
    AlertWarnIcon,
    BranchIcon,
    Button,
    Checkbox,
    CheckIcon,
    ClearIcon,
    ConfirmButton,
    CoverageRating,
    DateFormatter,
    DateFromNow,
    DateTimeFormatter,
    DeferredSpinner,
    Dropdown,
    DropdownIcon,
    DuplicationsRating,
    EditButton,
    Favorite,
    FormattedMessage,
    HelpIcon,
    HelpTooltip,
    HomePageSelect,
    Level,
    ListFooter,
    LockIcon,
    LongLivingBranchIcon,
    Modal,
    PullRequestIcon,
    QualifierIcon,
    RadioToggle,
    Rating,
    ReloadButton,
    ResetButtonLink,
    SearchBox,
    SearchSelect,
    Select,
    SelectList,
    SimpleModal,
    SubmitButton,
    Suggestions,
    Tooltip
  };
};

export default exposeLibraries;
