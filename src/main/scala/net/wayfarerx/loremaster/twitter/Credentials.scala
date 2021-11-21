package net.wayfarerx.loremaster.twitter

/**
 * OAuth credentials that can be used to authenticate with Twitter.
 *
 * @param consumerKey       The OAuth consumer key to authenticate with.
 * @param consumerSecret    The OAuth consumer secret to authenticate with.
 * @param accessToken       The OAuth access token to authenticate with.
 * @param accessTokenSecret The OAuth access token secret to authenticate with.
 */
case class Credentials(
  consumerKey: String,
  consumerSecret: String,
  accessToken: String,
  accessTokenSecret: String
)
