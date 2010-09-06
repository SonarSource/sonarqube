atom_feed do |feed|
  feed.title("#{@category ? @category : 'All events'} on project : #{@project.fullname}")
  feed.updated(@date)

  for event in @events
    event_url = "#{ApplicationController.root_context}/project/index/#{event.resource.key}"
    feed.entry(event, :url => event_url, :published => event.event_date) do |entry|
      entry.title(event.resource.fullname)
      if (event.description.nil?)
        entry.content(event.fullname, :type => 'html')
      else
        entry.content("#{event.fullname} : #{event.description}", :type => 'html')
      end
    end
  end
end