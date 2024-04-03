module HTTP
  class Parser
    VERSION = '1.0.1'.freeze

    TYPE_REQUEST  = 0
    TYPE_RESPONSE = 1
    TYPE_BOTH     = 2
    CALLBACKS     = %w(
      on_body
      on_header_field
      on_header_value
      on_headers_complete
      on_message_begin
      on_message_complete
      on_status_complete
      on_url
    )

    CALLBACKS.each do |name|
      define_method(name) do |&block|
        raise ArgumentError, "block expected" unless block
        @callbacks[name.to_sym] = block
      end
    end

    attr_reader :type

    def initialize type = TYPE_BOTH
      @callbacks = {}
      reset(type)
    end

    def reset value = nil
      if value
        raise ArgumentError, "Invalid parser type #{value}" unless [0, 1, 2].include?(value)
        @type = value
      end

      reset!(@type)
    end
  end
end

if RUBY_PLATFORM =~ /java/
  require_relative '../ext/vendor/http-parser.jar'
  require 'http-parser.jar'
  require 'ruby_http_parser'
else
  begin
    require_relative 'http_parser'
  rescue LoadError
    require 'http-parser/http_parser'
  end
end
