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
import java.io.{File, ByteArrayInputStream, FileOutputStream}
import javax.imageio.ImageIO

object UserIconUploadLock extends Object  

class UserIconController extends DispatchSnippet with UserIconHelpers {
  override def dispatch = {
    case "all" => all _
    case "top" => top _
    case "paginate" => paginator.paginate _

    case "addEntry" => addEntry _
  }

  val paginator = new MapperPaginatorSnippet(UserIcon){
    override def itemsPerPage = 25
    constantParams = OrderBy(UserIcon.id, Ascending) :: Nil

    override def currentXml: NodeSeq = Text("顯示："+(first+1)+"-"+(first+itemsPerPage min count)+"　總數： "+count)
  }

  def all(xhtml: NodeSeq): NodeSeq = many(paginator.page, xhtml)
  
  def top(xhtml: NodeSeq) = many(UserIcon.findAll(MaxRows(25),
    OrderBy(UserIcon.id, Ascending)), xhtml)

  def addEntry(xhtml: NodeSeq): NodeSeq = {
    // Add a variable to hold the FileParamHolder on submission
    var fileHolder  : Box[FileParamHolder] = Empty
    var icon_name   : String = ""

    def doTagsAndSubmit () {
      CurrentJinrouUser.is match {
        case Full(user) => ;
        case _          => S.error("尚未登入") ; return
      }

      val receiptOk = fileHolder match {
        // An empty upload gets reported with a null mime type,
        // so we need to handle this special case
        case Full(FileParamHolder(_, null, _, _)) =>
          S.error("上傳格式無法取得"); false
        case Full(FileParamHolder(_, mime, filename, file))
          if mime.startsWith("image/") => {
            val image = ImageIO.read(new ByteArrayInputStream(file))
            println("Width : " + image.getWidth())
            println("Height : " + image.getHeight())
            val color0 = S.param("color0").getOrElse("")
            val color  = (if (S.param("color").getOrElse("on") == "on")
                             color0
                          else S.param("color").getOrElse(""))

            println(new File(".").getAbsolutePath())


            if ((image.getWidth() <= 45) && (image.getHeight() <= 45)) {
              UserIconUploadLock synchronized {
                val usericon = UserIcon.create
                val newname  = "src/main/webapp/upload/" + filename + ".png"
                println("Filename : " + newname)
                println("Color : " + color)
                try {
                  ImageIO.write(image, "png", new FileOutputStream(newname))
                  CurrentJinrouUser.is match {
                    case Full(user) => usericon.jinrouuser_id(user.id.is)
                    case _          => ;
                  }
                  usericon.icon_name(icon_name).icon_group(2)
                          .icon_filename("upload/" + filename + ".png")
                          .icon_width(image.getWidth)
                          .icon_height(image.getHeight)
                          .color(color)

                  // 欄位檢核
                  usericon.validate match {
                    case Nil => usericon.save; S.notice("頭像上傳成功"); true
                    case xs  =>  S.error(xs); false
                  }
                } catch {
                  case e => e.printStackTrace()
                             S.error("檔案存檔失敗")
                  false
                }
              }
            }
            else {
              S.error("上傳長寬不符（長：" + image.getWidth() + "　寬：" + image.getHeight() + "）")
              false
            }
          }
        case Full(_) =>
          S.error("上傳格式不符"); false
        case _ =>
          S.error("無檔案上傳"); false
      }
    }

    bind("icon", xhtml,
      "upload" -> SHtml.fileUpload(x => fileHolder = Full(x), "size"->"60"),
      "name"    -> SHtml.text(icon_name, icon_name = _ , "size"->"20", "maxlength"->"20"),
      "submit"  -> SHtml.submit("上傳", doTagsAndSubmit))

  }
}

trait UserIconHelpers {
  protected def single(usericon: UserIcon, xhtml: NodeSeq): NodeSeq =
    bind("icon", xhtml,
        "id"   -> <span>{usericon.id.is}</span>,
        "name" -> <span>{usericon.icon_name.is}</span>,
        "color" -> <span><font color={usericon.color.is}>◆</font>{usericon.color.is}</span>,
        "img" -> <img src={usericon.icon_filename.is} width={usericon.icon_width.is.toString}
                      height= {usericon.icon_height.is.toString} style={"border-color:" + usericon.color.is +";"} />)

  protected def group(usericons: List[UserIcon], xhtml: NodeSeq): NodeSeq =
    bind("icons_td", xhtml, "icon" -> usericons.flatMap(t => single(t, chooseTemplate("icons_td","icon", xhtml))))

  protected def many(usericons: List[UserIcon], xhtml: NodeSeq): NodeSeq =
    usericons.grouped(5).toList.flatMap(a => group(a, chooseTemplate("icons_tr","icons_td", xhtml)))

}

