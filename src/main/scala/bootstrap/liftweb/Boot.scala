package bootstrap.liftweb

import _root_.net.liftweb.util._
import _root_.net.liftweb.common._
import _root_.net.liftweb.http._
import _root_.net.liftweb.http.provider._
import net.liftweb.mapper._
import javax.mail.{PasswordAuthentication, Authenticator}
import org.plummtw.jinrou_domi.view.ChineseCaptchaView
//import _root_.net.liftweb.sitemap._
//import _root_.net.liftweb.sitemap.Loc._
//import Helpers._
import _root_.java.sql.{Connection, DriverManager}

import org.plummtw.jinrou_domi.model._


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // DB
    if (!DB.jndiJdbcConnAvailable_?) {
      //LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, DBVendor)
    }

    // Snippet
    LiftRules.addToPackages("org.plummtw.jinrou_domi")

    // Model
    val schimifier_results = Schemifier.schemify(true, Schemifier.infoF _, JinrouUser, JinrouUserLogin, UserIcon)

    schimifier_results.foreach { schimifier_result =>
      if (schimifier_result.startsWith("CREATE TABLE usericon"))
        // 加入預設 User Icon
        create_default_usericon
    }

    // View
    LiftRules.dispatch.append {
     case Req("captcha" :: Nil, _, _) =>
       ChineseCaptchaView.captcha
    }


    // Build SiteMap
    //LiftRules.statelessDispatchTable.append(MyCSSMorpher)

    //LiftRules.setSiteMap(SiteMap(entries:_*))

    /*
     * Show the spinny image when an Ajax call starts
     */
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    /*
     * Make the spinny image go away when it ends
     */
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    LiftRules.early.append(makeUtf8)

    //LiftRules.loggedInTest = Full(() => User.loggedIn_?)

    S.addAround(DB.buildLoanWrapper)
  }

  /**
   * Force the request to be UTF-8
   */
  private def makeUtf8(req: HTTPRequest) {
    req.setCharacterEncoding("UTF-8")
  }

  private def configMailer() {
    // Enable TLS support
    System.setProperty("mail.smtp.starttls.enable","true");
    // // Set the host name
    System.setProperty("mail.smtp.host", Props.get("mail.host").getOrElse(""))
    // // Enable authentication
    System.setProperty("mail.smtp.auth", "true")

    val user     = Props.get("mail.user").getOrElse("")
    val password = Props.get("mail.password").getOrElse("")
    // // Provide a means for authentication. Pass it a Can, which can either be Full or Empty
    Mailer.authenticator = Full(new Authenticator {
      override def getPasswordAuthentication = new PasswordAuthentication(user, password)
    })
  }

  private def create_default_usericon() {
    val default_icons = List(
      //List("替身君", "dummy_boy_user_icon.gif", "#000000"),
      List("明灰",   "001.gif", "#DDDDDD"),
      List("暗灰",   "002.gif", "#999999"),
      List("黄色",   "003.gif", "#FFD700"),
      List("橘色",   "004.gif", "#FF9900"),
      List("紅色",   "005.gif", "#FF0000"),
      List("水色",   "006.gif", "#99CCFF"),
      List("青",     "007.gif", "#0066FF"),
      List("緑",     "008.gif", "#00EE00"),
      List("紫",     "009.gif", "#CC00CC"),
      List("櫻花色", "010.gif", "#FF9999"))

    default_icons.foreach { default_icon =>
      val user_icon = UserIcon.create.icon_group(0).icon_gname("")
                              .icon_name(default_icon(0)).icon_filename("user_icons/" + default_icon(1))
                              .icon_width(45).icon_height(45).color(default_icon(2))
      user_icon.save
    }
  }
}

/*
import net.liftweb.http.rest._

object MyCSSMorpher extends RestHelper {
  serve {
    case r @ Req("dynocss" :: file :: _, "css", GetRequest) =>
      for {
        convertFunc <- findConvertFunc(r)
        fileContents <- readFile(file+".css")
        converted <- convertFunc(fileContents)
      } yield CSSResponse(converted)
  }

  // based on the browser detected, return a function 
  // that will convert HTML5 css into CSS for that browser
  def findConvertFunc(req: Req): Box[String => Box[String]] =
    Empty

  // load the file from the specific location...
  // are you going put the CSS templates in
  // resources, etc.
  def readFile(name: String): Box[String] = Empty
}
*/

object DBVendor extends ConnectionManager {
  private var pool: List[Connection] = Nil
  private var poolSize = 0
  private val maxPoolSize = 66

  private def createOne: Box[Connection] = try {
    val driverName: String = Props.get("db.driver") openOr
      "org.apache.derby.jdbc.EmbeddedDriver"

    val dbUrl: String = Props.get("db.url") openOr
      "jdbc:derby:lift_example;create=true"

    Class.forName(driverName)

    val dm = (Props.get("db.user"), Props.get("db.password")) match {
      case (Full(user), Full(pwd)) =>
        DriverManager.getConnection(dbUrl, user, pwd)

      case _ =>
        DriverManager.getConnection(dbUrl)
    }

    Full(dm)
  } catch {
    case e: Exception => e.printStackTrace; Empty
  }

  def newConnection(name: ConnectionIdentifier): Box[Connection] =
    synchronized {
      pool match {
        case Nil if poolSize < maxPoolSize =>
          val ret = createOne
        poolSize = poolSize + 1
        ret.foreach(c => pool = c :: pool)
        ret

        case Nil => wait(100L); newConnection(name)
        case x :: xs => try {
          x.setAutoCommit(false)
          Full(x)
        } catch {
          case e =>
            e.printStackTrace
            try {
              pool = xs
              poolSize = poolSize - 1
              x.close
            } catch {
              case e =>
              e.printStackTrace
            }
          newConnection(name)
        }
      }
    }

  def releaseConnection(conn: Connection): Unit = synchronized {
    pool = conn :: pool
    notify
  }
}