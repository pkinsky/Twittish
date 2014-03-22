package actors

import play.api.libs.json._
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json.JsNumber
import service.{UserId, PostId}

sealed trait JsonMessage{
  def asJson: JsValue
}


object Msg {

  implicit val format = new Format[Msg]{
    def writes(msg: Msg): JsValue = {
      JsObject(Seq(
        ("timestamp", JsNumber(msg.timestamp)),
        ("user_id", JsString(msg.uid.uid)),
        ("post_id", JsString(msg.post_id.pid)),
        ("body", JsString(msg.body))
      ))
    }

    def reads(json: JsValue): JsResult[Msg] =
      for{
        timeStamp <- Json.fromJson[Long](json \ "timestamp")
        uid <- Json.fromJson[String](json \ "user_id")
        pid <- Json.fromJson[String](json \ "post_id")
        msg <- Json.fromJson[String](json \ "body")
      } yield Msg(PostId(pid), timeStamp, UserId(uid), msg)
  }

}

object User {
  implicit val format = Json.format[User]
}

case class User(uid: String, username: String, isFollowing: Boolean) extends JsonMessage {
  def asJson = Json.toJson(this)
}




case class Msg(post_id: PostId, timestamp: Long, uid: UserId, body: String) extends JsonMessage with SocketMessage{
  def asJson = Json.toJson(this)
}

object Update {
  implicit val format = Json.format[Update]
}

// users is map(user id => user name)
case class Update(feed: String, users: Seq[User], messages: Seq[Msg]) extends JsonMessage {
  def asJson = Json.toJson(this)
}

sealed trait SocketMessage

case class SendMessages(src: String, user_id: UserId, posts: Seq[PostId])

case class MakePost(author_uid: UserId, body: String)

case class RecentPosts(uid: UserId)

case class StartSocket(uid: UserId) extends SocketMessage

case class SocketClosed(uid: UserId) extends SocketMessage

case class RequestAlias(uid: UserId, alias: String) extends SocketMessage

case class SetAboutMe(uid: UserId, about_me: String)
