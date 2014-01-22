package actors

import securesocial.core.IdentityId
import play.api.libs.json._
import securesocial.core.IdentityId
import service.IdentityIdConverters._
import securesocial.core.IdentityId
import securesocial.core.IdentityId
import securesocial.core.IdentityId
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber


sealed trait JsonMessage{
  def asJson: JsValue
}


object Msg {

  implicit val format = new Format[Msg]{
    def writes(msg: Msg): JsValue = {
      JsObject(Seq(
        ("timestamp", JsNumber(msg.timestamp)),
        ("user_id", JsString(idToString(msg.user_id))),
        ("body", JsString(msg.body))
      ))
    }

    def reads(json: JsValue): JsResult[Msg] =
      for{
        timeStamp <- Json.fromJson[Long](json \ "timestamp")
        identityId <- Json.fromJson[String](json \ "user_id").map(stringToId(_))
        msg <- Json.fromJson[String](json \ "body")
      } yield Msg(timeStamp,identityId, msg)

  }

}

object AckRequestAlias {
  implicit val format = Json.format[AckRequestAlias]
}

object PublicIdentity {
  implicit val format1 = Json.format[IdentityId]
  implicit val format2 = Json.format[PublicIdentity]}


object MsgInfo{
  implicit val format = Json.format[MsgInfo]
}

object Update {
  implicit val format = Json.format[Update]
}


case class MsgInfo(post_id: String, favorite: Boolean, msg: Msg)

//todo: case class representing message + isFavorite and post_id for sending to client
case class Msg(timestamp: Long, user_id: IdentityId, body: String) extends JsonMessage with SocketMessage{
  def asJson = Json.toJson(this)
}

case class AckRequestAlias(alias: String, pass: Boolean) extends JsonMessage{
  def asJson = Json.toJson(this)
}

case class PublicIdentity(user_id: String, alias: String, avatar_url: Option[String])

case class Update(msg: List[MsgInfo]=Nil,
                  alias_result: Option[AckRequestAlias]=None,
                  user_info: Option[PublicIdentity]=None) extends JsonMessage {

  def asJson = Json.toJson(this)
}






sealed trait SocketMessage

case class AckSocket(user_id: IdentityId)

case object Register extends SocketMessage

case class StartSocket(user_id: IdentityId) extends SocketMessage

case class SocketClosed(user_id: IdentityId) extends SocketMessage

case class RequestAlias(user_id: IdentityId, alias: String) extends SocketMessage

case class RequestInfo(requester: IdentityId, user_id: IdentityId) extends SocketMessage

case class DeleteMessage(userId: IdentityId, post_id: String)

case class FavoriteMessage(userId: IdentityId, post_id: String)

case class UnFavoriteMessage(userId: IdentityId, post_id: String)