package com;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Test2Captcha extends Thread
{
	private static String url = "https://www.google.com/recaptcha/api2/demo";
	private static String key = "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-";
	private static String APIKey = ""; // your API key from 2Captcha
	private static String proxy = "";
	private static String proxyType = "";
	
	private double secondsAll = 0;
	private double shortestTime = -1;
	private double mean = 0;
	private double secondsForOne = 0;
	private int numberOfSolved = 0;
	private int total = 0;
	
	private static double secondsAllTotal = 0;
	private static int numberOfSolvedTotal = 0;
	private static double shortestTimeFinal = -1;
	private static double meanFinal = 0;
	private static int totalAmount = 0;
	
	private static final int MAX_EXCEPTIONS = 5;
	private static final int TIME_FOR_SOLVE = 130;
	
	private int streamNumb;
	private static int streamAmount = 0;
	private static final int AMOUNT_CAPTCHA_FOR_STREAM = 25;
	
	public static void main(String[] args)
	{
		Test2Captcha firstStream = new Test2Captcha(1);
		Test2Captcha secondStream = new Test2Captcha(2);
		Test2Captcha thirdStream = new Test2Captcha(3);
		Test2Captcha fourthStream = new Test2Captcha(4);
		firstStream.start();
		secondStream.start();
		thirdStream.start();
		fourthStream.start();
		
		try
		{
			firstStream.join();
			secondStream.join();
			thirdStream.join();
			fourthStream.join();
		}
		catch(InterruptedException e)
		{
			System.out.println("Exception in \'main\': " + e.getMessage());
		}
		finally
		{
			System.out.println("	Seconds: " + secondsAllTotal);
			System.out.println("	Number of solved: " + numberOfSolvedTotal);
			System.out.println("	Shortest time: " + shortestTimeFinal);
			System.out.println("	Mean: " + (meanFinal / streamAmount));
			System.out.println("	Total: " + totalAmount);
		}
	}
	
	public Test2Captcha(int stream)
	{
		streamNumb = stream;
	}
	
	public void run()
	{
		streamAmount++;
		String captchaId;
		String token;
		String connectUrlIn = "https://2captcha.com/in.php?key=" + APIKey 
				+ "&method=userrecaptcha" 
				+ "&googlekey=" + key 
				+ "&pageurl=" + url
				+ "&proxy=" + proxy 
				+ "&proxytype=" + proxyType;
		String connectUrlRes = "https://2captcha.com/res.php?key=" + APIKey
				+ "&action=get"
				+ "&id=";
		Document doc;
		
		int exception = 0;
		
		Date dateStart;
		Date dateFinish;
		
		String regexId = "(?<=OK|)([0-9]+)";
		Pattern patternId = Pattern.compile(regexId);
		
		String regexRes = "(CAPCHA_NOT_READY|ERROR_CAPTCHA_UNSOLVABLE|ERROR_BAD_DUPLICATES)";
		String regexResError = "^(REPORT|ERROR)[A-Z_]+$";
		Pattern patternRes = Pattern.compile(regexRes);
		Pattern patternResError = Pattern.compile(regexResError);
		
		for(int i = 0; i < AMOUNT_CAPTCHA_FOR_STREAM; i++)
		{
			if(exception >= MAX_EXCEPTIONS)
			{
				System.out.println("!!! TOO MANY EXCEPTION | Stream: " + streamNumb);
				break;
			}
			
			try
			{
				// Sending captcha and save ID
				dateStart = new Date();
				doc = Jsoup.connect(connectUrlIn).get();
				captchaId = doc.text();

				Matcher matcherId = patternId.matcher(captchaId);
				if(matcherId.find())
					captchaId = matcherId.group();
				else
				{
					System.out.println("--- https://2captcha.com/in.php returned error code: " + captchaId + "| Stream: " + streamNumb);
					break;
				}
				System.out.println("### Sending a new captcha. Id: " + captchaId + "| Stream: " + streamNumb);
			}
			catch(IOException e)
			{
				System.out.println("--- Exception connect to https://2captcha.com/in.php : " + e.getMessage() + "| Stream: " + streamNumb);
				exception++;
				i--;
				continue;
			}
			
			System.out.println("The stream sleeps 15 seconds| Stream: " + streamNumb);
			try {Thread.sleep(15000);}
			catch(InterruptedException e) {System.out.println("--- Exception in method sleep(15000) : " + e.getMessage() + "| Stream: " + streamNumb);}
			
			while(true)
			{
				if(exception >= MAX_EXCEPTIONS)
					break;
				
				if((new Date().getTime() - dateStart.getTime()) / 1000.0 > TIME_FOR_SOLVE)
				{
					System.out.println("A lot of time has passed ( >" + TIME_FOR_SOLVE + " seconds). Id: " + captchaId + "| Stream: " + streamNumb);
					captchaSolved(false);
					break;
				}
				
				try
				{
					// Request a response from https://2captcha.com/res.php
					System.out.println("Request from https://2captcha.com/res.php. Id: " + captchaId + " | Stream: " + streamNumb);
					doc = Jsoup.connect(connectUrlRes + captchaId).get();
					token = doc.text();
				}
				catch(IOException e)
				{
					System.out.println("--- Exception connect to https://2captcha.com/res.php| Stream: " + streamNumb);
					exception++;
					continue;
				}
				
				// Check status of captcha
				Matcher matcherRes = patternRes.matcher(token);
				Matcher matcherResError = patternResError.matcher(token);
				if(matcherRes.find())
				{
					System.out.println("##### Captcha token (" + captchaId + "): " + token);
					if(token.equals("CAPCHA_NOT_READY"))
					{
						try {Thread.sleep(5000);}
						catch(InterruptedException e) {System.out.println("--- Exception in method sleep(5000) : " + e.getMessage() + "| Stream: " + streamNumb);}
						continue;
					}
					else
					{
						captchaSolved(false);
						break;
					}
				}
				else if(matcherResError.find())
				{
					System.out.println("--- https://2captcha.com/res.php returned error code: " + token + "| Stream: " + streamNumb);
					exception = MAX_EXCEPTIONS;
					break;
				}
				else
				{
					dateFinish = new Date();
					secondsForOne = (dateFinish.getTime() - dateStart.getTime()) / 1000.0;
					captchaSolved(true);
					break;
				}
			}
		}
		
		if(total != 0)
			saveData(secondsAll, numberOfSolved, shortestTime, mean, total);
		else
			streamAmount--;
	}
	
	public synchronized static void saveData(double secondsAll, int numberOfSolved, double shortestTime, double mean, int total)
	{
		secondsAllTotal += secondsAll;
		numberOfSolvedTotal += numberOfSolved;
		if(shortestTimeFinal > shortestTime || shortestTimeFinal == -1)
			shortestTimeFinal = shortestTime;
		meanFinal += mean;
		totalAmount += total;
	}
	
	private void captchaSolved(boolean solved)
	{
		total++;
		
		if(solved)
		{
			System.out.println("##### Captcha SOLVED (Stream: " + streamNumb + ")");
			numberOfSolved++;
			secondsAll += secondsForOne;
			if(numberOfSolved != 0)
				mean = secondsAll / numberOfSolved;
			if(shortestTime == -1 || shortestTime > secondsForOne)
				shortestTime = secondsForOne;
		}
		else
			System.out.println("##### Captcha not RESOLVED (Stream: " + streamNumb + ")");
		
		System.out.println("Stream: " + streamNumb + " [" + secondsAll + ";" + numberOfSolved + ";" + shortestTime + ";" + mean + ";" + total + "]");
	}
}
