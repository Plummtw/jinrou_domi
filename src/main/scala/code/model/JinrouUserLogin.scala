package org.plummtw.jinrou_domi.model

import _root_.net.liftweb.mapper._
import _root_.net.liftweb.util._
import org.plummtw.jinrou_domi.enum.JinrouUserLoginEnum
import org.plummtw.jinrou_domi.util.JinrouUtil
import net.liftweb.http.{SHtml, S, SessionVar}
import xml.NodeSeq
import net.liftweb.common.{Empty, Box, Full}
//import _root_.net.liftweb.common._
import scala.util.matching._
import scala.collection.immutable.HashMap

class JinrouUserLogin extends LongKeyedMapper[JinrouUserLogin] with IdPK {
  def getSingleton = JinrouUserLogin

  object jinrouuser_id  extends MappedLongForeignKey(this, JinrouUser)
  object uname         extends MappedString(this,20)

  object login_type extends MappedString(this,1)

  object created_ip  extends MappedString(this,20) {
    override def defaultValue = JinrouUtil.getIpAddress()
  }


  object created       extends MappedDateTime(this) {
    override def defaultValue = new java.util.Date()
  }
}

object JinrouUserLogin extends JinrouUserLogin with LongKeyedMetaMapper[JinrouUserLogin] {
  override def fieldOrder = List(id, jinrouuser_id, uname, login_type,
                                   created_ip, created)

  def create_record(jinrouuser_id : Long, uname : String, login_type: JinrouUserLoginEnum.Value) {
    val login_record = JinrouUserLogin.create.jinrouuser_id(jinrouuser_id)
                                       .uname(uname).login_type(login_type).save
  }
}