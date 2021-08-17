/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import NotFound from '../../../app/components/NotFound';
import Favorite from '../../../components/controls/Favorite';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import BranchLikeIcon from '../../../components/icons/BranchLikeIcon';
import CoverageRating from '../../../components/ui/CoverageRating';
import {
  getBranchLikeQuery,
  isBranch,
  isMainBranch,
  isPullRequest
} from '../../../helpers/branch-like';
import * as measures from '../../../helpers/measures';
import {
  getStandards,
  renderCWECategory,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory
} from '../../../helpers/security-standard';
import {
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
  getRulesUrl
} from '../../../helpers/urls';
import ActionsDropdown, {
  ActionsDropdownItem
} from '../../../sonar-ui-common/components/controls/ActionsDropdown';
import {
  Button,
  EditButton,
  ResetButtonLink,
  SubmitButton
} from '../../../sonar-ui-common/components/controls/buttons';
import Checkbox from '../../../sonar-ui-common/components/controls/Checkbox';
import ConfirmButton from '../../../sonar-ui-common/components/controls/ConfirmButton';
import Dropdown from '../../../sonar-ui-common/components/controls/Dropdown';
import HelpTooltip from '../../../sonar-ui-common/components/controls/HelpTooltip';
import ListFooter from '../../../sonar-ui-common/components/controls/ListFooter';
import Modal from '../../../sonar-ui-common/components/controls/Modal';
import RadioToggle from '../../../sonar-ui-common/components/controls/RadioToggle';
import ReloadButton from '../../../sonar-ui-common/components/controls/ReloadButton';
import SearchBox from '../../../sonar-ui-common/components/controls/SearchBox';
import SearchSelect from '../../../sonar-ui-common/components/controls/SearchSelect';
import Select from '../../../sonar-ui-common/components/controls/Select';
import SelectList from '../../../sonar-ui-common/components/controls/SelectList';
import SimpleModal from '../../../sonar-ui-common/components/controls/SimpleModal';
import Tooltip from '../../../sonar-ui-common/components/controls/Tooltip';
import AlertErrorIcon from '../../../sonar-ui-common/components/icons/AlertErrorIcon';
import AlertSuccessIcon from '../../../sonar-ui-common/components/icons/AlertSuccessIcon';
import AlertWarnIcon from '../../../sonar-ui-common/components/icons/AlertWarnIcon';
import BranchIcon from '../../../sonar-ui-common/components/icons/BranchIcon';
import CheckIcon from '../../../sonar-ui-common/components/icons/CheckIcon';
import ClearIcon from '../../../sonar-ui-common/components/icons/ClearIcon';
import DetachIcon from '../../../sonar-ui-common/components/icons/DetachIcon';
import DropdownIcon from '../../../sonar-ui-common/components/icons/DropdownIcon';
import HelpIcon from '../../../sonar-ui-common/components/icons/HelpIcon';
import LockIcon from '../../../sonar-ui-common/components/icons/LockIcon';
import PlusCircleIcon from '../../../sonar-ui-common/components/icons/PlusCircleIcon';
import PullRequestIcon from '../../../sonar-ui-common/components/icons/PullRequestIcon';
import QualifierIcon from '../../../sonar-ui-common/components/icons/QualifierIcon';
import SecurityHotspotIcon from '../../../sonar-ui-common/components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from '../../../sonar-ui-common/components/icons/VulnerabilityIcon';
import DateFormatter from '../../../sonar-ui-common/components/intl/DateFormatter';
import DateFromNow from '../../../sonar-ui-common/components/intl/DateFromNow';
import DateTimeFormatter from '../../../sonar-ui-common/components/intl/DateTimeFormatter';
import { Alert } from '../../../sonar-ui-common/components/ui/Alert';
import DeferredSpinner from '../../../sonar-ui-common/components/ui/DeferredSpinner';
import DuplicationsRating from '../../../sonar-ui-common/components/ui/DuplicationsRating';
import Level from '../../../sonar-ui-common/components/ui/Level';
import Rating from '../../../sonar-ui-common/components/ui/Rating';
import { translate, translateWithParameters } from '../../../sonar-ui-common/helpers/l10n';
import { formatMeasure } from '../../../sonar-ui-common/helpers/measures';
import {
  get,
  getJSON,
  getText,
  post,
  postJSON,
  postJSONBody,
  request
} from '../../../sonar-ui-common/helpers/request';
import addGlobalSuccessMessage from '../../utils/addGlobalSuccessMessage';
import throwGlobalError from '../../utils/throwGlobalError';
import A11ySkipTarget from '../a11y/A11ySkipTarget';
import Suggestions from '../embed-docs-modal/Suggestions';

const exposeLibraries = () => {
  const global = window as any;

  global.SonarRequest = {
    request,
    get,
    getJSON,
    getText,
    post,
    postJSON,
    postJSONBody,
    throwGlobalError,
    addGlobalSuccessMessage
  };
  global.t = translate;
  global.tp = translateWithParameters;

  /**
   * @deprecated since SonarQube 8.7
   */
  Object.defineProperty(global, 'SonarHelpers', {
    get: () => {
      // eslint-disable-next-line no-console
      console.warn('SonarHelpers usages are deprecated since SonarQube 8.7');
      return {
        getBranchLikeQuery,
        isBranch,
        isMainBranch,
        isPullRequest,
        getStandards,
        renderCWECategory,
        renderOwaspTop10Category,
        renderSansTop25Category,
        renderSonarSourceSecurityCategory,
        getComponentIssuesUrl,
        getComponentSecurityHotspotsUrl,
        getRulesUrl
      };
    }
  });

  /**
   * @deprecated since SonarQube 8.7
   */
  Object.defineProperty(global, 'SonarMeasures', {
    get: () => {
      // eslint-disable-next-line no-console
      console.warn('SonarMeasures usages are deprecated since SonarQube 8.7');
      return { ...measures, formatMeasure };
    }
  });

  /**
   * @deprecated since SonarQube 8.7
   */
  Object.defineProperty(global, 'SonarComponents', {
    get: () => {
      // eslint-disable-next-line no-console
      console.warn('SonarComponents usages are deprecated since SonarQube 8.7');
      return {
        A11ySkipTarget,
        ActionsDropdown,
        ActionsDropdownItem,
        Alert,
        AlertErrorIcon,
        AlertSuccessIcon,
        AlertWarnIcon,
        BranchIcon: BranchLikeIcon,
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
        LongLivingBranchIcon: BranchIcon,
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
    }
  });
};

export default exposeLibraries;
