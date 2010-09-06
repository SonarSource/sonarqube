 #
 # Sonar, entreprise quality control tool.
 # Copyright (C) 2009 SonarSource SA
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
 class FilterResult
  attr_accessor :page_size, :page_id, :security_exclusions, :filter

  def initialize(filter, options={})
    @filter = filter
    @page_size=options[:page_size] || @filter.page_size
    @page_id=(options[:page_id] ? options[:page_id].to_i : 1)
    @sids=options[:snapshot_ids]
    @page_sids=[]
    @security_exclusions=options[:security_exclusions]
    @metric_ids=(options[:metric_ids] || @filter.columns.map{|col| col.metric ? col.metric.id : nil}.compact.uniq)

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
          measures=ProjectMeasure.find(:all, :conditions => ['rule_priority is null and rules_category_id is null and rule_id is null and snapshot_id in (?)', @page_sids])

          if filter.display_user_managed_metrics?
            measures.concat(AsyncMeasureSnapshot.search(@page_sids, @metric_ids))
          end

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
  end

  def size
    @sids.size
  end

  def empty?
    @page_sids.empty?
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

 end