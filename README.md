# Http4s WebApp Common 

Simple library providing some common functionality needed for developing http4s based web applications. 
It was primarily developed in support of [WebAuthn4s](https://www.webauthn4s.com/) and it's sample application.

* effectful ID generator 
  * includes secure implementation
* generic token store 
  * includes Redis implementation
* Magic Link service 
  * includes Redis store implementation based on the generic Redis token store