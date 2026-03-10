package dev.haomin.filesheep.common.web

import jakarta.servlet.http.HttpServletRequest

/**
 * Represents metadata about an incoming HTTP request, encapsulating key details such as
 * the client's IP address, user-agent information, and a unique request ID.
 *
 * @property ipAddress The IP address of the client that made the request. This is extracted
 *   from the `X-Forwarded-For` header if present; otherwise, it falls back to the remote
 *   address of the request.
 * @property userAgent The User-Agent string of the client, typically identifying the browser
 *   or application making the request. This is derived from the `User-Agent` header.
 * @property requestId A unique identifier for the request. This can be useful for tracing
 *   and debugging request lifecycles.
 */
data class RequestContext(
    val ipAddress: String?,
    val userAgent: String?,
    val requestId: String?,
) {
    companion object {
        const val X_FORWARDED_FOR = "X-Forwarded-For"
        const val USER_AGENT = "User-Agent"

        fun from(request: HttpServletRequest): RequestContext =
            RequestContext(
                ipAddress = request.getHeader(X_FORWARDED_FOR) ?: request.remoteAddr,
                userAgent = request.getHeader(USER_AGENT),
                requestId = request.requestId
            )
    }
}
