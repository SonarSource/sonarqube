import Handlebars from 'hbsfy/runtime';

import StateMigrationFailedPartial from './templates/_maintenance-state-migration-failed.hbs';
import StateMigrationNotSupportedPartial from './templates/_maintenance-state-migration-not-supported.hbs';
import StateMigrationRequiredPartial from './templates/_maintenance-state-migration-required.hbs';
import StateMigrationRunningPartial from './templates/_maintenance-state-migration-running.hbs';
import StateMigrationSucceededPartial from './templates/_maintenance-state-migration-succeeded.hbs';
import StateNoMigrationPartial from './templates/_maintenance-state-no-migration.hbs';

import StatusDownPartial from './templates/_maintenance-status-down.hbs';
import StatusMigrationPartial from './templates/_maintenance-status-migration.hbs';
import StatusUpPartial from './templates/_maintenance-status-up.hbs';

Handlebars.registerPartial('_maintenance-state-migration-failed', StateMigrationFailedPartial);
Handlebars.registerPartial('_maintenance-state-migration-not-supported', StateMigrationNotSupportedPartial);
Handlebars.registerPartial('_maintenance-state-migration-required', StateMigrationRequiredPartial);
Handlebars.registerPartial('_maintenance-state-migration-running', StateMigrationRunningPartial);
Handlebars.registerPartial('_maintenance-state-migration-succeeded', StateMigrationSucceededPartial);
Handlebars.registerPartial('_maintenance-state-no-migration', StateNoMigrationPartial);

Handlebars.registerPartial('_maintenance-status-down', StatusDownPartial);
Handlebars.registerPartial('_maintenance-status-migration', StatusMigrationPartial);
Handlebars.registerPartial('_maintenance-status-up', StatusUpPartial);
