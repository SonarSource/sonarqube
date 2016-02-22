/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import BaseFilters from './base-filters';
import Template from '../templates/choice-filter.hbs';
import ItemTemplate from '../templates/choice-filter-item.hbs';
import { translate } from '../../../helpers/l10n';

const DetailsChoiceFilterView = BaseFilters.DetailsFilterView.extend({
  template: Template,
  itemTemplate: ItemTemplate,


  events () {
    return {
      'click label': 'onCheck'
    };
  },


  render () {
    BaseFilters.DetailsFilterView.prototype.render.apply(this, arguments);
    this.updateLists();
  },


  renderList (collection, selector) {
    const that = this;
    const container = this.$(selector);

    container.empty().toggleClass('hidden', collection.length === 0);
    collection.each(function (item) {
      container.append(
          that.itemTemplate(_.extend(item.toJSON(), {
            multiple: that.model.get('multiple') && item.get('id')[0] !== '!'
          }))
      );
    });
  },


  updateLists () {
    const choices = new Backbone.Collection(this.options.filterView.choices.reject(function (item) {
      return item.get('id')[0] === '!';
    }));
    const opposite = new Backbone.Collection(this.options.filterView.choices.filter(function (item) {
      return item.get('id')[0] === '!';
    }));

    this.renderList(choices, '.choices');
    this.renderList(opposite, '.opposite');

    const current = this.currentChoice || 0;
    this.updateCurrent(current);
  },


  onCheck (e) {
    const checkbox = $(e.currentTarget);
    const id = checkbox.data('id');
    const checked = checkbox.find('.icon-checkbox-checked').length > 0;

    if (this.model.get('multiple')) {
      if (checkbox.closest('.opposite').length > 0) {
        this.options.filterView.choices.each(function (item) {
          item.set('checked', false);
        });
      } else {
        this.options.filterView.choices.filter(function (item) {
          return item.get('id')[0] === '!';
        }).forEach(function (item) {
          item.set('checked', false);
        });
      }
    } else {
      this.options.filterView.choices.each(function (item) {
        item.set('checked', false);
      });
    }

    this.options.filterView.choices.get(id).set('checked', !checked);
    this.updateValue();
    this.updateLists();
  },


  updateValue () {
    this.model.set('value', this.options.filterView.getSelected().map(function (m) {
      return m.get('id');
    }));
  },


  updateCurrent (index) {
    this.currentChoice = index;
    this.$('label').removeClass('current')
        .eq(this.currentChoice).addClass('current');
  },


  onShow () {
    this.bindedOnKeyDown = _.bind(this.onKeyDown, this);
    $('body').on('keydown', this.bindedOnKeyDown);
  },


  onHide () {
    $('body').off('keydown', this.bindedOnKeyDown);
  },


  onKeyDown (e) {
    switch (e.keyCode) {
      case 38:
        e.preventDefault();
        this.selectPrevChoice();
        break;
      case 40:
        e.preventDefault();
        this.selectNextChoice();
        break;
      case 13:
        e.preventDefault();
        this.selectCurrent();
        break;
      default:
        // Not a functional key - then skip
        break;
    }
  },


  selectNextChoice () {
    if (this.$('label').length > this.currentChoice + 1) {
      this.updateCurrent(this.currentChoice + 1);
      this.scrollNext();
    }
  },


  scrollNext () {
    const currentLabel = this.$('label').eq(this.currentChoice);
    if (currentLabel.length > 0) {
      const list = currentLabel.closest('ul');
      const labelPos = currentLabel.offset().top - list.offset().top + list.scrollTop();
      const deltaScroll = labelPos - list.height() + currentLabel.outerHeight();

      if (deltaScroll > 0) {
        list.scrollTop(deltaScroll);
      }
    }
  },


  selectPrevChoice () {
    if (this.currentChoice > 0) {
      this.updateCurrent(this.currentChoice - 1);
      this.scrollPrev();
    }
  },


  scrollPrev () {
    const currentLabel = this.$('label').eq(this.currentChoice);
    if (currentLabel.length > 0) {
      const list = currentLabel.closest('ul');
      const labelPos = currentLabel.offset().top - list.offset().top;

      if (labelPos < 0) {
        list.scrollTop(list.scrollTop() + labelPos);
      }
    }
  },


  selectCurrent () {
    const cb = this.$('label').eq(this.currentChoice);
    cb.click();
  },


  serializeData () {
    return _.extend({}, this.model.toJSON(), {
      choices: new Backbone.Collection(this.options.filterView.choices.reject(function (item) {
        return item.get('id')[0] === '!';
      })).toJSON(),
      opposite: new Backbone.Collection(this.options.filterView.choices.filter(function (item) {
        return item.get('id')[0] === '!';
      })).toJSON()
    });
  }

});


