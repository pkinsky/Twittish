package service

import securesocial.core.{Identity, IdentityId}
import actors.{MsgInfo, Msg, PublicIdentity}
import scala.concurrent.Future

trait RedisService {

  def get_followers(user_id: IdentityId): Future[Set[String]]

  def save_user(user: Identity): Future[Unit]

  def delete_post(post_id: String): Future[Unit]

  def get_user(user_id: IdentityId): Future[Identity]

  def get_public_user(user_id: IdentityId): Future[PublicIdentity]

  def post(msg: Msg): Future[String]

  def load_post(post_id: String): Future[Msg]

  def recent_posts(user_id: IdentityId): Future[List[MsgInfo]]

  def add_favorite_post(user_id: IdentityId, post_id: String): Future[Unit]

  def remove_favorite_post(user_id: IdentityId, post_id: String): Future[Unit]

  def load_favorite_posts(user_id: IdentityId): Future[Set[String]]

  def establish_alias(user_id: IdentityId, alias: String): Future[Boolean]
}