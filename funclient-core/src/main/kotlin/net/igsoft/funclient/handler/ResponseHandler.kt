package net.igsoft.funclient.handler

import net.igsoft.funclient.Serializer
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response

interface ResponseHandler<T> {
    fun handle(request: Request, response: Response, serializer: Serializer): T
}
