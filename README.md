# slick-akka-http-oauth2
The Slick Akka Http Oauth2 is a template for using akka-http with slick, with scala-oauth2 integrated!


##Running

```
$ sbt run
```

##Testing

```
$ sbt test
```

##Simple examples

###Getting a token

```
$ curl http://localhost:8080/oauth/access_token -X POST -d "client_id=bob_client_id" -d "client_secret=bob_client_secret" -d "grant_type=client_credentials"
```

```
{ "token_type":"Bearer",
  "access_token":"sXX7cCHBsk5Qdyh2lbGfduKutgeCJb8lxZZltfpT",
  "expires_in":3599,
  "refresh_token":"hUbAKiQEN95CNtRTLHmAanqf5bZoLRkVSqctjW6m"
}
```

###Accessing protected resources

```
$ curl --dump-header - -H "Authorization: Bearer sXX7cCHBsk5Qdyh2lbGfduKutgeCJb8lxZZltfpT" http://localhost:8080/resources
```

```
HTTP/1.1 200 OK
Server: akka-http/2.4.2
Date: Sun, 03 Jul 2016 21:18:26 GMT
Content-Type: text/plain; charset=UTF-8
Content-Length: 19

Hello bob_client_id
```

##Another examples

There are examples in tests for getting tokens with authorization code and user password.

##TODO
I did TDD to make the upper layer, but since I imported the dals from my play-slick-oauth2 template, the unit tests for dals are still TODO.
