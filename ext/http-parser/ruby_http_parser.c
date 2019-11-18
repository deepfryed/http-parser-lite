// vim:ts=4:sts=4:sw=4:expandtab
// (c) Bharanee Rathna 2012

#include <ruby/ruby.h>
#include "http_parser.h"

static VALUE mHTTP, cParser, eParserError;

static void rb_parser_free(http_parser *parser) {
    if (parser)
        free(parser);
}

static VALUE rb_parser_allocate(VALUE klass) {
    http_parser *parser = (http_parser *)malloc(sizeof(http_parser));
    http_parser_init(parser, HTTP_BOTH);
    return (VALUE)(parser->data = (void*)Data_Wrap_Struct(klass, 0, rb_parser_free, parser));
}

static http_parser* rb_http_parser_handle(VALUE self) {
    http_parser *parser = 0;
    Data_Get_Struct(self, http_parser, parser);
    if (!parser)
        rb_raise(rb_eArgError, "Invalid HTTP::Parser instance");
    return parser;
}

static void rb_parser_callback_call(VALUE self, const char *name, const char *data, size_t length) {
    VALUE callback = rb_hash_aref(rb_iv_get(self, "@callbacks"), ID2SYM(rb_intern(name)));
    if (!NIL_P(callback)) {
        VALUE args = rb_ary_new();
        if (data)
            rb_ary_push(args, rb_str_new(data, length));
        rb_proc_call(callback, args);
    }
}

static int rb_parser_on_url(http_parser *parser, const char *data, size_t length) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_url", data, length);
    return 0;
}

static int rb_parser_on_status(http_parser *parser, const char *data, size_t length) {
    VALUE self = (VALUE)parser->data;
    // backwards compatibility with v0.6.0
    rb_parser_callback_call(self, "on_status_complete", 0, 0);
    rb_parser_callback_call(self, "on_status", data, length);
    return 0;
}

static int rb_parser_on_header_field(http_parser *parser, const char *data, size_t length) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_header_field", data, length);
    return 0;
}

static int rb_parser_on_header_value(http_parser *parser, const char *data, size_t length) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_header_value", data, length);
    return 0;
}

static int rb_parser_on_headers_complete(http_parser *parser) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_headers_complete", 0, 0);
    return 0;
}

static int rb_parser_on_body(http_parser *parser, const char *data, size_t length) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_body", data, length);
    return 0;
}

static int rb_parser_on_message_begin(http_parser *parser) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_message_begin", 0, 0);
    return 0;
}

static int rb_parser_on_message_complete(http_parser *parser) {
    VALUE self = (VALUE)parser->data;
    rb_parser_callback_call(self, "on_message_complete", 0, 0);
    return 0;
}

static VALUE rb_parser_parse(VALUE self, VALUE data) {
    http_parser *parser = rb_http_parser_handle(self);
    http_parser_settings settings = {
        .on_url              = (http_data_cb)rb_parser_on_url,
        .on_status           = (http_data_cb)rb_parser_on_status,
        .on_header_field     = (http_data_cb)rb_parser_on_header_field,
        .on_header_value     = (http_data_cb)rb_parser_on_header_value,
        .on_headers_complete = (http_cb)rb_parser_on_headers_complete,
        .on_body             = (http_data_cb)rb_parser_on_body,
        .on_message_begin    = (http_cb)rb_parser_on_message_begin,
        .on_message_complete = (http_cb)rb_parser_on_message_complete
    };

    size_t parsed = http_parser_execute(parser, &settings, RSTRING_PTR(data), RSTRING_LEN(data));
    if (parsed != (size_t)RSTRING_LEN(data))
        rb_raise(eParserError, "Error Parsing data: %s", http_errno_description(HTTP_PARSER_ERRNO(parser)));
    return Qtrue;
}

static VALUE rb_parser_reset_bang(VALUE self, VALUE type) {
    http_parser *parser = rb_http_parser_handle(self);
    http_parser_init(parser, FIX2INT(type));
    return Qtrue;
}

static VALUE rb_parser_pause(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    http_parser_pause(parser, 1);
    return Qtrue;
}

static VALUE rb_parser_resume(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    http_parser_pause(parser, 0);
    return Qtrue;
}

static VALUE rb_parser_is_paused(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    return HTTP_PARSER_ERRNO(parser) == HPE_PAUSED ? Qtrue : Qfalse;
}

static VALUE rb_parser_http_method(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    return rb_str_new2(http_method_str(parser->method));
}

static VALUE rb_parser_http_version(VALUE self) {
    char version[16];
    http_parser *parser = rb_http_parser_handle(self);
    snprintf(version, 16, "%d.%d", parser->http_major, parser->http_minor);
    return rb_str_new2(version);
}

static VALUE rb_parser_http_status(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    return INT2NUM(parser->status_code);
}

static VALUE rb_parser_error_q(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    return HTTP_PARSER_ERRNO(parser) != HPE_OK ? Qtrue : Qfalse;
}

static VALUE rb_parser_error(VALUE self) {
    http_parser *parser = rb_http_parser_handle(self);
    int errno = HTTP_PARSER_ERRNO(parser);
    return errno != HPE_OK ? rb_str_new2(http_errno_description(errno)) : Qnil;
}

void Init_http_parser() {
    mHTTP        = rb_define_module("HTTP");
    cParser      = rb_define_class_under(mHTTP,   "Parser", rb_cObject);
    eParserError = rb_define_class_under(cParser, "Error",  rb_eStandardError);

    rb_define_alloc_func(cParser, rb_parser_allocate);

    rb_define_method(cParser, "<<",           rb_parser_parse,        1);
    rb_define_method(cParser, "parse",        rb_parser_parse,        1);
    rb_define_method(cParser, "pause",        rb_parser_pause,        0);
    rb_define_method(cParser, "resume",       rb_parser_resume,       0);
    rb_define_method(cParser, "paused?",      rb_parser_is_paused,    0);
    rb_define_method(cParser, "error?",       rb_parser_error_q,      0);
    rb_define_method(cParser, "error",        rb_parser_error,        0);
    rb_define_method(cParser, "http_method",  rb_parser_http_method,  0);
    rb_define_method(cParser, "http_version", rb_parser_http_version, 0);
    rb_define_method(cParser, "http_status",  rb_parser_http_status,  0);

    rb_define_private_method(cParser, "reset!", rb_parser_reset_bang, 1);
}
