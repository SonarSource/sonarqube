#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2013 SonarSource
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
module SourceHelper

  #
  # Options :
  # - display_scm : boolean (default is true)
  # - display_violations: boolean (default is false)
  # - display_issues: boolean (default is false)
  # - display_coverage: boolean (default is false)
  # - expand: boolean (default is false). Only if display_violations or display_coverage is true
  # - min_date
  # - line_range : range (default is complete lines range)
  # - highlighted_lines : range (default is complete lines range)
  #
  def snapshot_html_source(snapshot, options={})

    panel = get_html_source_panel(snapshot, options)

    if panel
      panel.filter_min_date(options[:min_date]) if options[:min_date]

      unless panel.empty?

        render :partial => "shared/source_display", :locals => {:display_manual_violation_form => false, \
                                                               :scm_available => options[:display_scm], \
                                                               :display_coverage => options[:display_coverage], \
                                                               :lines => panel.html_lines, \
                                                               :expanded => options[:expand], \
                                                               :display_violations => options[:display_violations], \
                                                               :display_issues => options[:display_issues], \
                                                               :resource => nil, \
                                                               :snapshot => nil, \
                                                               :review_screens_by_vid => false, \
                                                               :filtered => panel.filtered}

      end
    else
      ''
    end
  end

  def get_html_source_panel(snapshot, options={})

    if snapshot && snapshot.has_source

      panel=SourcePanel.new
      revisions_by_line={}
      authors_by_line={}
      dates_by_line={}
      if options[:display_scm]
        panel.display_scm=(snapshot.measure('last_commit_datetimes_by_line')!=nil)
        authors_by_line=load_distribution(snapshot, 'authors_by_line')
        revisions_by_line=load_distribution(snapshot, 'revisions_by_line')
        dates_by_line=load_distribution(snapshot, 'last_commit_datetimes_by_line')
      end

      panel.html_lines=[]
      html_source_lines = snapshot.highlighted_source_lines || snapshot.source.syntax_highlighted_lines()
      line_range=sanitize_range(options[:line_range], 1..html_source_lines.length)

      html_source_lines.each_with_index do |source, index|
        if line_range.include?(index+1)
          html_line=HtmlLine.new(source, index+1)
          html_line.revision=revisions_by_line[index+1]
          html_line.author=authors_by_line[index+1]
          if options[:highlighted_lines] && options[:highlighted_lines].include?(index+1)
            html_line.set_focus
          end
          date_string=dates_by_line[index+1]
          html_line.datetime=(date_string ? Java::OrgSonarApiUtils::DateUtils.parseDateTime(date_string) : nil)
          panel.html_lines<<html_line
        end
      end
      panel
    end
  end

  def load_distribution(snapshot, metric_key)
    m=snapshot.measure(metric_key)
    m ? m.data_as_line_distribution() : {}
  end

  def sanitize_range(range, default_range)
    if range
      ([range.min, 1].max)..(range.max)
    elsif default_range
      default_range
    else
      0..-1
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
    attr_accessor :id, :index, :source, :revision, :author, :datetime, :violations, :issues, :hits, :conditions,
                  :covered_conditions, :hidden, :displayed, :deprecated_conditions_label, :covered_lines, :has_focus

    def initialize(source, id)
      @source=source
      @id=id
    end

    def add_issue(issue)
      @issues||=[]
      @issues<<issue
      @visible=true
    end

    def issues?
      @issues && @issues.size>0
    end

    def issue_severity
      if @issues && @issues.size>0
        @issues[0].severity
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

    def flag_as_displayed
      @displayed=true
      @hidden=false
    end

    def flag_as_displayed_context
      # do not force if displayed has already been set to true
      @displayed=false if @displayed.nil?
      @hidden=false
    end

    def flag_as_hidden
      # do not force if it has already been flagged as visible
      if @hidden.nil?
        @hidden=true
        @displayed=false
      end
    end

    def hidden?
      @hidden==true
    end

    def displayed?
      # displayed if the @displayed has not been set or has been set to true
      !hidden? && @displayed!=false
    end

    def set_focus
      @has_focus=true
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