# Virtual threads in SpringBoot demo
Demonstrates using virtual-thread-per-request model in Spring Boot 
(+ embedded Tomcat), and also demonstrates how to create a Java `HttpClient`
configured to use a thread-per-HTTP call. 

I mostly wanted to try out the thread-per-HTTP call functionality, since
the thread-per-servlet request model has been configured elsewhere already.
