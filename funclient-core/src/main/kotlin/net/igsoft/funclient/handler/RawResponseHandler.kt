package net.igsoft.funclient.handler

import net.igsoft.funclient.Serializer
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response

class RawResponseHandler : ResponseHandler<Response> {
    override fun handle(request: Request, response: Response, serializer: Serializer): Response {
        return response
    }
}
