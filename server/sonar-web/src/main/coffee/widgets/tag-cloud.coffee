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

class TagCloud extends window.SonarWidgets.BaseWidget
  sizeLow: 10
  sizeHigh: 24


  constructor: ->
    @addField 'width', []
    @addField 'height', []
    @addField 'tags', []
    @addField 'maxResultsReached', false
    super


  renderWords: ->
    window.requestMessages().done =>
      words = @wordContainer.selectAll('.cloud-word').data @tags()

      wordsEnter = words.enter().append('a').classed 'cloud-word', true
      wordsEnter.text (d) -> d.key
      wordsEnter.attr 'href', (d) =>
        url = @options().baseUrl + '|tags=' + d.key
        if @options().createdAfter
          url += '|createdAfter=' + @options().createdAfter
        url
      wordsEnter.attr 'title', (d) => @tooltip d

      words.style 'font-size', (d) =>
        "#{@size d.value}px"

      words.sort (a, b) =>
        if a.key.toLowerCase() > b.key.toLowerCase() then 1 else -1


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    box.classed 'cloud-widget', true
    @wordContainer = box.append 'div'

    sizeDomain = d3.extent @tags(), (d) => d.value
    @size = d3.scale.linear().domain(sizeDomain).range [@sizeLow, @sizeHigh]

    # Show maxResultsReached message
    if @maxResultsReached()
      maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
      maxResultsReachedLabel.classed 'max-results-reached-message', true

    @renderWords()

    super


  tooltip: (d) ->
    suffixKey = if d.value == 1 then 'issue' else 'issues'
    suffix = t(suffixKey)
    "#{d.value}\u00a0" + suffix


  parseSource: (response) ->
    @tags(response.tags)


window.SonarWidgets.TagCloud = TagCloud
