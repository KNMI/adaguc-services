package nl.knmi.adaguc.tools;

import lombok.Getter;

@Getter
public class JsonMessage {
	private String message;
	public JsonMessage(String message) {
		this.message=message;
	}

}