{
  :'nl' => {
    :date => {
      :formats => {
        :default      => "%d/%m/%Y",
        :short        => "%e %b",
        :long         => "%e %B %Y",
        :long_ordinal => lambda { |date| "#{date.day}e van %B %Y" },
        :only_day     => "%e"
      },
      :day_names => %w(Zondag Maandag Dinsdag Woensdag Donderdag Vrijdag Zaterdag),
      :abbr_day_names => %w(Zo Ma Di Wo Do Vr Za),
      :month_names => [nil] + %w(Januari Februari Maart April Mei Juni Juli Augustus September Oktober November December),
      :abbr_month_names => [nil] + %w(Jan Feb Mrt Apr Mei Jun Jul Aug Sep Okt Nov Dec),
      :order => [:day, :month, :year]
    },
    :time => {
      :formats => {
        :default      => "%a %b %d %H:%M:%S %Z %Y",
        :time         => "%H:%M",
        :short        => "%d %b %H:%M",
        :long         => "%d %B %Y %H:%M",
        :long_ordinal => lambda { |time| "#{time.day}e van %B %Y %H:%M" },
        :only_second  => "%S"
      },
      :datetime => {
        :formats => {
          :default => "%Y-%m-%dT%H:%M:%S%Z"
        }
      },
      :time_with_zone => {
        :formats => {
          :default => lambda { |time| "%Y-%m-%d %H:%M:%S #{time.formatted_offset(false, 'UTC')}" }
        }
      },
      :am => %q('s ochtends),
      :pm => %q('s middags)
    },
    :datetime => {
      :distance_in_words => {
        :half_a_minute       => 'een halve minuut',
        :less_than_x_seconds => {:zero => 'minder dan een seconde', :one => 'minder dan 1 seconde', :other => 'minder dan {{count}} secondes'},
        :x_seconds           => {:one => '1 seconde', :other => '{{count}} secondes'},
        :less_than_x_minutes => {:zero => 'minder dan een minuut', :one => 'minder dan 1 minuut', :other => 'minder dan {{count}} minuten'},
        :x_minutes           => {:one => "1 minuut", :other => "{{count}} minuten"},
        :about_x_hours       => {:one => 'ongeveer 1 uur', :other => 'ongeveer {{count}} uren'},
        :x_days              => {:one => '1 dag', :other => '{{count}} dagen'},
        :about_x_months      => {:one => 'ongeveer 1 maand', :other => 'ongeveer {{count}} maanden'},
        :x_months            => {:one => '1 maand', :other => '{{count}} maanden'},
        :about_x_years       => {:one => 'ongeveer 1 jaar', :other => 'ongeveer {{count}} jaren'},
        :over_x_years        => {:one => 'langer dan 1 jaar', :other => 'langer dan {{count}} jaren'}
      }
    },
    :number => {
      :format => {
        :precision => 2,
        :separator => ',',
        :delimiter => '.'
      },
      :currency => {
        :format => {
          :unit => '€',
          :precision => 2,
          :format => '%u %n'
        }
      }
    },

    # Active Record
    :activerecord => {
      :errors => {
        :template => {
          :header => {
            :one => "Kon dit {{model}} object niet opslaan: 1 fout.", 
            :other => "Kon dit {{model}} niet opslaan: {{count}} fouten."
          },
          :body => "Controleer alstublieft de volgende velden:"
        },
        :messages => {
          :inclusion => "is niet in de lijst opgenomen",
          :exclusion => "is niet beschikbaar",
          :invalid => "is ongeldig",
          :confirmation => "komt niet met z'n bevestiging overeen",
          :accepted  => "moet worden geaccepteerd",
          :empty => "moet opgegeven zijn",
          :blank => "moet opgegeven zijn",
          :too_long => "is te lang (niet meer dan {{count}} karakters)",
          :too_short => "is te kort (niet minder dan {{count}} karakters)",
          :wrong_length => "heeft niet de juiste lengte (moet precies {{count}} karakters zijn)",
          :taken => "is niet beschikbaar",
          :not_a_number => "is niet een getal",
          :greater_than => "moet groter zijn dan {{count}}",
          :greater_than_or_equal_to => "moet groter of gelijk zijn aan {{count}}",
          :equal_to => "moet gelijk zijn aan {{count}}",
          :less_than => "moet minder zijn dan {{count}}",
          :less_than_or_equal_to => "moet minder of gelijk zijn aan {{count}}",
          :odd => "moet oneven zijn",
          :even => "moet even zijn"
        }
      }
    }
  }
}
