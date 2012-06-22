require 'helper'

describe 'http-parser' do
  before do
    @parser = HTTP::Parser.new
  end

  it 'should parse GET' do
    got = []
    @parser.on_url          {|data| got << data}
    @parser.on_header_field {|data| got << data}
    @parser.on_header_value {|data| got << data}

    @parser << "GET http://www.google.com HTTP/1.1\r\nX-Test-Key: 1\r\n\r\n"
    assert_equal %w(http://www.google.com X-Test-Key 1), got
  end
end
