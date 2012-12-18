package controllers

import akka.dispatch.Future
import domain._
import play.api._
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.format.Formatter
import play.api.libs.concurrent._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Akka._
import play.api.libs.concurrent.Promise
import play.api.mvc._
import play.api.data.FormError
import views.html.defaultpages.badRequest

class NotFoundException extends RuntimeException

object Application extends Controller {

  lazy val invoices = Global.services.invoiceService

  val invoiceForm = Form("id" -> of[String])

  val itemForm = Form(
    mapping(
      "description" -> nonEmptyText,
      "count" -> number(min = 1),
      "amount" -> of[BigDecimal])(InvoiceItem.apply)(InvoiceItem.unapply))

  val sendForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "street" -> nonEmptyText,
      "city" -> nonEmptyText,
      "country" -> nonEmptyText)(InvoiceAddress.apply)(InvoiceAddress.unapply))

  def error = Action {
    Ok(views.html.error())
  }

  def list = Action {
    Ok(views.html.list(Global.services.invoiceService.getInvoices, invoiceForm))
  }

  def addInvoice = Action { implicit request =>
    Async {
      val form = invoiceForm.bindFromRequest
      form.fold(
        errors => Akka.future { BadRequest(views.html.list(invoices.getInvoices, errors)) },
        id => {
          val invoice = invoices.createInvoice(id)
          invoice.mapTo[DomainValidation[DraftInvoice]].asPromise map {
            _ fold (
              errors => { BadRequest(views.html.list(invoices.getInvoices, form, errors)) },
              ok => { Redirect(routes.Application.list) })
          }
        })
    }
  }

  def detail(id: String) = Action {
    invoices.getInvoice(id) match {
      case None => throw new NotFoundException
      case Some(invoice) => Ok(views.html.detail(invoice, itemForm, sendForm))
    }
  }

  def addItem(id: String) = Action { implicit request =>
    Async {
      itemForm.bindFromRequest.fold(
        errors => { Akka.future { BadRequest(views.html.detail(getInvoice(id), errors, sendForm)) } },
        item => {
          val future = invoices.addInvoiceItem(id, None, item)
          future.mapTo[DomainValidation[DraftInvoice]].asPromise map {
            _ fold (
              error => { BadRequest(views.html.error()) },
              invoice => { Redirect(routes.Application.detail(id)) })
          }
        })
    }
  }

  def sendInvoice(id: String) = Action { implicit request =>
    Async {
      val form = sendForm.bindFromRequest()
      form.fold(
        error => Akka.future { BadRequest(views.html.detail(getInvoice(id), itemForm, error)) },
        address => {
          val future = invoices.sendInvoiceTo(id, invoices.getInvoice(id) map (_.version), address)
          future.mapTo[DomainValidation[SentInvoice]].asPromise map {
            _ fold (
              errors => { BadRequest(views.html.detail(getInvoice(id), itemForm, form, errors)) },
              invoice => { Redirect(routes.Application.detail(id)) })
          }
        })
    }
  }
  
  def stats() = Action {
    Ok(views.html.stats(invoices.getInvoices))
  }

  implicit def bigDecimalFormat: Formatter[BigDecimal] = new Formatter[BigDecimal] {

    override val format = Some("format.numeric", Nil)

    def bind(key: String, data: Map[String, String]) = {
      stringFormat.bind(key, data).right.flatMap { s =>
        scala.util.control.Exception.allCatch[BigDecimal]
          .either(BigDecimal(s))
          .left.map(e => Seq(FormError(key, "error.number", Nil)))
      }
    }

    def unbind(key: String, value: BigDecimal) = Map(key -> value.toString)
  }
  
  def getInvoice(id: String) = invoices.getInvoice(id).getOrElse(throw new NotFoundException)
}