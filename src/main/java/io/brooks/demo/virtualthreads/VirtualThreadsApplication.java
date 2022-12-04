package io.brooks.demo.virtualthreads;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.Executors;

@SpringBootApplication
@Slf4j
public class VirtualThreadsApplication {
    public static void main(String[] args) {
        SpringApplication.run(VirtualThreadsApplication.class, args);
    }

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor asyncTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }

    public record Response(String thread, String message) {
        static Response fromMessage(String message) {
            return new Response(Thread.currentThread().toString(), message);
        }
    }

    @GetMapping("/sleep")
    Response sleep(@RequestParam(required = false, defaultValue = "2000") long duration) {
        try {
            log.info("Request handled by thread {}", Thread.currentThread());
            Thread.sleep(duration);
            return Response.fromMessage("slept for " + duration + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/call-api")
    Response callApi() {
        log.info("Request handled by thread {}", Thread.currentThread());
        HttpClient client = HttpClient.newBuilder().executor(Executors.newVirtualThreadPerTaskExecutor()).build();
        int postNumber = new Random().nextInt(100);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://jsonplaceholder.typicode.com/posts/" + postNumber)).GET().build();
        HttpResponse<String> response;
        try {
            response = client.send(request, responseInfo -> {
                log.info("Response marshalling handled by thread {}", Thread.currentThread());
                return HttpResponse.BodyHandlers.ofString().apply(responseInfo);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Response.fromMessage(response.body());
    }

}
