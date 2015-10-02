import Handlebars from 'hbsfy/runtime';
import FilterNamePartial from './templates/_issues-filter-name.hbs';
import FacetHeaderPartial from './templates/facets/_issues-facet-header.hbs';
import MarkdownTipsPartial from '../../components/common/templates/_markdown-tips.hbs';

Handlebars.registerPartial('_issues-filter-name', FilterNamePartial);
Handlebars.registerPartial('_issues-facet-header', FacetHeaderPartial);
Handlebars.registerPartial('_markdown-tips', MarkdownTipsPartial);
