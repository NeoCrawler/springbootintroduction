package com.example.introduction;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import java.security.SecureRandom;
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
	// Consts
	final int TRIVIAS = 25;

	// Custom pair object.
	public record Pair<T0, T1>(T0 Key, T1 Value) {}

	// Entry point.
	public static void main(String[] args) 
	{
		SpringApplication.run(IntroductionApplication.class, args);
	}

	// ----------
	// Members

	private SecureRandom secureRandom = new SecureRandom();
	private OpenTrivia m_currentTrivia = null;
	private int m_triviaIndex = TRIVIAS;
	private boolean m_fetchingTrivia = false;
	
	// ----------
	// Constructor

	// App specific entry point.
	public IntroductionApplication()
	{
		// For sanity sake, check whether our entry is being hit.
		ServerLog("Start");
	}
	
	// ----------
	// Web Calls

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
    public String[] questionnaire() throws URISyntaxException, ExecutionException, InterruptedException, TimeoutException
	{
		if(m_triviaIndex < TRIVIAS)
		{
			m_triviaIndex++;
			return QuestionAnswers(m_triviaIndex-1);
		}
		else
		{
			if(!m_fetchingTrivia)
			{
				m_fetchingTrivia = true;

				var uri = URI.create(String.format("https://opentdb.com/api.php?amount=%s", TRIVIAS));
				var builder = HttpRequest.newBuilder();

				HttpRequest request = builder.uri(uri).build();
				HttpClient client = HttpClient.newHttpClient();
				
				CompletableFuture<HttpResponse<String>> httpFuture = null;
				httpFuture = client.sendAsync(request, BodyHandlers.ofString());
				
				// Not sure how this functions knows to wait for this.
				httpFuture.thenApply(HttpResponse::body).get(1, TimeUnit.SECONDS);
				httpFuture.thenAccept((value) ->
				{
					ServerLog(value.body().toString());

					ObjectMapper mapper = new ObjectMapper();
					TypeReference<OpenTrivia> jsonRef = new TypeReference<OpenTrivia>() {};
					m_currentTrivia = mapper.readValue(value.body(), jsonRef);
				});
			
				// Need to check if the respose did not result in an empty result.
				if(m_currentTrivia.results != null)
				{
					var result = m_currentTrivia.results;

					if(result.length > 0)
					{
						m_triviaIndex = 1;
						m_fetchingTrivia = false;
						return QuestionAnswers(0);
					}
				}

				m_fetchingTrivia = false;
			}
			
			return new String[] {"API not responding, please try again in a minute."};
		}
    }

	/** 
	 * Guess the anwer.
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
					for(int i = 0; i < trivia.length; i++)
					{
						var current = trivia[i];
						ServerLog(current.correct_answer);
						ServerLog(answer);

						if(current.correct_answer.equals(answer))
							return "true";
					}
				}
			}
		}

		return "false";
    }

	// ----------
	// Utlity

	/** Merge anwers. */
	private String[] QuestionAnswers(int index)
	{
		var trivia = m_currentTrivia.results[index];
		int count = 2 + trivia.incorrect_answers.length;

		String[] result = new String[count];
		result[0] = trivia.question;
		result[1] = trivia.correct_answer;

		for(int i = 0; i < trivia.incorrect_answers.length; i++)
		{
			result[i+2] = trivia.incorrect_answers[i];
		}

		// Randomise the awnser.
		
		int rand = 2 + secureRandom.nextInt(0, trivia.incorrect_answers.length);

		Pair<Integer, String> anwser = new Pair<>(1, result[1]);
		Pair<Integer, String> target = new Pair<>(rand, result[rand]);

		result[anwser.Key] = target.Value;
		result[target.Key] = anwser.Value; 

		return result;
	}		

	/** Utility for server logging. */
	public void ServerLog(String value)
	{
		System.out.println(String.format("Server | %s", value));
	}

}

