package com.example.introduction;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@SpringBootApplication

// | Tells Spring that this code describes an endpoint that should be made available over the web.
// V 
@RestController
public class IntroductionApplication 
{
	// Entry point.
	public static void main(String[] args) 
	{
		SpringApplication.run(IntroductionApplication.class, args);
	}

	// Token to create a unique session with open triva.
	// We don't care too much for clearing, since it will be removed after 6 hours.
	private String m_token;
	private OpenTrivia m_currentTrivia;

	// App specific entry point.
	public IntroductionApplication()
	{
		// For sanity sake, check whether our entry is being hit.
		ServerLog("Start");

		// Generate an unique token on startup, this will ensure an unique session with open trivia.
		SecureRandom secureRandom = new SecureRandom(); //threadsafe
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();

		byte[] randomBytes = new byte[24];
    	secureRandom.nextBytes(randomBytes);
    	m_token = base64Encoder.encodeToString(randomBytes);

		// Check whether a valid token was generated.
		ServerLog(String.format("Token: %s", m_token));
	}
	

	// | Tells Spring to use our hello() method to answer requests that get sent to the http://localhost:8080/hello address.
	// V 
	@GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
      return String.format("Hello %s!", name);
    }

	/** 
	 * Fetch a question from open trivia.
	 * Source: https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.html
	 * Source: https://www.youtube.com/watch?v=Pd_WMnnsBro
	 */
	@GetMapping("/questionnaire")
    public String questionnaire() throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException
	{
		var uri = URI.create("https://opentdb.com/api.php?amount=1");
		var builder = HttpRequest.newBuilder();

		HttpRequest request = builder.uri(uri).build();
		HttpClient client = HttpClient.newHttpClient();
		
		CompletableFuture<HttpResponse<String>> httpFuture = null;
		httpFuture = client.sendAsync(request, BodyHandlers.ofString());
		
		// Not sure how this functions knows to wait for this.
		httpFuture.thenApply(HttpResponse::body).get(10, TimeUnit.SECONDS);
		httpFuture.thenAccept((value) ->
		{
			ServerLog(value.body().toString());

			ObjectMapper mapper = new ObjectMapper();
			TypeReference<OpenTrivia> jsonRef = new TypeReference<OpenTrivia>() {};
			m_currentTrivia = mapper.readValue(value.body(), jsonRef);
		});
		
		return m_currentTrivia.results[0].question;
    }

	/** 
	 * Fetch a question from open trivia.
	 * Source: https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpRequest.html
	 * Source: https://www.youtube.com/watch?v=Pd_WMnnsBro
	 */
	@GetMapping("/guess")
    public String guess(@RequestParam(value = "answer") String answer)
	{
		// Check if a trivia is cached.
		if(m_currentTrivia != null)
		{
			var trivia = m_currentTrivia.results;

			// Check if there is content in this trivia.
			if(trivia != null)
			{
				if(trivia.length > 0)
				{
					ServerLog(trivia[0].correct_answer);
					ServerLog(answer);
					return trivia[0].correct_answer.equals(answer) ? "true" : "false";
				}
			}
		}

		return "false";
    }

	/** Utility for server logging. */
	public void ServerLog(String value)
	{
		System.out.println(String.format("Server | %s", value));
	}

}

