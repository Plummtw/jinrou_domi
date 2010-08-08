package org.plummtw.jinrou_domi.comet

import _root_.net.liftweb._
import http._
import common._
import actor._
import util._
import Helpers._
import _root_.scala.xml.{NodeSeq, Text}
import org.plummtw.jinrou_domi.model.{JinrouUser_System, JinrouUser}
//import textile.TextileParser
import _root_.java.util.Date

/**
 * A chat server.  It gets messages and returns them
 */

class JinrouChat(val message: String, val css_class: String) {
  def toHtml(): NodeSeq = <span class={css_class}>{message}</span>
}

object ChatServer extends LiftActor with ListenerManager {
  private var chats: List[ChatLine] = List(ChatLine(JinrouUser_System, new JinrouChat("歡迎", ""), now))

  override def lowPriority = {
    case ChatServerMsg(user, chat) if chat.message.length > 0 =>
      chats ::= ChatLine(user, chat, timeNow)
      chats = chats.take(50)
      updateListeners()

    case _ =>
  }

  def createUpdate = ChatServerUpdate(chats.take(50))

  /**
   * Convert an incoming string into XHTML using Textile Markup
   *
   * @param msg the incoming string
   *
   * @return textile markup for the incoming string
   */
  //def toHtml(msg: String): NodeSeq = TextileParser.paraFixer(TextileParser.toHtml(msg, Empty))
}

case class ChatLine(user: JinrouUser, chat: JinrouChat, when: Date)
case class ChatServerMsg(user: JinrouUser, chat: JinrouChat)
case class ChatServerUpdate(msgs: List[ChatLine])

