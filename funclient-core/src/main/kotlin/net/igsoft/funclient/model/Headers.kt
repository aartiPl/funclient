package net.igsoft.funclient.model

val EmptyHeaders = Headers(emptyMap())

@Suppress("unused")
data class Headers(val all: Map<String, List<String>>) {
    companion object {
        const val ACCEPT: String = "Accept"
        const val ACCEPT_CHARSET: String = "Accept-Charset"
        const val ACCEPT_ENCODING: String = "Accept-Encoding"
        const val ACCEPT_LANGUAGE: String = "Accept-Language"
        const val ACCEPT_RANGES: String = "Accept-Ranges"
        const val AGE: String = "Age"
        const val ALLOW: String = "Allow"

        // Application-Layer Protocol Negotiation, HTTP/2
        const val ALPN: String = "ALPN"
        const val AUTHENTICATION_INFO: String = "Authentication-Info"
        const val AUTHORIZATION: String = "Authorization"
        const val CACHE_CONTROL: String = "Cache-Control"
        const val CONNECTION: String = "Connection"
        const val CONTENT_DISPOSITION: String = "Content-Disposition"
        const val CONTENT_ENCODING: String = "Content-Encoding"
        const val CONTENT_LANGUAGE: String = "Content-Language"
        const val CONTENT_LENGTH: String = "Content-Length"
        const val CONTENT_LOCATION: String = "Content-Location"
        const val CONTENT_RANGE: String = "Content-Range"
        const val CONTENT_TYPE: String = "Content-Type"
        const val COOKIE: String = "Cookie"

        // WebDAV Search
        const val DASL: String = "DASL"
        const val DATE: String = "Date"

        // WebDAV
        const val DAV: String = "DAV"
        const val DEPTH: String = "Depth"

        const val DESTINATION: String = "Destination"
        const val ETAG: String = "ETag"
        const val EXPECT: String = "Expect"
        const val EXPIRES: String = "Expires"
        const val FROM: String = "From"
        const val FORWARDED: String = "Forwarded"
        const val HOST: String = "Host"
        const val HTTP2_SETTINGS: String = "HTTP2-Settings"
        const val IF: String = "If"
        const val IF_MATCH: String = "If-Match"
        const val IF_MODIFIED_SINCE: String = "If-Modified-Since"
        const val IF_NONE_MATCH: String = "If-None-Match"
        const val IF_RANGE: String = "If-Range"
        const val IF_SCHEDULE_TAG_MATCH: String = "If-Schedule-Tag-Match"
        const val IF_UNMODIFIED_SINCE: String = "If-Unmodified-Since"
        const val LAST_MODIFIED: String = "Last-Modified"
        const val LOCATION: String = "Location"
        const val LOCK_TOKEN: String = "Lock-Token"
        const val LINK: String = "Link"
        const val MAX_FORWARDS: String = "Max-Forwards"
        const val MIME_VERSION: String = "MIME-Version"
        const val ORDERING_TYPE: String = "Ordering-Type"
        const val ORIGIN: String = "Origin"
        const val OVERWRITE: String = "Overwrite"
        const val POSITION: String = "Position"
        const val PRAGMA: String = "Pragma"
        const val PREFER: String = "Prefer"
        const val PREFERENCE_APPLIED: String = "Preference-Applied"
        const val PROXY_AUTHENTICATE: String = "Proxy-Authenticate"
        const val PROXY_AUTHENTICATION_INFO: String = "Proxy-Authentication-Info"
        const val PROXY_AUTHORIZATION: String = "Proxy-Authorization"
        const val PUBLIC_KEY_PINS: String = "Public-Key-Pins"
        const val PUBLIC_KEY_PINS_REPORT_ONLY: String = "Public-Key-Pins-Report-Only"
        const val RANGE: String = "Range"
        const val REFERRER: String = "Referer"
        const val RETRY_AFTER: String = "Retry-After"
        const val SCHEDULE_REPLY: String = "Schedule-Reply"
        const val SCHEDULE_TAG: String = "Schedule-Tag"
        const val SEC_WEBSOCKET_ACCEPT: String = "Sec-WebSocket-Accept"
        const val SEC_WEBSOCKET_EXTENSIONS: String = "Sec-WebSocket-Extensions"
        const val SEC_WEBSOCKET_KEY: String = "Sec-WebSocket-Key"
        const val SEC_WEBSOCKET_PROTOCOL: String = "Sec-WebSocket-Protocol"
        const val SEC_WEBSOCKET_VERSION: String = "Sec-WebSocket-Version"
        const val SERVER: String = "Server"
        const val SET_COOKIE: String = "Set-Cookie"

        // Atom Publishing
        const val SLUG: String = "SLUG"
        const val STRICT_TRANSPORT_SECURITY: String = "Strict-Transport-Security"
        const val TE: String = "TE"
        const val TIMEOUT: String = "Timeout"
        const val TRAILER: String = "Trailer"
        const val TRANSFER_ENCODING: String = "Transfer-Encoding"
        const val UPGRADE: String = "Upgrade"
        const val USER_AGENT: String = "User-Agent"
        const val VARY: String = "Vary"
        const val VIA: String = "Via"
        const val WARNING: String = "Warning"
        const val WWW_AUTHENTICATE: String = "WWW-Authenticate"

        // CORS
        const val ACCESS_CONTROL_ALLOW_ORIGIN: String = "Access-Control-Allow-Origin"
        const val ACCESS_CONTROL_ALLOW_METHODS: String = "Access-Control-Allow-Methods"
        const val ACCESS_CONTROL_ALLOW_CREDENTIALS: String = "Access-Control-Allow-Credentials"
        const val ACCESS_CONTROL_ALLOW_HEADERS: String = "Access-Control-Allow-Headers"

        const val ACCESS_CONTROL_REQUEST_METHOD: String = "Access-Control-Request-Method"
        const val ACCESS_CONTROL_REQUEST_HEADERS: String = "Access-Control-Request-Headers"
        const val ACCESS_CONTROL_EXPOSE_HEADERS: String = "Access-Control-Expose-Headers"
        const val ACCESS_CONTROL_MAX_AGE: String = "Access-Control-Max-Age"

        // Unofficial de-facto headers
        const val X_HTTP_METHOD_OVERRIDE: String = "X-Http-Method-Override"
        const val X_FORWARDED_HOST: String = "X-Forwarded-Host"
        const val X_FORWARDED_SERVER: String = "X-Forwarded-Server"
        const val X_FORWARDED_PROTO: String = "X-Forwarded-Proto"
        const val X_FORWARDED_FOR: String = "X-Forwarded-For"

        const val X_FORWARDED_PORT: String = "X-Forwarded-Port"

        const val X_REQUEST_ID: String = "X-Request-ID"
        const val X_CORRELATION_ID: String = "X-Correlation-ID"
        const val X_TOTAL_COUNT: String = "X-Total-Count"

        fun builder() = HeadersBuilder()
    }
}
