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
import org.plummtw.jinrou_domi.view.ChineseCaptchaAnswer
import org.plummtw.jinrou_domi.util.JinrouUtil
import java.text.SimpleDateFormat
import org.plummtw.jinrou_domi.enum.JinrouUserLoginEnum
import org.plummtw.jinrou_domi.model.{JinrouUserLogin, CurrentJinrouUser, JinrouUser}
import java.util.{Date, Calendar}

class JinrouUserController extends DispatchSnippet {
  
  object JinrouUserLock {}
  object JinrouUserRequest extends RequestVar[Box[JinrouUser]](Empty) {}

  val dispatch: DispatchIt = {
    case "status" if CurrentJinrouUser.isDefined =>
      xhtml => status_in(chooseTemplate("choose", "status_in", xhtml))

    case "status" =>
      xhtml => status_out(chooseTemplate("choose", "status_out", xhtml))

    case "manage" if CurrentJinrouUser.isDefined => edit _

    case "manage"  => add _
  }

  def status_in (xhtml : NodeSeq) : NodeSeq = {
    def date_format (in : Date) : String = {
      //val date_format = new SimpleDateFormat("年MM月dd日")
      val calendar = Calendar.getInstance()
      calendar.setTime(in)
      val year  = calendar.get(Calendar.YEAR) - 1911
      val month = calendar.get(Calendar.MONTH) + 1
      val day   = calendar.get(Calendar.DAY_OF_MONTH)

      val builder = new StringBuilder("")
      builder.append(year)
      builder.append("年")
      builder.append(month)
      builder.append("月")
      builder.append(day)
      builder.append("日")

      return builder.toString  
    }
    
    val jinrouuser = CurrentJinrouUser.is match {
      case Full(xs) => xs
      case _        => return redirectTo("main.html")
    }

    def check_logout()  {
      JinrouUserLogin.create_record(jinrouuser.id.is, jinrouuser.uname.is, JinrouUserLoginEnum.LOGOUT)
      CurrentJinrouUser.set(Empty)
    }

    bind("jinrouuser", xhtml,
      "uname"         -> <span>{jinrouuser.uname.is}</span>,
      "handle_name"  -> <span>{jinrouuser.handle_name.is}</span>,
      "last_login"   -> <span>{date_format(jinrouuser.last_login.is)}</span>,
      "logout"        -%> SHtml.submit("登出", check_logout),
      "edit"           -> SHtml.link("main.html", () => redirectTo("jinrouuser.html"), Text("管理"))
    )
  }

  def status_out (xhtml : NodeSeq) : NodeSeq = {
    var uname = ""
    var password = ""
    var trip     = ""

    def check_login()  {
      println("Uname : " + uname)
      println("Passw : " + password)
      println("Trip  : " + trip)

      // 先檢查失敗的紀錄
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.MINUTE, -15)
      val date  = calendar.getTime

      val ip_address = JinrouUtil.getIpAddress()

      val fail_count = JinrouUserLogin.findAll(By(JinrouUserLogin.uname, uname),
                                               By(JinrouUserLogin.login_type, JinrouUserLoginEnum.LOGIN_FAIL),
                                               By_>(JinrouUserLogin.created, date)).length +
                       JinrouUserLogin.findAll(By(JinrouUserLogin.created_ip, ip_address),
                                               By(JinrouUserLogin.login_type, JinrouUserLoginEnum.LOGIN_FAIL),
                                               By_>(JinrouUserLogin.created, date)).length
      if (fail_count >= 5) {
        S.error(<b>登入失敗錯誤超過安全限制，請於15分鐘之後再行登入</b>)
        return redirectTo("main.html")
      }

      // 輸入 ID, Trip, PASSWORD 登入
      println(JinrouUtil.generateSHA1(password.trim()).substring(0,20))
      println(JinrouUtil.generateSHA1_php(trip.trim()).substring(1,9))

      JinrouUser.find(By(JinrouUser.uname, uname),
                      By(JinrouUser.password, JinrouUtil.generateSHA1(password.trim()).substring(0,20)),
                      By(JinrouUser.trip, JinrouUtil.generateSHA1_php(trip.trim()).substring(1,9))) match {
        case Full(xs) => JinrouUserLogin.find(By(JinrouUserLogin.jinrouuser_id, xs.id.is), OrderBy(JinrouUserLogin.created, Descending)) match {
                            case Full(xy) => xs.last_login(xy.created.is)
                                             xs.save
                            case _        => ;
                          }
                          JinrouUserLogin.create_record(xs.id.is, uname, JinrouUserLoginEnum.LOGIN)
                          CurrentJinrouUser.set(Full(xs))
        case _        => JinrouUserLogin.create_record(0, uname, JinrouUserLoginEnum.LOGIN_FAIL)
                          S.error(<b>帳號、密碼或trip錯誤</b>)
      }

      redirectTo("main.html")
    }


