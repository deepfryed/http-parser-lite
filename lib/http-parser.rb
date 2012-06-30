require 'http-parser/http_parser'

module HTTP
  class Parser
    TYPE_REQUEST  = 0
    TYPE_RESPONSE = 1
    TYPE_BOTH     = 2
    CALLBACKS     = %w(on_url on_header_field on_header_value on_headers_complete on_body on_message_begin on_message_complete)

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
