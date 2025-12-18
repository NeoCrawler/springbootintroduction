package com.example.introduction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// By interpretation of: https://medium.com/@smita.s.kothari/unit-testing-in-spring-boot-best-practices-with-sample-code-e483eaa4cc19
// I use SpringBootTest instead of Mockito because we do depend on the rest API.

@SpringBootTest
class IntroductionApplicationTests 
{
	@Autowired
    private IntroductionController _controller;

	@Test
	void ShouldReturnEmptyHello() 
	{
		var result = _controller.hello("");
		Assertions.assertTrue(result.equals("Hello !"));
	}

	@Test
	void ShouldReturnHelloHerald() 
	{
		var result = _controller.hello("Herald");
		Assertions.assertTrue(result.equals("Hello Herald!"));
	}

	@Test
	void ShouldReturnValidQuestion() throws Exception 
	{
		var result = _controller.questionnaire();
		Assertions.assertTrue(result!=null);
	}

	@Test
	void ShouldReturnAnswerCorrect() throws Exception 
	{
		var question = _controller.questionnaire();
		var trivia = _controller.GetTrivia();
		var result = _controller.guess(trivia.correct_answer);

		Assertions.assertTrue(result.equals("true"));
	}
}
