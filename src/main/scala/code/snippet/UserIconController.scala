package org.plummtw.jinrou_domi.snippet

import scala.xml._
import net.liftweb._
import common.{Empty, Box, Full}
import net.liftweb.mapper._
import http._
import js._
import util._
import S._
import SHtml._
import Helpers._
import view._
import org.plummtw.jinrou_domi.model._

class UserIconController extends DispatchSnippet with UserIconHelpers {
  override def dispatch = {
    case "all" => all _
    case "top" => top _
    case "paginate" => paginator.paginate _
  }

  val paginator = new MapperPaginatorSnippet(UserIcon){
    override def itemsPerPage = 25
    constantParams = OrderBy(UserIcon.id, Ascending) :: Nil

    override def currentXml: NodeSeq = Text("顯示："+(first+1)+"-"+(first+itemsPerPage min count)+"　總數： "+count)
  }

  def all(xhtml: NodeSeq): NodeSeq = many(paginator.page, xhtml)
  
  def top(xhtml: NodeSeq) = many(UserIcon.findAll(MaxRows(25),
    OrderBy(UserIcon.id, Ascending)), xhtml)
}

trait UserIconHelpers {
  protected def single(usericon: UserIcon, xhtml: NodeSeq): NodeSeq =
    bind("icon", xhtml,
        "id"   -> <span>{usericon.id.is}</span>,
        "name" -> <span>{usericon.icon_name.is}</span>,
        "color" -> <span><font color={usericon.color.is}>◆</font>{usericon.color.is}</span>,
        "img" -> <img src={usericon.icon_filename.is} width={usericon.icon_width.is.toString} height= {usericon.icon_height.is.toString} />)

  protected def group(usericons: List[UserIcon], xhtml: NodeSeq): NodeSeq =
    bind("icons_td", xhtml, "icon" -> usericons.flatMap(t => single(t, chooseTemplate("icons_td","icon", xhtml))))

  protected def many(usericons: List[UserIcon], xhtml: NodeSeq): NodeSeq =
    usericons.grouped(5).toList.flatMap(a => group(a, chooseTemplate("icons_tr","icons_td", xhtml)))

}

