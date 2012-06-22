require 'helper'

describe 'http-parser' do
  attr_reader :parser

  before do
    @parser = HTTP::Parser.new
  end

  it 'should parse request' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}

    parser << "GET http://www.google.com HTTP/1.1\r\nX-Test-Key: 1\r\n\r\n"
    assert_equal %w(http://www.google.com X-Test-Key 1), got
    assert_equal 'GET', parser.http_method
    assert_equal '1.1', parser.http_version
  end

  it 'should parse response' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}
    parser.on_body         {|data| got << data}
    parser << "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nContent-Length: 5\r\n\r\nhello"

    assert_equal %w(Content-Type text/plain Content-Length 5 hello), got
    assert_equal '1.1', parser.http_version
    assert_equal 200,   parser.http_status
  end

  it 'should parse CONNECT' do
    got = []
    parser.on_url          {|data| got << data}
    parser.on_header_field {|data| got << data}
    parser.on_header_value {|data| got << data}
    parser << "CONNECT 345.example.com:443 HTTP/1.1\r\nUser-Agent: X-Test-UA\r\n\r\n"

    assert_equal %w(345.example.com:443 User-Agent X-Test-UA), got
    assert_equal 'CONNECT', parser.http_method
    assert_equal '1.1',     parser.http_version
  end

  it 'should raise an error on invalid data' do
    assert_raises(HTTP::ParserError) { parser << "GET / foobar\r\n" }
  end
end
