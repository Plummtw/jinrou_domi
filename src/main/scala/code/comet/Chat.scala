package org.plummtw.jinrou_domi.comet

import _root_.net.liftweb._
import http._
import common._
import actor._
import util._
import Helpers._
import _root_.scala.xml._
import S._
import SHtml._
import js._
import JsCmds._
import JE._

import org.plummtw.jinrou_domi.model._
import net.liftweb.http.js.jquery.JqJsCmds.PrependHtml
import collection.mutable.HashMap

object ChatCss {
  val chat_map  = HashMap("chat1 red" -> "強勢發言(紅)", "chat1" -> "強勢發言",
                            "chat2" -> "稍強發言", "chat3" -> "普通發言",
                            "chat4" -> "小聲發言", "chat4 blue" -> "小聲發言(藍)")
  val chat_list = List("chat1 red", "chat1", "chat2", "chat3", "chat4", "chat4 blue")
  val chat_seq_pair = chat_list.map(x => (x,chat_map.get(x).getOrElse("")))

  def generateHtml(fn: (String) => Any) = SHtml.select(chat_seq_pair, Full("chat3"), fn)

}

class Chat extends CometActor with CometListener {
  private var userName = ""
  private var chats: List[ChatLine] = Nil
  private lazy val infoId = uniqueId + "_info"
  private lazy val infoIn = uniqueId + "_in"
  private lazy val inputArea  = findKids(defaultXml, "chat", "input")
  private lazy val bodyArea   = findKids(defaultXml, "chat", "body")
  private lazy val singleLine = deepFindKids(bodyArea, "chat", "list")

  case object Tick
  ActorPing.schedule(this, Tick, 1 seconds)
  // handle an update to the chat lists
  // by diffing the lists and then sending a partial update
  // to the browser
  override def lowPriority = {
    case Tick =>
      //reRender(false)
      ActorPing.schedule(this, Tick, 10 seconds)  
    case ChatServerUpdate(value) =>
      val update = (value filterNot (chats contains)).map(b => PrependHtml(infoId, line(b)))
      reRender(false)
      //partialUpdate(update)
      chats = value
  }

  // render the input area by binding the
  // appropriate dynamically generated code to the
  // view supplied by the template
  override lazy val fixedRender: Box[NodeSeq] = {
    var message = ""
    var css_string = ""

    def ajaxSendMessage() : JsCmd = {
      println("1:" + message)
      println("2:" + css_string)
      sendMessage(message, css_string)
      //Thread.sleep(100)
      //reRender(true)
      JsCmds.Noop
    }

    ajaxForm(//After(100, SetValueAndFocus(infoIn, "")),
             bind("chat", inputArea,
                  "input" -> text("", message = _, "id" -> infoIn, "size" -> "40"),
                  "css_string" -> ChatCss.generateHtml(css_string = _),
                  "submit"-> ajaxSubmit("送出", ajaxSendMessage)))

  }

  // send a message to the chat server
  private def sendMessage(message: String, css_string: String) = {
    println("SendMessage : " + message + " " + css_string)
    CurrentJinrouUser.is match {
    case Full(user) => ChatServer ! ChatServerMsg(user, new JinrouChat(message, css_string))
    case _          =>
  }
}


  // display a line
  private def line(c: ChatLine) = bind("list", singleLine,
                                       "when" -> hourFormat(c.when),
                                       "who" -> c.user.handle_name.is,
                                       "message" -> c.chat.toHtml)

  // display a list of chats
  private def displayList(in: NodeSeq): NodeSeq = chats.flatMap(line)

  // render the whole list of chats
  override def render =
    bind("chat", bodyArea,
         // "name" -> userName,
         // "input" -> fixedRender,
         AttrBindParam("id", Text(infoId), "id"),
         "list" -> displayList _)

  // setup the component
  override def localSetup {
    askForName
    super.localSetup
  }

  // register as a listener
  def registerWith = ChatServer

  // ask for the user's name
  private def askForName {
    if (userName.length == 0) {
      CurrentJinrouUser.is match {
        case Full(x) =>
          userName = x.handle_name.is
          reRender(true)

        case _ =>
          reRender(false)
      }
    }
  }

}