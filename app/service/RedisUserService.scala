/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import play.api.{Logger, Application}
import securesocial.core.{AuthenticationMethod, OAuth1Info, OAuth2Info, Identity, PasswordInfo, IdentityId, UserServicePlugin}
import scredis.Redis
import play.api.libs.json._
import play.api.libs.functional.syntax._
import scredis.parsing.{Parser, StringParser}
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.Await
import scala.concurrent.duration._
import securesocial.core.SocialUser
import com.typesafe.config.{ConfigValueFactory, ConfigValue, Config, ConfigFactory}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

import java.net.URI
import securesocial.core.providers.Token

/**
 * It will be necessary to block on redis calls here. cache heavily to minimize blocking api calls.
 */



class RedisUserService(application: Application) extends UserServicePlugin(application) {
  lazy val log = Logger("application." + this.getClass.getName)



  def find(id: IdentityId): Option[Identity] = {

    log.debug(s"find $id")

    val fJson = RedisServiceImpl.get_user(id)
    val json = Await.result(fJson, 1 second)

    json
  }


  //oh god this is awful
  def save(user: Identity): Identity = {

    Await.result(RedisServiceImpl.save_user(user), 1 second)

    user
  }

  //not implemented
  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = None
  def save(token: Token) = None
  def findToken(token: String): Option[Token] = None
  def deleteToken(uuid: String) = None
  def deleteTokens() = None
  def deleteExpiredTokens() = None
}
