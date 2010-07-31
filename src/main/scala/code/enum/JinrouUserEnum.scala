package org.plummtw.jinrou_domi.enum

object JinrouUserEnum extends Enumeration {
  type JinrouUserEnum = Value

  val UNAUTHORIZED    = Value("UA")  // 未註冊
  val LOCKED            = Value("LK") // 已鎖定

  implicit def jinrouuserenum2String (en : JinrouUserEnum) : String = en.toString
}

object JinrouUserLoginEnum extends Enumeration {
  type JinrouUserLoginEnum = Value

  val LOGIN             = Value("0")  // 登入
  val LOGOUT            = Value("1")  // 登出
  val LOGIN_FAIL        = Value("2")  // 登入失敗

  implicit def jinrouuserloginenum2String (en : JinrouUserLoginEnum) : String = en.toString
}

