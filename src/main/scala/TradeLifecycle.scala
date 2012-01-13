package net.debasishg.domain.trade
package service

import event.{EventLogEntry, EventLog}

import akka.actor.{Actor, FSM}
import akka.util.duration._
import akka.util.Duration

import model.TradeModel
import TradeModel._

class TradeLifecycle(trade: Trade, timeout: Duration, log: Option[EventLog]) 
  extends Actor with FSM[TradeState, Trade] {
  import FSM._

  startWith(Created, trade)

  when(Created) {
    case Event(e@AddValueDate, data) =>
      log.map(_.appendAsync(data.refNo, Created, Some(data), e))
      val trd = addValueDate(data)
      notifyListeners(trd) 
      goto(ValueDateAdded) using trd forMax(timeout)
  }

  when(ValueDateAdded) {
    case Event(StateTimeout, _) =>
      stay

    case Event(e@EnrichTrade, data) =>
      log.map(_.appendAsync(data.refNo, ValueDateAdded, None,  e))
      val trd = enrichTrade(data)
      notifyListeners(trd)
      goto(Enriched) using trd forMax(timeout)
  }

  when(Enriched) {
    case Event(StateTimeout, _) =>
      stay

    case Event(e@SendOutContractNote, data) =>
      log.map(_.appendAsync(data.refNo, Enriched, None,  e))
      sender ! data
      stop
  }

  initialize
}
