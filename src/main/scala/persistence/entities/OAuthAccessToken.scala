package persistence.entities

import java.sql.Timestamp

import scalaoauth2.provider.GrantHandlerResult

case class OAuthAccessToken( id: Long,
                             accountId: Long,
                             oauthClientId: Long,
                             accessToken: String,
                             refreshToken: String,
                             createdAt: Timestamp
                           ) extends BaseEntity

case class TokenResponse(token_type : String, access_token : String, expires_in : Long, refresh_token : String)