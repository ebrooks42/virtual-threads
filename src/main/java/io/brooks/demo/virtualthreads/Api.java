package io.brooks.demo.virtualthreads;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;
import java.util.concurrent.Executors;

@RestController
@Slf4j
public class Api {
    private final Random random;

    public Api() {
        this.random = new Random();
    }

    public record Result(String thread, String output) {
    }

    @GetMapping("/sleep")
    Result sleep(@RequestParam(required = false, defaultValue = "2000") long duration) {
        try {
            log.info("Request handled by thread {}", Thread.currentThread());
            Thread.sleep(duration);
            return new Result(Thread.currentThread().toString(), "slept for " + duration + "ms");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/call-api")
    Result callApi() {
        log.info("Request handled by thread {}", Thread.currentThread());
        HttpClient client = HttpClient
                .newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        int postNumber = random.nextInt(100);
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create("https://jsonplaceholder.typicode.com/posts/" + postNumber)).GET().build();
        try {
            var response = client.send(
                    request,
                    responseInfo -> {
                        log.info("Response marshalling handled by thread {}", Thread.currentThread());
                        return HttpResponse.BodyHandlers.ofString().apply(responseInfo);
                    });
            return new Result(Thread.currentThread().toString(), response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
