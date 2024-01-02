/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import A11ySkipTarget from '../../../components/a11y/A11ySkipTarget';
import DonutChart from '../../../components/charts/DonutChart';
import ActionsDropdown, { ActionsDropdownItem } from '../../../components/controls/ActionsDropdown';
import BoxedTabs from '../../../components/controls/BoxedTabs';
import {
  Button,
  EditButton,
  ResetButtonLink,
  SubmitButton,
} from '../../../components/controls/buttons';
import ButtonToggle from '../../../components/controls/ButtonToggle';
import Checkbox from '../../../components/controls/Checkbox';
import ClickEventBoundary from '../../../components/controls/ClickEventBoundary';
import ConfirmButton from '../../../components/controls/ConfirmButton';
import Dropdown from '../../../components/controls/Dropdown';
import Favorite from '../../../components/controls/Favorite';
import HelpTooltip from '../../../components/controls/HelpTooltip';
import HomePageSelect from '../../../components/controls/HomePageSelect';
import ListFooter from '../../../components/controls/ListFooter';
import Modal from '../../../components/controls/Modal';
import Radio from '../../../components/controls/Radio';
import ReloadButton from '../../../components/controls/ReloadButton';
import SearchBox from '../../../components/controls/SearchBox';
import Select, { SearchSelect } from '../../../components/controls/Select';
import SelectList, { SelectListFilter } from '../../../components/controls/SelectList';
import SimpleModal from '../../../components/controls/SimpleModal';
import Tooltip from '../../../components/controls/Tooltip';
import Suggestions from '../../../components/embed-docs-modal/Suggestions';
import AlertErrorIcon from '../../../components/icons/AlertErrorIcon';
import AlertSuccessIcon from '../../../components/icons/AlertSuccessIcon';
import AlertWarnIcon from '../../../components/icons/AlertWarnIcon';
import BranchIcon from '../../../components/icons/BranchIcon';
import BranchLikeIcon from '../../../components/icons/BranchLikeIcon';
import BulletListIcon from '../../../components/icons/BulletListIcon';
import CheckIcon from '../../../components/icons/CheckIcon';
import ClearIcon from '../../../components/icons/ClearIcon';
import DetachIcon from '../../../components/icons/DetachIcon';
import DropdownIcon from '../../../components/icons/DropdownIcon';
import HelpIcon from '../../../components/icons/HelpIcon';
import HistoryIcon from '../../../components/icons/HistoryIcon';
import LockIcon from '../../../components/icons/LockIcon';
import MeasuresIcon from '../../../components/icons/MeasuresIcon';
import PlusCircleIcon from '../../../components/icons/PlusCircleIcon';
import PullRequestIcon from '../../../components/icons/PullRequestIcon';
import QualifierIcon from '../../../components/icons/QualifierIcon';
import SecurityHotspotIcon from '../../../components/icons/SecurityHotspotIcon';
import VulnerabilityIcon from '../../../components/icons/VulnerabilityIcon';
import WarningIcon from '../../../components/icons/WarningIcon';
import DateFormatter from '../../../components/intl/DateFormatter';
import DateFromNow from '../../../components/intl/DateFromNow';
import DateTimeFormatter from '../../../components/intl/DateTimeFormatter';
import Measure from '../../../components/measure/Measure';
import RatingTooltipContent from '../../../components/measure/RatingTooltipContent';
import { Alert } from '../../../components/ui/Alert';
import CoverageRating from '../../../components/ui/CoverageRating';
import DeferredSpinner from '../../../components/ui/DeferredSpinner';
import DuplicationsRating from '../../../components/ui/DuplicationsRating';
import Level from '../../../components/ui/Level';
import MandatoryFieldMarker from '../../../components/ui/MandatoryFieldMarker';
import MandatoryFieldsExplanation from '../../../components/ui/MandatoryFieldsExplanation';
import Rating from '../../../components/ui/Rating';
import {
  getBranchLikeQuery,
  isBranch,
  isMainBranch,
  isPullRequest,
  sortBranches,
} from '../../../helpers/branch-like';
import { throwGlobalError } from '../../../helpers/error';
import { addGlobalSuccessMessage } from '../../../helpers/globalMessages';
import handleRequiredAuthentication from '../../../helpers/handleRequiredAuthentication';
import { translate, translateWithParameters } from '../../../helpers/l10n';
import * as measures from '../../../helpers/measures';
import {
  get,
  getJSON,
  getText,
  omitNil,
  parseError,
  post,
  postJSON,
  postJSONBody,
  request,
} from '../../../helpers/request';
import { sanitizeStringRestricted } from '../../../helpers/sanitize';
import {
  getStandards,
  renderCWECategory,
  renderOwaspTop102021Category,
  renderOwaspTop10Category,
  renderSansTop25Category,
  renderSonarSourceSecurityCategory,
} from '../../../helpers/security-standard';
import {
  getCodeUrl,
  getComponentDrilldownUrl,
  getComponentIssuesUrl,
  getComponentSecurityHotspotsUrl,
  getMeasureHistoryUrl,
  getRulesUrl,
} from '../../../helpers/urls';

const exposeLibraries = () => {
  const global = window as any;

  global.SonarRequest = {
    request,
    get,
    getJSON,
    getText,
    omitNil,
    parseError,
    post,
    postJSON,
    postJSONBody,
    throwGlobalError,
    addGlobalSuccessMessage,
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
        handleRequiredAuthentication,
        isBranch,
        isMainBranch,
        isPullRequest,
        sortBranches,
        getStandards,
        renderCWECategory,
        renderOwaspTop10Category,
        renderOwaspTop102021Category,
        renderSansTop25Category,
        renderSonarSourceSecurityCategory,
        getCodeUrl,
        getComponentDrilldownUrl,
        getComponentIssuesUrl,
        getComponentSecurityHotspotsUrl,
        getMeasureHistoryUrl,
        getRulesUrl,
        sanitizeStringRestricted,
      };
    },
  });

  /**
   * @deprecated since SonarQube 8.7
   */
  Object.defineProperty(global, 'SonarMeasures', {
    get: () => {
      // eslint-disable-next-line no-console
      console.warn('SonarMeasures usages are deprecated since SonarQube 8.7');
      return { ...measures };
    },
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
        BoxedTabs,
        BulletListIcon,
        Button,
        Checkbox,
        CheckIcon,
        ClearIcon,
        ClickEventBoundary,
        ConfirmButton,
        CoverageRating,
        DateFormatter,
        DateFromNow,
        DateTimeFormatter,
        DeferredSpinner,
        DetachIcon,
        DonutChart,
        Dropdown,
        DropdownIcon,
        DuplicationsRating,
        EditButton,
        Favorite,
        FormattedMessage,
        HelpIcon,
        HelpTooltip,
        HistoryIcon,
        HomePageSelect,
        Level,
        ListFooter,
        LockIcon,
        LongLivingBranchIcon: BranchIcon,
        MandatoryFieldMarker,
        MandatoryFieldsExplanation,
        Measure,
        MeasuresIcon,
        Modal,
        NotFound,
        PlusCircleIcon,
        PullRequestIcon,
        QualifierIcon,
        Radio,
        ButtonToggle,
        Rating,
        RatingTooltipContent,
        ReloadButton,
        ResetButtonLink,
        SearchBox,
        SearchSelect,
        SecurityHotspotIcon,
        Select,
        SelectList,
        SelectListFilter,
        SimpleModal,
        SubmitButton,
        Suggestions,
        Tooltip,
        VulnerabilityIcon,
        WarningIcon,
      };
    },
  });
};

export default exposeLibraries;
