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
 # License along with {library}; if not, write to the Free Software
 # Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 #
 class FilterContext
  attr_accessor :filter, :page_size, :page_id, :security_exclusions, :period_index, :sorted_column_id, :ascending_sort

  def initialize(filter, options={})
    @filter = filter
    @page_size=options[:page_size] || @filter.page_size
    @page_id=(options[:page_id] ? options[:page_id].to_i : 1)
    @sorted_column_id=(options[:sort].blank? ? nil : options[:sort].to_i)
    @ascending_sort=(options[:asc].blank? ? nil : options[:asc]=='true')
    @period_index = (options[:period] ? options[:period].to_i : @filter.period_index )
    @metric_ids=(options[:metric_ids] || @filter.columns.map{|col| col.metric ? col.metric.id : nil}.compact.uniq)
  end

  def process_results(snapshot_ids, security_exclusions)
    @sids=snapshot_ids
    @security_exclusions=security_exclusions
    
    from=(@page_id-1) * @page_size
    to=(@page_id*@page_size)-1
    to=@sids.size-1 if to>=@sids.size

    @measures_by_snapshot={}
    @page_snapshots=[]
    @snapshots_by_id={}
    @links_by_pid={}
    if from<@sids.size
      #
      # load snapshots and resources
      #
      @page_sids=@sids[from..to]
      @page_snapshots=Snapshot.find(:all, :include => ['project'], :conditions => ['id in (?)', @page_sids])
      @page_snapshots.each{|s| @snapshots_by_id[s.id]=s}

      if @page_sids.size>0
        #
        # load measures
        #
        if @metric_ids.size>0
          measures=ProjectMeasure.find(:all, :conditions => ['rule_priority is null and rule_id is null and characteristic_id is null and person_id is null and snapshot_id in (?)', @page_sids])

          measures.each do |m|
            snapshot=@snapshots_by_id[m.snapshot_id]
            @measures_by_snapshot[snapshot]||={}
            @measures_by_snapshot[snapshot][m.metric]=m
          end
        end

        #
        # load links
        #
        if @filter.display_links?
          pids=@page_snapshots.map{|snapshot| snapshot.project_id}
          ProjectLink.find(:all, :conditions => {:project_id => pids}, :order => 'link_type').each do |link|
            @links_by_pid[link.project_id] ||= []
            @links_by_pid[link.project_id]<<link
          end
        end
      end
    end
    self
  end


  def size
    @sids.size
  end

  def empty?
    @page_sids.nil? || @page_sids.empty?
  end

  def page_count
    result=@sids.size / @page_size
    result+=1 if (@sids.size % @page_size > 0)
    result
  end

  def page_sorted_snapshot_ids
    @page_sids
  end

  def snapshots
    @page_snapshots
  end

  def snapshot(sid)
    @snapshots_by_id[sid]
  end

  def measure(snapshot, metric)
    if metric
      hash=@measures_by_snapshot[snapshot]
      hash ? hash[metric] : nil
    else
      nil
    end
  end

  def measures_by_snapshot
    @measures_by_snapshot
  end

  def links(resource_id)
    @links_by_pid[resource_id] || []
  end

  def security_exclusions?
    @security_exclusions==true
  end

  def selected_period?
    @period_index && @period_index>0
  end



  private

  def extract_snapshot_ids(sql_rows)
    sids=[]
    project_ids=sql_rows.map{|r| r[2] ? to_integer(r[2]) : to_integer(r[1])}.compact.uniq
    authorized_pids=select_authorized(:user, project_ids)
    sql_rows.each do |row|
      pid=(row[2] ? to_integer(row[2]) : to_integer(row[1]))
      if authorized_pids.include?(pid)
        sids<<to_integer(row[0])
      end
    end
    sids
  end

  def to_integer(obj)
    if obj.is_a?(Fixnum)
      obj
    else
      # java.math.BigDecimal
      obj.intValue()
    end
  end
 end