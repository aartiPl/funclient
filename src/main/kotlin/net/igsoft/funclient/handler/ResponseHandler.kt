package net.igsoft.funclient.handler

import com.google.gson.Gson
import net.igsoft.funclient.model.Request
import net.igsoft.funclient.model.Response

interface ResponseHandler<T> {
    fun handle(request: Request, response: Response, gson: Gson): T
}
