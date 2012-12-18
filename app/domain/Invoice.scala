/*
 * Copyright 2012 Eligotech BV.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package domain

import scalaz._
import Scalaz._

sealed abstract class Invoice {
  def id: String

  def version: Long
  def versionOption = if (version == -1L) None else Some(version)

  def items: List[InvoiceItem]
  def discount: BigDecimal

  def total: BigDecimal = sum - discount

  def sum: BigDecimal = items.foldLeft(BigDecimal(0)) {
    (sum, item) => sum + item.amount * item.count
  }
}

object Invoice {
  val invalidVersionMessage = "invoice %s: expected version %s doesn't match current version %s"

  def invalidVersion(invoiceId: String, expected: Long, current: Long) =
    DomainError(invalidVersionMessage format (invoiceId, expected, current))

  def requireVersion[T <: Invoice](invoice: T, expectedVersion: Option[Long]): DomainValidation[T] = {
    val id = invoice.id
    val version = invoice.version

    expectedVersion match {
      case Some(expected) if (version != expected) => invalidVersion(id, expected, version).fail
      case Some(expected) if (version == expected) => invoice.success
      case None => invoice.success
    }
  }

  def create(id: String): DomainValidation[DraftInvoice] = DraftInvoice(id, version = 0L).success
}

case class DraftInvoice(id: String, version: Long = -1, items: List[InvoiceItem] = Nil, discount: BigDecimal = 0) extends Invoice {

  def addItem(item: InvoiceItem): DomainValidation[DraftInvoice] =
    copy(version = version + 1, items = items :+ item).success

  def setDiscount(discount: BigDecimal): DomainValidation[DraftInvoice] =
    if (sum <= 100) DomainError("Discount only on orders with sum > 100").fail
    else copy(version = version + 1, discount = discount).success

  def sendTo(address: InvoiceAddress): DomainValidation[SentInvoice] =
    if (items.isEmpty) DomainError("Cannot send empty invoice").fail
    else SentInvoice(id, version + 1, items, discount, address).success
}

case class SentInvoice(id: String, version: Long = -1, items: List[InvoiceItem] = Nil, discount: BigDecimal = 0, address: InvoiceAddress) extends Invoice {

  def pay(amount: BigDecimal): DomainValidation[PaidInvoice] =
    if (amount < total) DomainError("Paid amount less than total amount").fail
    else PaidInvoice(id, version + 1, items, discount, address).success
}

case class PaidInvoice(id: String, version: Long = -1, items: List[InvoiceItem] = Nil,
  discount: BigDecimal = 0, address: InvoiceAddress) extends Invoice {

  def paid = true
}

case class InvoiceItem(description: String, count: Int, amount: BigDecimal)

///**
// * Needed to support conditional updates via XML/JSON Web API,
// */
//case class InvoiceItemVersioned(description: String, count: Int, amount: BigDecimal, invoiceVersion: Long = -1) {
//
//  def toInvoiceItem = InvoiceItem(description, count, amount)
//  def invoiceVersionOption = if (invoiceVersion == -1L) None else Some(invoiceVersion)
//}

case class InvoiceAddress(name: String, street: String, city: String, country: String)

// Events
case class InvoiceCreated(invoiceId: String)
case class InvoiceItemAdded(invoiceId: String, item: InvoiceItem)
case class InvoiceDiscountSet(invoiceId: String, discount: BigDecimal)
case class InvoiceSent(invoiceId: String, invoice: Invoice, to: InvoiceAddress)
case class InvoicePaid(invoiceId: String)

case class InvoicePaymentRequested(invoiceId: String, amount: BigDecimal, to: InvoiceAddress)
case class InvoicePaymentReceived(invoiceId: String, amount: BigDecimal)

// Commands
case class CreateInvoice(invoiceId: String)
case class AddInvoiceItem(invoiceId: String, expectedVersion: Option[Long], invoiceItem: InvoiceItem)
case class SetInvoiceDiscount(invoiceId: String, expectedVersion: Option[Long], discount: BigDecimal)
case class SendInvoiceTo(invoiceId: String, expectedVersion: Option[Long], to: InvoiceAddress)