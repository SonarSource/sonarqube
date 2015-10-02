import Handlebars from 'hbsfy/runtime';
import ActivationPartial from './templates/_coding-rules-workspace-list-item-activation.hbs';
import FacetHeaderPartial from './templates/facets/_coding-rules-facet-header.hbs';
import MarkdownTipsPartial from '../../components/common/templates/_markdown-tips.hbs';

Handlebars.registerPartial('_coding-rules-workspace-list-item-activation', ActivationPartial);
Handlebars.registerPartial('_coding-rules-facet-header', FacetHeaderPartial);
Handlebars.registerPartial('_markdown-tips', MarkdownTipsPartial);
