import Handlebars from 'hbsfy/runtime';
import ChangeEntryPartial from './templates/_update-center-plugin-changelog-entry.hbs';
import ActionsPartial from './templates/_update-center-plugin-actions.hbs';

Handlebars.registerPartial('_update-center-plugin-changelog-entry', ChangeEntryPartial);
Handlebars.registerPartial('_update-center-plugin-actions', ActionsPartial);
