import $ from 'jquery';
import {getCurrentUser} from '../../api/users';
import {component} from '../../api/navigation';
import NavApp from '../nav/app';

const APP_URL_MAPPING = {
  '': 'dashboard/app',
  'account': 'account/app',
  'api_documentation': 'api-documentation/app',
  'coding_rules': 'coding-rules/app',
  'component_issues': 'issues/app-context',
  'component': 'source-viewer/app',
  'computation': 'computation/app',
  'custom_measures': 'custom-measures/app',
  'dashboard': 'dashboard/app',
  'drilldown': 'drilldown/app',
  'groups': 'groups/app',
  'issues/search': 'issues/app',
  'maintenance': { name: 'maintenance/app', options: { setup: false } },
  'markdown': 'markdown/app',
  'measures': 'measures/app',
  'metrics': 'metrics/app',
  'overview': 'overview/app',
  'permission_templates': 'select-list/app',
  'profiles': 'quality-profiles/app',
  'project_roles': 'project-permissions/app',
  'provisioning': 'provisioning/app',
  'quality_gates': 'quality-gates/app',
  'roles/global': 'global-permissions/app',
  'roles/projects': 'project-permissions/app',
  'setup': { name: 'maintenance/app', options: { setup: true } },
  'updatecenter': 'update-center/app',
  'users': 'users/app'
};


class App {
  constructor(options) {
    this.user = null;
    this.component = null;
    this.options = options;
  }

  /**
   * Start the Main App
   */
  start() {
    if (window.location.pathname.indexOf('/sessions/') !== -1) {
      // do not run any app on the login page
      return;
    }

    App.initLanguage(this.options.lang);
    $.when(
        window.requestMessages(),
        this.loadUserDetails(),
        this.loadComponentDetails()
    ).done(() => {
      this.startNav();
      this.startPageApp();
    });
  }


  /**
   * Start the Navigation App
   */
  startNav() {
    NavApp.start(_.extend({}, this.options, { user: this.user }));
  }


  /**
   * Start an App for the current page
   */
  startPageApp() {
    let app = this.getApp();
    app && this.startApp(app);
  }


  /**
   * Start an App with a given name
   * @param {object} app
   */
  startApp(app) {
    let appScript = 'apps/' + app.name;
    require([appScript], App => {
      let appOptions = {
        el: '#content',
        component: this.options.component,
        user: this.user,
        urlRoot: app.urlRoot
      };
      _.extend(appOptions, app.options);
      App.start(appOptions);
    });
  }


  /**
   * Initialize formatting libraries for a given language
   * @param {string} lang
   */
  static initLanguage(lang) {
    moment.lang(lang);
    numeral.language(lang);
  }


  /**
   * Get a part of a page URL representing a App name
   * @returns {string}
   */
  static getAppPath() {
    let path = window.location.pathname;
    let relativePath = path.substr(window.baseUrl.length);
    return relativePath.substr(1) + '/';
  }


  /**
   * Try to get a App name for the current page
   * @returns {null|object}
   */
  getApp() {
    let appPath = App.getAppPath();
    let matchedUrl = _.find(Object.keys(APP_URL_MAPPING), urlPrefix => {
      let test = urlPrefix + '/';
      return appPath.indexOf(test) === 0;
    });
    if (matchedUrl == null) {
      return null;
    }
    let app = APP_URL_MAPPING[matchedUrl];
    return {
      name: typeof app === 'string' ? app : app.name,
      options: typeof app === 'string' ? {} : app.options,
      urlRoot: window.baseUrl + '/' + matchedUrl
    };
  }


  /**
   * Load current component details
   * @returns {jqXHR}
   */
  loadComponentDetails() {
    if (!this.options.componentKey) {
      return $.Deferred().resolve().promise();
    }
    return component(this.options.componentKey).done(component => {
      this.options.component = component;
      this.options.component.qualifier = _.last(component.breadcrumbs).qualifier;
    });
  }


  /**
   * Load current user details
   * @returns {jqXHR}
   */
  loadUserDetails() {
    return getCurrentUser().done(user => {
      this.user = user;
      this.user.isAdmin = user.permissions.global.indexOf('admin') !== -1;
    });
  }
}

export default App;
