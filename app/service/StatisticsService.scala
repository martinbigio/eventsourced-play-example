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
package service

import scala.concurrent.stm.Ref

import akka.actor.Actor

import org.eligosource.eventsourced.core._
import domain._

class StatisticsService(statisticsRef: Ref[Map[String, Int]]) {
  def statistics = statisticsRef.single.get
}

class StatisticsProcessor(statisticsRef: Ref[Map[String, Int]]) extends Actor {
  def receive = {
    case InvoiceItemAdded(id, _) => statisticsRef.single.transform { statistics =>
      statistics.get(id) match {
        case Some(count) => statistics + (id -> (count + 1))
        case None => statistics + (id -> 1)
      }
    }
  }
}
