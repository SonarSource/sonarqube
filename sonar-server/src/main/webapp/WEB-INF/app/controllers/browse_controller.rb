#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2011 SonarSource
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
class BrowseController < ApplicationController

  SECTION=Navigation::SECTION_RESOURCE

  def index
    @resource = Project.by_key(params[:id])

    if (@resource && has_role?(:user, @resource))
      @snapshot=@resource.last_snapshot
      render_resource()
    else
      access_denied
    end
  end

  private


  def render_resource
    @source = @snapshot.source
    if @source
      source_lines=Java::OrgSonarServerUi::JRubyFacade.new.colorizeCode(@source.data, @snapshot.project.language).split("\n")
      load_scm()

      @lines=[]
      current_revision=nil
      source_lines.each_with_index do |source, index|
        line=Line.new(source)
        @lines<<line

        line.revision=@revisions_by_line[index+1]
        line.author=@authors_by_line[index+1]
        line.date=@dates_by_line[index+1]

        if line.revision
          if current_revision!=line.revision
            current_revision=line.revision
            line.display_scm=true
          end
        end
      end
     end

    if (params[:tab]=='violations')
      load_violations_tab()
    elsif (params[:tab]=='coverage')
      load_coverage_tab()
    else
      load_source_tab()
    end

    params[:layout]='false'
  end

  def load_scm()
    @scm_available=(@snapshot.measure('last_commit_datetimes_by_line')!=nil)
    @display_scm=(params[:scm]=='true')
    if @display_scm
      @authors_by_line=load_distribution('authors_by_line')
      @revisions_by_line=load_distribution('revisions_by_line')
      @dates_by_line=load_distribution('last_commit_datetimes_by_line')
    else
      @authors_by_line={}
      @revisions_by_line={}
      @dates_by_line={}
    end
  end

  def load_distribution(metric_key)
    m=@snapshot.measure(metric_key)
    m ? m.data_as_line_distribution() : {}
  end

  def load_coverage_tab
    @display_coverage=true
    @hits_by_line=load_distribution('coverage_line_hits_data')
    @conditions_by_line=load_distribution('conditions_by_line')
    @covered_conditions_by_line=load_distribution('covered_conditions_by_line')

    @hits_by_line.each_pair do |line_id,hits|
      line=@lines[line_id-1]
      if line
        line.hits=hits.to_i
        line.conditions=@conditions_by_line[line_id].to_i
        line.covered_conditions=@covered_conditions_by_line[line_id].to_i
      end
    end
  end

  def load_violations_tab
    @display_violations=true
    @global_violations=[]

    conditions='snapshot_id=?'
    values=[@snapshot.id]
    unless params[:rule].blank?
      if params[:rule].include?(':')
        rule=Rule.by_key_or_id(params[:rule])
        conditions += ' AND rule_id=?'
        values<<(rule ? rule.id : -1)
      else
        # severity
        conditions += ' AND failure_level=?'
        values<<params[:rule].to_i
      end
    end

    unless params[:date].blank?
      conditions+=' AND created_at>=?'
      values<<Date::strptime(params[:date])
    end

    RuleFailure.find(:all, :include => 'rule', :conditions => [conditions] + values, :order => 'failure_level DESC').each do |violation|
      # sorted by severity => from blocker to info
      if violation.line && violation.line>0
        @lines[violation.line-1].add_violation(violation)
      else
        @global_violations<<violation
      end
    end
  end

  def load_source_tab

  end

  class Line
    attr_accessor :source, :revision, :author, :date, :display_scm, :violations, :hits, :conditions, :covered_conditions

    def initialize(source)
      @source=source
      @display_scm=false
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
  end
end