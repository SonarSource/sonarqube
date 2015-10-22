import Handlebars from 'hbsfy/runtime';
import AllMeasuresPartial from './templates/measures/_source-viewer-measures-all.hbs';
import CoveragePartial from './templates/measures/_source-viewer-measures-coverage.hbs';
import DuplicationsPartial from './templates/measures/_source-viewer-measures-duplications.hbs';
import IssuesPartial from './templates/measures/_source-viewer-measures-issues.hbs';
import LinesPartial from './templates/measures/_source-viewer-measures-lines.hbs';
import TestCasesPartial from './templates/measures/_source-viewer-measures-test-cases.hbs';
import TestsPartial from './templates/measures/_source-viewer-measures-tests.hbs';

Handlebars.registerPartial('_source-viewer-measures-all', AllMeasuresPartial);
Handlebars.registerPartial('_source-viewer-measures-coverage', CoveragePartial);
Handlebars.registerPartial('_source-viewer-measures-duplications', DuplicationsPartial);
Handlebars.registerPartial('_source-viewer-measures-issues', IssuesPartial);
Handlebars.registerPartial('_source-viewer-measures-lines', LinesPartial);
Handlebars.registerPartial('_source-viewer-measures-test-cases', TestCasesPartial);
Handlebars.registerPartial('_source-viewer-measures-tests', TestsPartial);
