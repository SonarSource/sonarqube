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
import { translate } from '../../helpers/l10n';

(function () {

  function TagCloud () {
    window.SonarWidgets.BaseWidget.apply(this, arguments);
    this.addField('width', []);
    this.addField('height', []);
    this.addField('tags', []);
    this.addField('maxResultsReached', false);
  }

  TagCloud.prototype = new window.SonarWidgets.BaseWidget();

  TagCloud.prototype.sizeHigh = 24;

  TagCloud.prototype.sizeLow = 10;

  TagCloud.prototype.renderWords = function () {
    var that = this;
    return window.sonarqube.appStarted.then(function () {
      var words = that.wordContainer.selectAll('.cloud-word').data(that.tags()),
          wordsEnter = words.enter().append('a').classed('cloud-word', true);
      wordsEnter.text(function (d) {
        return d.key;
      });
      wordsEnter.attr('href', function (d) {
        var url = that.options().baseUrl + '|tags=' + d.key;
        if (that.options().createdAfter) {
          url += '|createdAfter=' + that.options().createdAfter;
        }
        return url;
      });
      wordsEnter.attr('title', function (d) {
        return that.tooltip(d);
      });
      words.style('font-size', function (d) {
        return that.size(d.value) + 'px';
      });
      return words.sort(function (a, b) {
        if (a.key.toLowerCase() > b.key.toLowerCase()) {
          return 1;
        } else {
          return -1;
        }
      });
    });
  };

  TagCloud.prototype.render = function (container) {
    var box = d3.select(container).append('div');
    box.classed('sonar-d3', true);
    box.classed('cloud-widget', true);
    this.wordContainer = box.append('div');
    var sizeDomain = d3.extent(this.tags(), function (d) {
      return d.value;
    });
    this.size = d3.scale.linear().domain(sizeDomain).range([this.sizeLow, this.sizeHigh]);
    if (this.maxResultsReached()) {
      var maxResultsReachedLabel = box.append('div').text(this.options().maxItemsReachedMessage);
      maxResultsReachedLabel.classed('max-results-reached-message', true);
    }
    this.renderWords();
    return window.SonarWidgets.BaseWidget.prototype.render.apply(this, arguments);
  };

  TagCloud.prototype.tooltip = function (d) {
    var suffixKey = d.value === 1 ? 'issue' : 'issues',
        suffix = translate(suffixKey);
    return (d.value + '\u00a0') + suffix;
  };

  TagCloud.prototype.parseSource = function (response) {
    return this.tags(response.tags);
  };

  window.SonarWidgets.TagCloud = TagCloud;

})();
