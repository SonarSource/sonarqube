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
import { FormattedMessage } from 'react-intl';
import * as ReactRedux from 'react-redux';
import * as ReactRouter from 'react-router';
import ActionsDropdown, {
  ActionsDropdownItem
} from 'sonar-ui-common/components/controls/ActionsDropdown';
import {
  Button,
  EditButton,
  ResetButtonLink,
  SubmitButton
} from 'sonar-ui-common/components/controls/buttons';
import Checkbox from 'sonar-ui-common/components/controls/Checkbox';
import ConfirmButton from 'sonar-ui-common/components/controls/ConfirmButton';
import Dropdown from 'sonar-ui-common/components/controls/Dropdown';
import HelpTooltip from 'sonar-ui-common/components/controls/HelpTooltip';
import ListFooter from 'sonar-ui-common/components/controls/ListFooter';
import Modal from 'sonar-ui-common/components/controls/Modal';
import RadioToggle from 'sonar-ui-common/components/controls/RadioToggle';
import ReloadButton from 'sonar-ui-common/components/controls/ReloadButton';
import SearchBox from 'sonar-ui-common/components/controls/SearchBox';
import SearchSelect from 'sonar-ui-common/components/controls/SearchSelect';
import Select from 'sonar-ui-common/components/controls/Select';
import SelectList from 'sonar-ui-common/components/controls/SelectList';
import SimpleModal from 'sonar-ui-common/components/controls/SimpleModal';
import Tooltip from 'sonar-ui-common/components/controls/Tooltip';
import AlertErrorIcon from 'sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from 'sonar-ui-common/components/icons/AlertSuccessIcon';
import AlertWarnIcon from 'sonar-ui-common/components/icons/AlertWarnIcon';
import CheckIcon from 'sonar-ui-common/components/icons/CheckIcon';
import ClearIcon from 'sonar-ui-common/components/icons/ClearIcon';
import DetachIcon from 'sonar-ui-common/components/icons/DetachIcon';
import DropdownIcon from 'sonar-ui-common/components/icons/DropdownIcon';
import HelpIcon from 'sonar-ui-common/components/icons/HelpIcon';
import LockIcon from 'sonar-ui-common/components/icons/LockIcon';
import LongLivingBranchIcon from 'sonar-ui-common/components/icons/LongLivingBranchIcon';
import PlusCircleIcon from 'sonar-ui-common/components/icons/PlusCircleIcon';
import PullRequestIcon from 'sonar-ui-common/components/icons/PullRequestIcon';
import QualifierIcon from 'sonar-ui-common/components/icons/QualifierIcon';
import SecurityHotspotIcon from 'sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from 'sonar-ui-common/components/icons/VulnerabilityIcon';
import { Alert } from 'sonar-ui-common/components/ui/Alert';
import DeferredSpinner from 'sonar-ui-common/components/ui/DeferredSpinner';
import DuplicationsRating from 'sonar-ui-common/components/ui/DuplicationsRating';
import Level from 'sonar-ui-common/components/ui/Level';
import Rating from 'sonar-ui-common/components/ui/Rating';
import { formatMeasure } from 'sonar-ui-common/helpers/measures';
import NotFound from '../../../app/components/NotFound';
import Favorite from '../../../components/controls/Favorite';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import BranchIcon from '../../../components/icons-components/BranchIcon';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import CoverageRating from '../../../components/ui/CoverageRating';
import {
  getBranchLikeQuery,
  isBranch,
  isLongLivingBranch,
  isPullRequest
} from '../../../helpers/branches';
import * as measures from '../../../helpers/measures';
import {
  getStandards,
  renderCWECategory,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory
} from '../../../helpers/security-standard';
import { getComponentIssuesUrl, getRulesUrl } from '../../../helpers/urls';
import addGlobalSuccessMessage from '../../utils/addGlobalSuccessMessage';
import throwGlobalError from '../../utils/throwGlobalError';
import A11ySkipTarget from '../a11y/A11ySkipTarget';
import Suggestions from '../embed-docs-modal/Suggestions';
import request from './legacy/request-legacy';

const exposeLibraries = () => {
  const global = window as any;

  global.ReactRedux = ReactRedux;
  global.ReactRouter = ReactRouter;
  global.SonarHelpers = {
    getBranchLikeQuery,
    isBranch,
    isLongLivingBranch,
    isPullRequest,
    getStandards,
    renderCWECategory,
    renderOwaspTop10Category,
    renderSansTop25Category,
    renderSonarSourceSecurityCategory,
    getComponentIssuesUrl,
    getRulesUrl
  };
  global.SonarMeasures = { ...measures, formatMeasure };
  global.SonarRequest = {
    ...request,
    throwGlobalError,
    addGlobalSuccessMessage
  };
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
    DetachIcon,
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
    NotFound,
    PlusCircleIcon,
    PullRequestIcon,
    QualifierIcon,
    RadioToggle,
    Rating,
    ReloadButton,
    ResetButtonLink,
    SearchBox,
    SearchSelect,
    SecurityHotspotIcon,
    Select,
    SelectList,
    SimpleModal,
    SubmitButton,
    Suggestions,
    Tooltip,
    VulnerabilityIcon
  };
};

export default exposeLibraries;