const ChoiceFilterView = BaseFilters.BaseFilterView.extend({

  initialize (options) {
    BaseFilters.BaseFilterView.prototype.initialize.call(this, {
      detailsView: (options && options.detailsView) ? options.detailsView : DetailsChoiceFilterView
    });

    let index = 0;
    const icons = this.model.get('choiceIcons');

    this.choices = new Backbone.Collection(
        _.map(this.model.get('choices'), function (value, key) {
          const model = new Backbone.Model({
            id: key,
            text: value,
            checked: false,
            index: index++
          });

          if (icons && icons[key]) {
            model.set('icon', icons[key]);
          }

          return model;
        }), { comparator: 'index' }
    );
  },


  getSelected () {
    return this.choices.filter(function (m) {
      return m.get('checked');
    });
  },


  renderInput () {
    const input = $('<select>')
        .prop('name', this.model.get('property'))
        .prop('multiple', true)
        .css('display', 'none');
    this.choices.each(function (item) {
      const option = $('<option>')
          .prop('value', item.get('id'))
          .prop('selected', item.get('checked'))
          .text(item.get('text'));
      option.appendTo(input);
    });
    input.appendTo(this.$el);
  },


  renderValue () {
    const value = this.getSelected().map(function (item) {
      return item.get('text');
    });
    const defaultValue = this.model.has('defaultValue') ?
        this.model.get('defaultValue') :
        this.model.get('multiple') ? translate('all') : translate('any');

    return this.isDefaultValue() ? defaultValue : value.join(', ');
  },


  isDefaultValue () {
    const selected = this.getSelected();
    return selected.length === 0;
  },


  disable () {
    this.choices.each(function (item) {
      item.set('checked', false);
    });
    BaseFilters.BaseFilterView.prototype.disable.apply(this, arguments);
  },


  restoreFromQuery (q) {
    let param = _.findWhere(q, { key: this.model.get('property') });

    if (this.choices) {
      this.choices.forEach(function (item) {
        if (item.get('id')[0] === '!') {
          let x = _.findWhere(q, { key: item.get('id').substr(1) });
          if (item.get('id').indexOf('=') >= 0) {
            const key = item.get('id').split('=')[0].substr(1);
            const value = item.get('id').split('=')[1];
            x = _.findWhere(q, { key, value });
          }
          if (x == null) {
            return;
          }
          if (!param) {
            param = { value: item.get('id') };
          } else {
            param.value += ',' + item.get('id');
          }
        }
      });
    }

    if (param && param.value) {
      this.model.set('enabled', true);
      this.restore(param.value, param);
    } else {
      this.clear();
    }
  },


  restore (value) {
    if (_.isString(value)) {
      value = value.split(',');
    }

    if (this.choices && value.length > 0) {
      const that = this;

      that.choices.each(function (item) {
        item.set('checked', false);
      });

      const unknownValues = [];

      _.each(value, function (v) {
        const cModel = that.choices.findWhere({ id: v });
        if (cModel) {
          cModel.set('checked', true);
        } else {
          unknownValues.push(v);
        }
      });

      value = _.difference(value, unknownValues);

      this.model.set({
        value,
        enabled: true
      });

      this.render();
    } else {
      this.clear();
    }
  },


  clear () {
    if (this.choices) {
      this.choices.each(function (item) {
        item.set('checked', false);
      });
    }
    this.model.unset('value');
    this.detailsView.render();
    if (this.detailsView.updateCurrent) {
      this.detailsView.updateCurrent(0);
    }
  },


  formatValue () {
    const q = {};
    if (this.model.has('property') && this.model.has('value') && this.model.get('value').length > 0) {
      const opposite = _.filter(this.model.get('value'), function (item) {
        return item[0] === '!';
      });
      if (opposite.length > 0) {
        opposite.forEach(function (item) {
          if (item.indexOf('=') >= 0) {
            const paramValue = item.split('=');
            q[paramValue[0].substr(1)] = paramValue[1];
          } else {
            q[item.substr(1)] = false;
          }
        });
      } else {
        q[this.model.get('property')] = this.model.get('value').join(',');
      }
    }
    return q;
  }

});


/*
 * Export public classes
 */

export default {
  DetailsChoiceFilterView,
  ChoiceFilterView
};
