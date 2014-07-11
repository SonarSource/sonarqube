class Treemap extends window.SonarWidgets.BaseWidget
  sizeLow: 11
  sizeHigh: 18


  constructor: ->
    @addField 'width', null
    @addField 'height', null
    @addField 'maxResultsReached', false
    super


  getNodes: ->
    @treemap.nodes(children: @components()).filter (d) -> !d.children


  renderTreemap: ->
    nodes = @getNodes()
    @cells = @box.selectAll('.treemap-cell').data nodes
    @cells.exit().remove()

    cellsEnter = @cells.enter().append 'div'
    cellsEnter.classed 'treemap-cell', true
    cellsEnter.append('div').classed 'treemap-inner', true
    cellsEnter.append('a').classed 'treemap-link', true

    @cells.attr 'title', (d) => @tooltip d
    @cells.style 'background-color', (d) =>
      if @colorMetric.value(d)? then @color @colorMetric.value(d) else @colorUnknown
    @cells.classed 'treemap-cell-drilldown', (d) ->
      d.qualifier? && d.qualifier != 'FIL' && d.qualifier != 'CLA'

    prefix = @mostCommonPrefix _.pluck @components(), 'longName'
    prefixLength = prefix.length
    @cellsInner = @box.selectAll('.treemap-inner').data nodes
    @cellsInner.html (d) ->
      if prefixLength > 0
        "#{prefix}<br>#{d.longName.substr prefixLength}"
      else d.longName

    @cellsLink = @box.selectAll('.treemap-link').data nodes
    @cellsLink.html '<i class="icon-link"></i>'
    @cellsLink.attr 'href', (d) =>
      url = @options().baseUrl + encodeURIComponent(d.key)
      url += '?metric=' + encodeURIComponent(@colorMetric.key) if d.qualifier == 'CLA' || d.qualifier == 'FIL'
      url

    @attachEvents cellsEnter

    @maxResultsReachedLabel.style 'display', if @maxResultsReached() then 'block' else 'none'


  updateTreemap: (components, maxResultsReached) ->
    @components components
    @maxResultsReached maxResultsReached

    @renderTreemap()
    @positionCells()


  attachEvents: (cells) ->
    cells.on 'click', (d) => @requestChildren d


  positionCells: ->
    @cells.style 'left', (d) -> "#{d.x}px"
    @cells.style 'top', (d) -> "#{d.y}px"
    @cells.style 'width', (d) -> "#{d.dx}px"
    @cellsInner.style 'max-width', (d) -> "#{d.dx}px"
    @cells.style 'height', (d) -> "#{d.dy}px"
    @cells.style 'line-height', (d) -> "#{d.dy}px"
    @cells.style 'font-size', (d) => "#{@size (d.dx / d.longName.length)}px"
    @cellsLink.style 'font-size', (d) => "#{@sizeLink Math.min(d.dx, d.dy)}px"
    @cells.classed 'treemap-cell-small', (d) -> (d.dx / d.longName.length) < 1 || d.dy < 40
    @cells.classed 'treemap-cell-very-small', (d) -> d.dx < 24 || d.dy < 24


  renderLegend: (box) ->
    @legend = box.insert 'div', ':first-child'
    @legend.classed 'legend', true
    @legend.classed 'legend-html', true
    @legend.append('span').classed('legend-text', true).html "Size: <span class='legend-text-main'>#{@sizeMetric.name}</span>"
    @legend.append('span').classed('legend-text', true).html "Color: <span class='legend-text-main'>#{@colorMetric.name}</span>"


  renderBreadcrumbs: (box) ->
    @breadcrumbsBox = box.append('div').classed 'treemap-breadcrumbs', true
    @breadcrumbs = []
    d = name: '<i class="icon-home"></i>', components: @components(), maxResultsReached: @maxResultsReached()
    @addToBreadcrumbs d


  updateBreadcrumbs: ->
    breadcrumbs = @breadcrumbsBox.selectAll('.treemap-breadcrumbs-item').data @breadcrumbs
    breadcrumbs.exit().remove()
    breadcrumbsEnter = breadcrumbs.enter().append('span').classed 'treemap-breadcrumbs-item', true
    breadcrumbsEnter.attr 'title', (d) =>
      if d.longName? then d.longName else @options().resource
    breadcrumbsEnter.append('i').classed('icon-chevron-right', true).style 'display', (d, i) ->
      if i > 0 then 'inline' else 'none'
    breadcrumbsEnter.append('i').attr 'class', (d) ->
      if d.qualifier? then "icon-qualifier-#{d.qualifier.toLowerCase()}" else ''
    breadcrumbsEnterLinks = breadcrumbsEnter.append 'a'
    breadcrumbsEnterLinks.classed 'underlined-link', (d, i) -> i > 0
    breadcrumbsEnterLinks.html (d) -> d.name
    breadcrumbsEnterLinks.on 'click', (d) =>
      @updateTreemap d.components, d.maxResultsReached
      @cutBreadcrumbs d
    @breadcrumbsBox.style 'display', if @breadcrumbs.length < 2 then 'none' else 'block'


  addToBreadcrumbs: (d) ->
    @breadcrumbs.push d
    @updateBreadcrumbs()


  cutBreadcrumbs: (lastElement) ->
    index = null
    @breadcrumbs.forEach (d, i) ->
      index = i if d.key == lastElement.key
    if index?
      @breadcrumbs = _.initial @breadcrumbs, @breadcrumbs.length - index - 1
      @updateBreadcrumbs()


  getColorScale: ->
    return @getLevelColorScale() if @colorMetric.type == 'LEVEL'
    return @getRatingColorScale() if @colorMetric.type == 'RATING'
    @getPercentColorScale()


  getPercentColorScale: ->
    color = d3.scale.linear().domain([0, 25, 50, 75, 100])
    color.range if @colorMetric.direction == 1 then @colors5 else @colors5r
    color


  getRatingColorScale: ->
    color = d3.scale.ordinal().domain([1, 2, 3, 4, 5]).range @colors5r
    color


  getLevelColorScale: ->
    color = d3.scale.ordinal().domain(['ERROR', 'WARN', 'OK', 'NONE']).range @colorsLevel
    color


  render: (container) ->
    box = d3.select(container).append('div')
    box.classed 'sonar-d3', true
    @box = box.append('div').classed 'treemap-container', true

    # Configure metrics
    @addMetric 'colorMetric', 0
    @addMetric 'sizeMetric', 1

    # Configure scales
    @color = @getColorScale()

    @size = d3.scale.linear().domain([3, 15]).range([@sizeLow, @sizeHigh]).clamp true
    @sizeLink = d3.scale.linear().domain([60, 100]).range([12, 14]).clamp true

    @treemap = d3.layout.treemap()
    @treemap.sort (a, b) -> a.value - b.value
    @treemap.round true

    @treemap.value (d) => @sizeMetric.value d

    @maxResultsReachedLabel = box.append('div').text @options().maxItemsReachedMessage
    @maxResultsReachedLabel.classed 'max-results-reached-message', true

    @renderLegend box
    @renderBreadcrumbs box
    @renderTreemap()
    super


  update: ->
    @width @box.property 'offsetWidth'
    @height (@width() / 100.0 * @options().heightInPercents)
    @box.style 'height', "#{@height()}px"
    @treemap.size [@width(), @height()]
    @cells.data @getNodes()
    @positionCells()


  formatComponents: (data) ->
    components = _.filter data, (component) =>
      hasSizeMetric = => _.findWhere component.msr, key: @sizeMetric.key
      _.isArray(component.msr) && component.msr.length > 0 && hasSizeMetric()

    if _.isArray(components) && components.length > 0
      components.map (component) =>
        measures = {}
        component.msr.forEach (measure) ->
          measures[measure.key] = val: measure.val, fval: measure.frmt_val

        key: if component.copy? then component.copy else component.key
        name: component.name
        longName: component.lname
        qualifier: component.qualifier
        measures: measures


  requestChildren: (d) ->
    metrics = @metricsPriority().join ','
    RESOURCES_URL = "#{baseUrl}/api/resources/index"
    jQuery.get(RESOURCES_URL, resource: d.key, depth: 1, metrics: metrics).done (r) =>
      components = @formatComponents r
      if components?
        components = _.sortBy components, (d) => @sizeMetric.value d
        components = _.initial components, components.length - @options().maxItems - 1
        @updateTreemap components, components.length > @options().maxItems
        @addToBreadcrumbs _.extend d, components: components, maxResultsReached: @maxResultsReached()


  mostCommonPrefix: (strings) ->
    sortedStrings = strings.slice(0).sort()
    firstString = sortedStrings[0]
    firstStringLength = firstString.length
    lastString = sortedStrings[sortedStrings.length - 1]
    i = 0
    while i < firstStringLength && firstString.charAt(i) == lastString.charAt(i)
      i++
    prefix = firstString.substr 0, i
    lastPrefixPart = _.last prefix.split /[\s\\\/]/
    prefix.substr 0, prefix.length - lastPrefixPart.length




window.SonarWidgets.Treemap = Treemap