    bind("jinrouuser", xhtml,
      "uname"         -> SHtml.text(uname, uname = _, "size"->"10", "maxlength"->"20"),
      "trip"          -> SHtml.text(trip, trip = _ , "size"->"10", "maxlength"->"20"),
      "password"      -> SHtml.password(password, password = _, "size"->"10", "maxlength"->"20"),
      "login"         -%> SHtml.submit("登入", check_login),
      "register"      -> SHtml.link("main.html", () => redirectTo("jinrouuser.html"), Text("註冊"))
    )
  }

  def add (xhtml : NodeSeq) : NodeSeq = {
    // 參數
    var jinrouuser      = JinrouUser.create
    //var uname          = ""
    //var handle_name    = ""
    //var trip           = ""
    //var password       = ""
    var repassword     = ""
    //var email          = ""
    //var sex            = "M"
    var captcha        = ""
    //var user_icon_id   = 1

    def create_user () {
      //println("Captcha : " + ChineseCaptchaAnswer.is)
      //val user_icon_id : Long =
      //  try { user_icon.toLong }
      //  catch { case e: Exception => 2 }

      var is_error = false
      if ( captcha != ChineseCaptchaAnswer.is ) {
        S.error(<b>驗證碼錯誤 答案：{ChineseCaptchaAnswer.is} 你輸入：{captcha} </b>)
        is_error = true
      }


      if ( jinrouuser.password != repassword ) {
        S.error(<b>密碼不一致</b>)
        is_error = true
      }


      // 欄位檢核
      //val trip_value =
      //  if (trip == "") "" else JinrouUtil.generateSHA1_php(trip.trim()).substring(1,9)
      //val jinrouuser = JinrouUser.create.uname(uname.trim()).handle_name(handle_name.replace('　',' ').trim()).sex(sex)
      //                            .trip(trip_value)
      //                            .password(JinrouUtil.generateSHA1(password.trim()).substring(0,20))

      jinrouuser.validate match {
        case Nil => ;
        case xs  => is_error = true
                     S.error(xs)
      }

      jinrouuser.password(JinrouUtil.generateSHA1(jinrouuser.password.is.trim()).substring(0,20))
      if (jinrouuser.trip.is != "")
        jinrouuser.trip(JinrouUtil.generateSHA1_php(jinrouuser.trip.is.trim()).substring(1,9))

      JinrouUserLock.synchronized {
        // 檢查是否 uname 重複
        val uname_count = JinrouUser.count(By(JinrouUser.uname, jinrouuser.uname.is))

        if (uname_count > 0) {
          is_error = true
          S.error(<b>帳號重複</b>)
        }

        if (is_error == true) {
          return redirectTo("jinrouuser.html", () => JinrouUserRequest.set(Full(jinrouuser)))
        }

        jinrouuser.save
        S.notice(<b>註冊成功</b>)
        redirectTo("main.html")
      }

    }

    val jinrouuser_r = JinrouUserRequest.is
    jinrouuser_r match {
      case Full(xs) => jinrouuser.handle_name(xs.handle_name.is)
                       .sex(xs.sex.is)
                       .email(xs.email.is)
                       .msn(xs.msn.is)
                       .zodiac(xs.zodiac.is)
                       .user_memo(xs.user_memo.is)
      case _        => ;
    }

    bind("entry", xhtml,
      "uname"         -> SHtml.text(jinrouuser.uname,          jinrouuser.uname(_), "size"->"40", "maxlength"->"30"),
      "handle_name"  -> SHtml.text(jinrouuser.handle_name,    jinrouuser.handle_name(_), "size"->"40", "maxlength"->"30"),
      "trip"          -> SHtml.text("",                        jinrouuser.trip(_), "size"->"40", "maxlength"->"30"),
      "password"     -> SHtml.password("",                    jinrouuser.password(_), "size"->"40", "maxlength"->"30"),
      "repassword"   -> SHtml.password("",                    repassword = _, "size"->"40", "maxlength"->"30"),
      "sex"           -> jinrouuser.sex.generateHtml,
      "email"         -%> SHtml.text(jinrouuser.email,          jinrouuser.email(_), "size"->"40", "maxlength"->"80"),
      "msn"           -%> SHtml.text(jinrouuser.msn,            jinrouuser.msn(_), "size"->"40", "maxlength"->"80"),
      //"male"          -> sex_radios(0),
      //"female"       -> sex_radios(1),
      "zodiac"        -> jinrouuser.zodiac.generateHtml,
      "memo"          -> SHtml.textarea(jinrouuser.user_memo,  jinrouuser.user_memo(_), "size"->"40", "rows"->"5", "cols"->"70","wrap"->"soft"),
      "captcha"       -> SHtml.text("",                        captcha = _, "size"->"40", "maxlength"->"80"),
      //"user_icon_id"  -> SHtml.textarea(dummy_last_words,  dummy_last_words = _, "rows"->"3","cols"->"70","wrap"->"soft"),
      "submit"        -> SHtml.submit(" 建  立 ",  create_user)
    )
  }

  def edit (xhtml : NodeSeq) : NodeSeq = {
    // 參數
    var jinrouuser : JinrouUser = null
    var old_password    = ""
    var repassword      = ""
    var captcha        = ""
    //var user_icon_id   = 1

    def update_user () {
      var is_error = false
      if ( captcha != ChineseCaptchaAnswer.is ) {
        S.error(<b>驗證碼錯誤 答案：{ChineseCaptchaAnswer.is} 你輸入：{captcha} </b>)
        is_error = true
      }


      if ( jinrouuser.password != repassword ) {
        S.error(<b>密碼不一致</b>)
        is_error = true
      }


      // 欄位檢核
      //val trip_value =
      //  if (trip == "") "" else JinrouUtil.generateSHA1_php(trip.trim()).substring(1,9)
      //val jinrouuser = JinrouUser.create.uname(uname.trim()).handle_name(handle_name.replace('　',' ').trim()).sex(sex)
      //                            .trip(trip_value)
      //                            .password(JinrouUtil.generateSHA1(password.trim()).substring(0,20))

      jinrouuser.validate match {
        case Nil => ;
        case xs  => is_error = true
                     S.error(xs)
      }

      jinrouuser.password(JinrouUtil.generateSHA1(jinrouuser.password.is.trim()).substring(0,20))
      if (jinrouuser.trip.is != "")
        jinrouuser.trip(JinrouUtil.generateSHA1_php(jinrouuser.trip.is.trim()).substring(1,9))

      JinrouUserLock.synchronized {
        if (is_error == true) {
          return redirectTo("jinrouuser.html", () => JinrouUserRequest.set(Full(jinrouuser)))
        }

        jinrouuser.save
        S.notice(<b>修改成功</b>)
        redirectTo("main.html")
      }
    }

    CurrentJinrouUser.is match {
      case Full(xs) => jinrouuser = xs
      case _        => return redirectTo("main.html")
    }

    val jinrouuser_r = JinrouUserRequest.is
    jinrouuser_r match {
      case Full(xs) => jinrouuser.uname(xs.uname.is)
                       .handle_name(xs.handle_name.is)
                       .sex(xs.sex.is)
                       .email(xs.email.is)
                       .msn(xs.msn.is)
                       .zodiac(xs.zodiac.is)
                       .user_memo(xs.user_memo.is)
      case _        => ;
    }

    bind("entry", xhtml,
      "uname"         -> <span>{jinrouuser.uname}</span>,
      "handle_name"  -> SHtml.text(jinrouuser.handle_name,    jinrouuser.handle_name(_), "size"->"40", "maxlength"->"30"),
      "trip"          -> SHtml.text("",                        jinrouuser.trip(_), "size"->"40", "maxlength"->"30"),
      "password"     -> SHtml.password("",                    jinrouuser.password(_), "size"->"40", "maxlength"->"30"),
      "repassword"   -> SHtml.password("",                    repassword = _, "size"->"40", "maxlength"->"30"),
      "sex"           -> jinrouuser.sex.generateHtml,
      "email"         -%> SHtml.text(jinrouuser.email,          jinrouuser.email(_), "size"->"40", "maxlength"->"80"),
      "msn"           -%> SHtml.text(jinrouuser.msn,            jinrouuser.msn(_), "size"->"40", "maxlength"->"80"),
      //"male"          -> sex_radios(0),
      //"female"       -> sex_radios(1),
      "zodiac"        -> jinrouuser.zodiac.generateHtml,
      "memo"          -> SHtml.textarea(jinrouuser.user_memo,  jinrouuser.user_memo(_), "size"->"40", "rows"->"5", "cols"->"40","wrap"->"soft"),
      "captcha"       -> SHtml.text("",                        captcha = _, "size"->"40", "maxlength"->"80"),
      //"user_icon_id"  -> SHtml.textarea(dummy_last_words,  dummy_last_words = _, "rows"->"3","cols"->"70","wrap"->"soft"),
      "submit"        -> SHtml.submit(" 修  改 ",  update_user)
    )
  }
}