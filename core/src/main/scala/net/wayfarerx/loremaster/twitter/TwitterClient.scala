/* TwitterClient.scala
 *
 * Copyright (c) 2022 wayfarerx (@thewayfarerx).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package net.wayfarerx.loremaster
package twitter

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

import zio.IO

import http.*
import model.*

/**
 * Definition of a Twitter client.
 */
trait TwitterClient:

  /**
   * Posts a book to Twitter.
   *
   * @param book The book to post to Twitter.
   * @return The result of posting a book to Twitter.
   */
  def postTweet(book: Book): IO[TwitterProblem, Unit]

/**
 * Definitions associated with Twitter clients.
 */
object TwitterClient extends ((String, Http) => TwitterClient) :

  /** The Twitter HTTP endpoint to connect to for tweeting. */
  val TweetsEndpoint = "https://api.twitter.com/2/tweets"

  /**
   * Creates a Twitter client.
   *
   * @param bearerToken The bearer token to authenticate to Twitter with.
   * @param http        The http service to use when connecting to Twitter.
   * @return A new Twitter client.
   */
  override def apply(bearerToken: String, http: Http): TwitterClient = book => {
    http.post(
      TweetsEndpoint,
      emitJson(Body(text = Some(book.toString))),
      "Authorization" -> s"Bearer $bearerToken", "Content-Type" -> "application/json"
    ) *> IO.unit
  } catchAll { problem =>
    IO.fail(TwitterProblem(Messages.twitterFailure(problem.message), Some(problem), problem.shouldRetry))
  }

  /**
   * The body of a tweet.
   *
   * @param text The optional text of the tweet.
   */
  case class Body(text: Option[String] = None)

  /**
   * Factory for tweet bodies.
   */
  object Body extends (Option[String] => Body) :

    /** The given encoding of tweet bodies to JSON. */
    given Encoder[Body] = deriveEncoder[Body].mapJson(_.dropNullValues)