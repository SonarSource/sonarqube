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

import { IconChevronDown } from '@sonarsource/echoes-react';
import {
  ButtonSecondary,
  Dropdown,
  ItemDivider,
  ItemLink,
  PopupPlacement,
  PopupZLevel,
} from 'design-system';
import * as React from 'react';
import { getAlmSettings } from '../../../api/alm-settings';
import withCurrentUserContext from '../../../app/components/current-user/withCurrentUserContext';
import { Button } from '../../../components/controls/buttons';
import { IMPORT_COMPATIBLE_ALMS } from '../../../helpers/constants';
import { translate } from '../../../helpers/l10n';
import { hasGlobalPermission } from '../../../helpers/users';
import { AlmKeys, AlmSettingsInstance } from '../../../types/alm-settings';
import { Permissions } from '../../../types/permissions';
import { LoggedInUser } from '../../../types/users';

import { Location, Router, withRouter } from '../../../components/hoc/withRouter';

interface Props {
  currentUser: LoggedInUser;
  location: Location;
  router: Router;
}

interface State {
  boundAlms: Array<string>;
  showProjectsLink: boolean;
}

const almSettingsValidators = {
  [AlmKeys.Azure]: (settings: AlmSettingsInstance) => Boolean(settings.url),
  [AlmKeys.BitbucketCloud]: (_: AlmSettingsInstance) => true,
  [AlmKeys.BitbucketServer]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitHub]: (_: AlmSettingsInstance) => true,
  [AlmKeys.GitLab]: (settings: AlmSettingsInstance) => Boolean(settings.url),
};

export class ProjectCreationMenu extends React.PureComponent<Props, State> {
  mounted = false;
  state: State = { boundAlms: [],
                  showProjectsLink: true };

  componentDidMount() {
    this.mounted = true;
    const href = window.location.href;
    let boolValue = false;
    boolValue = href.indexOf("organizations")>0;
    this.setState({showProjectsLink:boolValue})

  }

  componentWillUnmount() {
    this.mounted = false;
  }

  almSettingIsValid = (settings: AlmSettingsInstance) => {
    return almSettingsValidators[settings.alm](settings);
  };

  fetchAlmBindings = async () => {
    const { currentUser } = this.props;
    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    // getAlmSettings requires branchesEnabled
    if (!canCreateProject) {
      return;
    }

    const almSettings: AlmSettingsInstance[] = await getAlmSettings().catch(() => []);

    const boundAlms = IMPORT_COMPATIBLE_ALMS.filter((key) => {
      const currentAlmSettings = almSettings.filter((s) => s.alm === key);

      return (
        currentAlmSettings.length > 0 &&
        key === currentAlmSettings[0].alm &&
        this.almSettingIsValid(currentAlmSettings[0])
      );
    });

    if (this.mounted) {
      this.setState({
        boundAlms,
      });
    }
  };

  handleProjectCreate=()=> {
    let path = this.props.location.pathname;
    let newPath = "";
    if(path.endsWith("/projects")){
       newPath = path.replace("projects", "extension/developer/projects");
     }
     this.props.router.push(newPath);
  }

  render() {
    const { currentUser } = this.props;
    const { showProjectsLink } = this.state;

    const canCreateProject = hasGlobalPermission(currentUser, Permissions.ProjectCreation);

    if (!canCreateProject) {
      return null;
    }

    if (!showProjectsLink) {
      return null;
    }

    return (
      <Button className="button-primary" onClick={this.handleProjectCreate}>
        {translate('projects.add')}

        <IconChevronDown className="sw-ml-1" />
      </Button>
    );
  }
}

export default withRouter(withCurrentUserContext(ProjectCreationMenu));
