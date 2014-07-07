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
      url = @options().baseUrl + encodeURIComponent(d.key)
      url += '?metric=' + encodeURIComponent(@colorMetric.key) if d.qualifier == 'CLA' || d.qualifier == 'FIL'
      url
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
