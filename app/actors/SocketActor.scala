package actors

import akka.actor.{Props, Actor}

import play.api.libs.iteratee.{Concurrent, Enumerator}

import play.api.libs.iteratee.Concurrent.Channel
import play.api.Logger
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.util.{ Success, Failure }
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import scala.concurrent.{ExecutionContext, Future}
import play.api.Play.current
import service.{RedisService, RedisUserService}
import RedisService.{idToString, stringToId}


import securesocial.core._
import actors.RequestAlias
import securesocial.core.OAuth1Info
import securesocial.core.IdentityId
import actors.AckSocket
import scala.util.Failure
import play.api.libs.json.JsString
import scala.Some
import actors.StartSocket
import securesocial.core.OAuth2Info
import scala.util.Success
import securesocial.core.PasswordInfo
import play.api.libs.json.JsObject
import actors.SocketClosed


class SocketActor extends Actor {
  case class UserChannel(user_id: IdentityId, var channelsCount: Int, enumerator: Enumerator[JsValue], channel: Channel[JsValue])

  lazy val log = Logger("application." + this.getClass.getName)


  // this map relate every user with his or her UserChannel
  var webSockets: Map[IdentityId, UserChannel] = Map.empty


  def establishConnection(user_id: IdentityId): UserChannel = {
    log.debug(s"establish socket connection for user $user_id")

    val userChannel: UserChannel =  webSockets.get(user_id) getOrElse {
        val broadcast: (Enumerator[JsValue], Channel[JsValue]) = Concurrent.broadcast[JsValue]
        UserChannel(user_id, 0, broadcast._1, broadcast._2)

      }

    userChannel.channelsCount = userChannel.channelsCount + 1
    webSockets += (user_id -> userChannel)

    userChannel
  }


  def onRecentPosts(posts: Seq[Msg], user_id: IdentityId) = {

    import RedisService.idToString

    posts.reverse.foreach{ msg =>
      webSockets(user_id).channel push Update(
        msg = Some(msg)
      ).asJson
    }

  }



  override def receive = {
    case StartSocket(user_id) =>
        val userChannel = establishConnection(user_id)
        sender ! userChannel.enumerator




    case AckSocket(user_id) =>
      log.debug(s"ack socket $user_id")

      val result = for {
        posts <- RedisService.recent_posts
        //users <- Future.sequence(posts.map(p => RedisService.get_public_user(p.user_id).map(_.get)))
      } yield posts //users.zip(posts)

      result.onComplete{
        case Success(messages) =>   onRecentPosts(messages, user_id)
        case Failure(t) => log.error(s"recent posts fail: ${t}");
      }



    case RequestAlias(user_id, alias) =>
        log.info(s"user $user_id requesting alias $alias")

        val alias_f = RedisService.establish_alias(user_id, alias)

        alias_f.foreach{ alias_pass =>

              }

        alias_f.map(result => AckRequestAlias(alias, result)).onComplete{
          case Success(ar) => webSockets(user_id).channel push Update(alias_result = Some(ar)).asJson
          case Failure(t) => log.error(s"error requesting alias: $t")
        }



    case RequestInfo(requester, user_id) =>
        RedisService.get_public_user(user_id).onComplete{
          case Success(Some(user_info)) => webSockets(requester).channel push Update(user_info = Some(user_info)).asJson
          case Success(None) => log.error(s"user info for $user_id not found");
          case Failure(t) => log.error(s"error: ${t}");
        }



    case message@Msg(user_id, msg) =>
        RedisService.post(user_id, msg).onComplete{ _ =>
                webSockets(user_id).channel push Update(
                  Some(message),
                  None
                ).asJson
        }



    case SocketClosed(user_id) =>
      log debug s"closed socket for $user_id"
      val userChannel = webSockets(user_id)

      if (userChannel.channelsCount > 1) {
        userChannel.channelsCount = userChannel.channelsCount - 1
        webSockets += (user_id -> userChannel)
        log debug s"channel for user : $user_id count : ${userChannel.channelsCount}"
      } else {
        removeUserChannel(user_id)
      }

  }

  def removeUserChannel(user_id: IdentityId) = {
    log debug s"removed channel for $user_id"
    webSockets -= user_id
  }
}


sealed trait SocketMessage
sealed trait JsonMessage{
  def asJson: JsValue
}


case class AckSocket(user_id: IdentityId)

case object Register extends SocketMessage

case class StartSocket(user_id: IdentityId) extends SocketMessage

case class SocketClosed(user_id: IdentityId) extends SocketMessage

case class RequestAlias(user_id: IdentityId, alias: String) extends SocketMessage

case class RequestInfo(requester: IdentityId, user_id: IdentityId) extends SocketMessage


object Msg {

  implicit val format = new Format[Msg]{
    def writes(msg: Msg): JsValue = {
      JsObject(Seq(
        ("user_id", JsString(idToString(msg.user_id))),
        ("msg", JsString(msg.msg))
      ))
    }

    def reads(json: JsValue): JsResult[Msg] =
      for{
        identityId <- Json.fromJson[String](json \ "user_id").map(stringToId(_))
        msg <- Json.fromJson[String](json \ "msg")
      } yield Msg(identityId, msg)





  }

}

object AckRequestAlias {
  implicit val format = Json.format[AckRequestAlias]
}

object PublicIdentity {
  implicit val format1 = Json.format[IdentityId]
  implicit val format2 = Json.format[PublicIdentity]}

object Update {
  implicit val format = Json.format[Update]
}

//todo: add post id to Msg for absolute ordering
case class Msg(user_id: IdentityId, msg: String) extends JsonMessage with SocketMessage{
  def asJson = Json.toJson(this)
}

case class AckRequestAlias(alias: String, pass: Boolean) extends JsonMessage{
  def asJson = Json.toJson(this)
}

case class PublicIdentity(user_id: String, alias: String, avatar_url: Option[String])

case class Update(msg: Option[Msg]=None,
				  alias_result: Option[AckRequestAlias]=None,
				  user_info: Option[PublicIdentity]=None) extends JsonMessage {

  def asJson = Json.toJson(this)
}