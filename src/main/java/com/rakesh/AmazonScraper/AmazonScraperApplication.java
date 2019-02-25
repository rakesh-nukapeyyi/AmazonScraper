package com.rakesh.AmazonScraper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@SpringBootApplication
@RestController
public class AmazonScraperApplication {

	private @Value("${session-id}") String sessionId;
	private @Value("${session-token}") String sessionToken;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(AmazonScraperApplication.class, args);
	}

	@GetMapping("/test")
	public String getSomething() {
		return "rakesh";
	}

	@GetMapping("/review/{asin}")
	public AmazonProduct scrape(@PathVariable("asin") String asin) throws Exception {
		Map<String, String> cookies = new HashMap<>();
		cookies.put("session-id", sessionId);
		cookies.put("session-token", sessionToken);

		Document amazonProductHomeDocument = Jsoup.connect("https://www.amazon.in/dp/" + asin)
				.cookies(cookies)
				.get();
		Element titleElement = amazonProductHomeDocument.getElementById("productTitle");
		Element avgCustomerReviewsElement = amazonProductHomeDocument.getElementById("averageCustomerReviews");
		Elements starRatingAltElement = avgCustomerReviewsElement.getElementsByClass("a-icon-alt");
		String reviewsCountText = amazonProductHomeDocument.getElementById("acrCustomerReviewText")
				.text();
		int reviewsCount = Integer.parseInt(reviewsCountText.substring(0, reviewsCountText.indexOf(" ")));
		Element reviewsLinkElement = amazonProductHomeDocument.getElementById("acrCustomerReviewLink");
		String reviewLink = reviewsLinkElement.attr("href");
		Document productReviewsDocument = Jsoup.connect("https://www.amazon.in" + reviewLink)
				.cookies(cookies)
				.get();
		Elements reviewElementBodies = productReviewsDocument.getElementById("cm_cr-review_list")
				.getElementsByAttributeValue("data-hook", "review-body");
		Elements reviewElementTitles = productReviewsDocument.getElementById("cm_cr-review_list")
				.getElementsByAttributeValue("data-hook", "review-title");
		Elements reviewElementRatings = productReviewsDocument.getElementById("cm_cr-review_list")
				.getElementsByAttributeValue("data-hook", "review-star-rating");
		List<ProductReview> reviews = new ArrayList<>();
		for (int i = 0; i < reviewElementTitles.size(); i++) {
			Element reviewTitleElement = reviewElementTitles.get(i);
			Element reviewBodyElement = reviewElementBodies.get(i);
			Element reviewRatingsElement = reviewElementRatings.get(i);
			ProductReview productReview = ProductReview.builder()
					.title(reviewTitleElement.text())
					.body(reviewBodyElement.text())
					.rating(reviewRatingsElement.text())
					.build();
			reviews.add(productReview);
		}
		AmazonProduct amazonProduct = AmazonProduct.builder()
				.productName(titleElement.text())
				.starRating(starRatingAltElement.text())
				.reviewCount(reviewsCount)
				.reviews(reviews)
				.build();
		return amazonProduct;
	}

	@Getter
	@Setter
	@Builder
	static class AmazonProduct implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6171261335722845001L;

		private String productName;
		private String starRating;
		private Integer reviewCount;
		private List<ProductReview> reviews;

	}

	@Getter
	@Setter
	@Builder
	static class ProductReview implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3134103632411238972L;
		private String title;
		private String body;
		private String rating;

	}
}
