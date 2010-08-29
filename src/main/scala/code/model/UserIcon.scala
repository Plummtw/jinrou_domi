package org.plummtw.jinrou_domi.model

import net.liftweb._
import mapper._
import scala.util.matching.Regex
import util.FieldError
//import http._ 
//import SHtml._
//import util._
import java.util.Date

class UserIcon extends LongKeyedMapper[UserIcon] with IdPK {
  def getSingleton = UserIcon

  object jinrouuser_id  extends MappedLongForeignKey(this, JinrouUser)

  object icon_group    extends MappedInt(this)
  object icon_gname    extends MappedString(this,20)
  
  object icon_name     extends MappedString(this,20)
  object icon_filename extends MappedString(this,80)
  object icon_width    extends MappedInt(this)
  object icon_height   extends MappedInt(this)
  object color         extends MappedString(this,7)  {
    override def validations = validPriority _ :: super.validations

    val regex = new Regex("#[0-9A-f]{6}")
    def validPriority(in: String) = in match {
      case regex() => Nil
      case _       => List(FieldError(this, "顏色格式錯誤"))
    }
  }
  
  object created       extends MappedDateTime(this) {
    override def defaultValue = new Date
  }
  object updated       extends MappedDateTime(this) with LifecycleCallbacks {
    override def beforeCreate = this(new Date)
    override def beforeUpdate = this(new Date)
  }
  
}

object UserIcon extends UserIcon with LongKeyedMetaMapper[UserIcon] {
  override def fieldOrder = List(id, jinrouuser_id, icon_group, icon_gname, icon_name, icon_filename,
                                 icon_width, icon_height, color, created, updated)
}

