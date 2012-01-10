 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2008-2012 SonarSource
 # mailto:contact AT sonarsource DOT com
 #
 # Sonar is free software; you can redistribute it and/or
 # modify it under the terms of the GNU Lesser General Public
 # License as published by the Free Software Foundation; either
 # version 3 of the License, or (at your option) any later version.
 #
 # Sonar is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 # Lesser General Public License for more details.
 #
 # You should have received a copy of the GNU Lesser General Public
 # License along with Sonar; if not, write to the Free Software
 # Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 #
module SourceHelper

  #
  # Options :
  # - display_scm : boolean (default is true)
  # - display_violations: boolean (default is false)
  # - display_coverage: boolean (default is false)
  # - expand: boolean (default is false). Only if display_violations or display_coverage is true
  # - min_date
  # - line_range
  #
  def snapshot_source_to_html(snapshot, options={})
    return '' unless snapshot && snapshot.source

    panel=SourcePanel.new
    revisions_by_line={}
    authors_by_line={}
    dates_by_line={}
    unless options[:display_scm]==false
      panel.display_scm=(snapshot.measure('last_commit_datetimes_by_line')!=nil)
      authors_by_line=load_distribution(snapshot,'authors_by_line')
      revisions_by_line=load_distribution(snapshot,'revisions_by_line')
      dates_by_line=load_distribution(snapshot,'last_commit_datetimes_by_line')
    end

    panel.html_lines=[]
    line_range=sanitize_line_range(options[:line_range])
    highlighted_lines=options[:highlighted_lines]||[]
    snapshot.source.syntax_highlighted_lines().each_with_index do |source, index|
      if line_range.include?(index+1)
        html_line=HtmlLine.new(source, index+1)
        html_line.revision=revisions_by_line[index+1]
        html_line.author=authors_by_line[index+1]
        html_line.highlighted=(highlighted_lines.include?(index+1))
        date_string=dates_by_line[index+1]
        html_line.datetime=(date_string ? Java::OrgSonarApiUtils::DateUtils.parseDateTime(date_string): nil)
        panel.html_lines<<html_line
      end
    end

    panel.filter_min_date(options[:min_date]) if options[:min_date]
    
    render :partial => 'source/source', :locals => {:panel => panel}
  end

  def load_distribution(snapshot, metric_key)
    m=snapshot.measure(metric_key)
    m ? m.data_as_line_distribution() : {}
  end

  def sanitize_line_range(range)
    if range
      ([range.min, 1].max)..(range.max)
    else
      [0..-1]
    end
  end

  class SourcePanel
    attr_accessor :filtered, :expanded, :html_lines, :display_scm

    def empty?
      @html_lines.nil? || @html_lines.empty?
    end

    def filter_min_date(min_date)
      @filtered=true
      to=Java::JavaUtil::Date.new(min_date.to_f * 1000)
      if to
        @html_lines.each do |line|
          line.flag_as_hidden() if !line.after(to)
        end
      end
    end
  end

  class HtmlLine
    attr_accessor :id, :source, :revision, :author, :datetime, :violations, :hits, :conditions, :covered_conditions, :hidden, :selected, :highlighted, :deprecated_conditions_label

    def initialize(source, id)
      @source=source
      @id=id
    end

    def add_violation(violation)
      @violations||=[]
      @violations<<violation
      @visible=true
    end

    def violations?
      @violations && @violations.size>0
    end

    def violation_severity
      if @violations && @violations.size>0
        @violations[0].failure_level
      else
        nil
      end
    end

    def after(date)
      if date && @datetime
        @datetime.after(date)
      else
        true
      end
    end

    def flag_as_selected
      @selected=true
      @hidden=false
    end

    def flag_as_selected_context
      # do not force if selected has already been set to true
      @selected=false if @selected.nil?
      @hidden=false
    end

    def flag_as_hidden
      # do not force if it has already been flagged as visible
      if @hidden.nil?
        @hidden=true
        @selected=false
      end
    end

    def hidden?
      @hidden==true
    end

    def selected?
      # selected if the @selected has not been set or has been set to true
      !hidden? && @selected!=false
    end

    def deprecated_conditions_label=(label)
      if label
        @deprecated_conditions_label=label
          if label=='0%'
            @conditions=2
            @covered_conditions=0
          elsif label=='100%'
            @conditions=2
            @covered_conditions=2
          else
            @conditions=2
            @covered_conditions=1
        end
      end
    end
  end
end