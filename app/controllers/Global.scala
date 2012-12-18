package controllers

import play.api._
import play.api.mvc.Results._
import service._
import service.AppServices._
import play.api.mvc.RequestHeader
import views.html.defaultpages.notFound

object Global extends GlobalSettings {

  lazy val services: AppServices = boot
  
   override def onError(request: RequestHeader, ex: Throwable) = ex match {
    case e: NotFoundException => NotFound(views.html.error())
    case _ => InternalServerError(views.html.error())
  }  
}