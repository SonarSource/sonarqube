#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

class WordCloud extends window.SonarWidgets.BaseWidget
  sizeLow: 10
  sizeHigh: 24


  constructor: ->
    @addField 'width', []
    @addField 'height', []
    @addField 'maxResultsReached', false
    super


  renderWords: ->
    words = @wordContainer.selectAll('.cloud-word').data @components()

    wordsEnter = words.enter().append('a').classed 'cloud-word', true
    wordsEnter.text (d) -> d.name
    wordsEnter.attr 'href', (d) =>
      @options().baseUrl + '?id=' + encodeURIComponent(d.key)
    wordsEnter.attr 'title', (d) => @tooltip d

    words.style 'color', (d) =>
      if @colorMetric.value(d)? then @color @colorMetric.value(d) else @colorUnknown
    words.style 'font-size', (d) => "#{@size @sizeMetric.value d}px"

    words.sort (a, b) =>
      if a.name.toLowerCase() > b.name.toLowerCase() then 1 else -1


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    box.classed 'cloud-widget', true
    @wordContainer = box.append 'div'

    # Configure metrics
    @addMetric 'colorMetric', 0
    @addMetric 'sizeMetric', 1

    # Configure scales
    @color = d3.scale.linear().domain([0, 33, 67, 100])
    if @colorMetric.direction == 1
      @color.range @colors4
    else
      @color.range @colors4r

    sizeDomain = d3.extent @components(), (d) => @sizeMetric.value d
    @size = d3.scale.linear().domain(sizeDomain).range [@sizeLow, @sizeHigh]

    # Show maxResultsReached message
    if @maxResultsReached()
      maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
      maxResultsReachedLabel.classed 'max-results-reached-message', true

    @renderWords()

    super



window.SonarWidgets.WordCloud = WordCloud
